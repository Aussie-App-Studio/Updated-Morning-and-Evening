"""Builds the app's content assets from the two source editions.

Outputs (in app/src/main/assets/):
  modern.json   - Modern English / BSB edition (from the 'modernized' folders)
  classic.json  - Original Victorian English / KJV edition (from the classic EPUB)
  about.json    - "About this Edition" text
  editions/     - EPUB files offered for download in Settings

JSON shape:  { "1-1": { "m": {"verse","ref","paras":[...]}, "e": {...} }, ... }
Paragraph text may contain <i>..</i> and <b>..</b>, and \n for line breaks.

Usage:  python tools/build_content.py        (also run by sync_content.ps1)
"""
import html
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

SHARP_SRC = Path(r"C:\Users\jesse\Documents\antigravity\sharp-hawking")
MODERN_DIR = SHARP_SRC / "project" / "modernized"
MODERN_EPUB = SHARP_SRC / "output" / "Morning_and_Evening_Modern_English.epub"
LEGACY_SRC = Path(r"C:\AndroidProjects\Morning and Evening Modern English")
LEGACY_EXTRACTED = LEGACY_SRC / "project" / "extracted"
LEGACY_MODERN_DIR = LEGACY_SRC / "project" / "modernized"
CLASSIC_EPUB = Path(r"C:\Users\jesse\Downloads\Morning and Evening - Charles H Spurgeon.epub")
ASSETS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets"

MONTHS = ["January", "February", "March", "April", "May", "June", "July",
          "August", "September", "October", "November", "December"]
DAYS_IN = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

BLOCK = re.compile(r"<(h2|h3|p|blockquote)\b([^>]*)>(.*?)</\1>|<hr\b[^>]*/?>", re.S)


def inline(s: str) -> str:
    s = re.sub(r"\s+", " ", s)
    s = re.sub(r"<br\s*/?>", "\n", s)
    s = re.sub(r"<(/?)(em|i)\b[^>]*>", r"<\1i>", s)
    s = re.sub(r"<(/?)(strong|b)\b[^>]*>", r"<\1b>", s)
    s = re.sub(r"<(?!/?[ib]>)[^>]+>", "", s)
    s = html.unescape(s)
    s = re.sub(r"[ \t\r\f\v]+", " ", s)
    s = re.sub(r" ?\n ?", "\n", s)
    return s.strip()


def plain(s: str) -> str:
    return re.sub(r"</?[ib]>", "", s)


def split_verse(text: str):
    """'Verse text - <i>Ref</i>' -> (verse, ref)"""
    m = re.match(r"^(.*?)\s+[—–-]+\s+<i>(.*?)</i>\s*$", text, re.S)
    if m:
        ref = re.sub(r"^([123])([A-Z])", r"\1 \2", m.group(2).strip())
        return m.group(1).strip(), ref
    m = re.match(r"^(.*)\s+[—–-]+\s+(.+)$", text, re.S)
    if m and len(plain(m.group(2))) < 60:
        ref = re.sub(r"^([123])([A-Z])", r"\1 \2", plain(m.group(2)).strip())
        return m.group(1).strip(), ref
    # Reference trailing the verse with no separator, e.g. 'John 14:21' or '1 Peter 1:23'
    m = re.match(
        r"^(.*?\S)\s+((?:[123]\s*)?[A-Z][A-Za-z]+(?:\s+of\s+[A-Za-z]+)?\.?\s+\d+(?::\d+(?:[-–,\s]*\d+)*)?)\s*$",
        plain(text), re.S,
    )
    if m and len(m.group(1)) > 5:
        ref = re.sub(r"^([123])([A-Z])", r"\1 \2", m.group(2).strip())
        return m.group(1).strip(), ref
    return text, ""


def parse_files(xhtml_texts):
    """Parse a stream of xhtml documents into {key: {m:..., e:...}}."""
    out, key, sec = {}, None, None
    for doc in xhtml_texts:
        body = doc.split("<body", 1)[-1]
        for m in BLOCK.finditer(body):
            tag, attrs, inner = m.group(1), m.group(2), m.group(3)
            if tag is None:  # <hr>
                continue
            text = inline(inner)
            if tag == "h3":
                dm = re.match(r"^(%s)\s+(\d+)$" % "|".join(MONTHS), plain(text))
                if dm:
                    key = f"{MONTHS.index(dm.group(1)) + 1}-{int(dm.group(2))}"
                    out.setdefault(key, {})
                    sec = None
                continue
            if tag == "h2" or key is None:
                continue
            low = plain(text).lower()
            if tag == "p" and len(low) < 40 and re.search(r"(morning|evening)'?s? verse", low):
                sec = "m" if "morning" in low else "e"
                if sec in out[key]:
                    # Source is missing a date heading (e.g. classic June 3): advance to the next day.
                    mi, d = map(int, key.split("-"))
                    mi, d = (mi + 1, 1) if d >= DAYS_IN[mi - 1] else (mi, d + 1)
                    key = f"{mi}-{d}"
                    out.setdefault(key, {})
                    print("note: inferred missing heading ->", key)
                out[key][sec] = {"verse": "", "ref": "", "paras": []}
                continue
            if sec is None or not text:
                continue
            cur = out[key][sec]
            if not cur["verse"] and not cur["paras"]:
                cur["verse"], cur["ref"] = split_verse(text)
            else:
                cur["paras"].append(text)
    return out


def read_modern():
    out = {}
    for mi, (mname, ndays) in enumerate(zip(MONTHS, DAYS_IN), 1):
        m_lower = mname.lower()
        mdir = MODERN_DIR / m_lower
        for d in range(1, ndays + 1):
            key = f"{mi}-{d}"
            out[key] = {}
            for slot, sec in (("morning", "m"), ("evening", "e")):
                f = mdir / f"{m_lower}_{d:02d}_{slot}.xhtml"
                txt = f.read_text(encoding="utf-8")
                q = re.search(r'<p class="quote">(.*?)</p>', txt, re.S).group(1)
                verse, ref = split_verse(inline(q))
                div = re.search(r'<div class="content">(.*?)</div>', txt, re.S).group(1)
                paras = [inline(m.group(1)) for m in re.finditer(r'<p(?:\s+class="[^"]*")?>(.*?)</p>', div, re.S)]
                paras = [p for p in paras if p]
                out[key][sec] = {"verse": verse, "ref": ref, "paras": paras}
    return out


def read_classic():
    with zipfile.ZipFile(CLASSIC_EPUB) as z:
        names = sorted(n for n in z.namelist() if n.startswith("OEBPS/text/") and n.endswith(".xhtml"))
        return parse_files([z.read(n).decode("utf-8") for n in names])


def about_text():
    f = LEGACY_MODERN_DIR / "01_january" / "content002_split_000.xhtml"
    paras = []
    for m in BLOCK.finditer(f.read_text(encoding="utf-8").split("<body", 1)[-1]):
        if m.group(1) in ("h2", "h3", "p") and plain(inline(m.group(3))) not in ("", "January"):
            paras.append({"tag": m.group(1), "text": inline(m.group(3))})
    return paras


def _render_epub_day_section(mname_lower: str, day: int, slot: str) -> str:
    f = MODERN_DIR / mname_lower / f"{mname_lower}_{day:02d}_{slot}.xhtml"
    txt = f.read_text(encoding="utf-8")
    q = re.search(r'<p class="quote">.*?</p>', txt, re.S).group(0)
    div = re.search(r'<div class="content">(.*?)</div>', txt, re.S).group(1).strip()
    div = re.sub(r"<p>", '<p class="calibre4">', div)
    div = re.sub(r"<br\s*/?>", '<br class="calibre5"/>', div)
    label = "This morning's verse:" if slot == "morning" else "This evening's verse:"
    return f'    <p class="noindent">{label}</p>\n\n    {q}\n\n    {div}'


def build_modern_epub(dest_epub: Path):
    if not LEGACY_EXTRACTED.exists() or not LEGACY_MODERN_DIR.exists():
        return False
    replacements = {}
    about_xhtml = LEGACY_MODERN_DIR / "01_january" / "content002_split_000.xhtml"
    if about_xhtml.exists():
        replacements["OEBPS/text/content002_split_000.xhtml"] = about_xhtml.read_bytes()

    for f in sorted(LEGACY_MODERN_DIR.rglob("*.xhtml")):
        if f.name == "content002_split_000.xhtml":
            continue
        orig_txt = f.read_text(encoding="utf-8")
        h3s = list(re.finditer(r"<h3[^>]*>((?:%s)\s+(\d+))</h3>" % "|".join(MONTHS), orig_txt))
        if not h3s:
            continue
        parts = []
        for idx, m in enumerate(h3s):
            if idx > 0:
                parts.append('    <hr class="section"/>')
            h3_tag = m.group(0)
            mname = m.group(1).split()[0].lower()
            day = int(m.group(2))
            parts.append(f"    {h3_tag}")
            parts.append(_render_epub_day_section(mname, day, "morning"))
            parts.append('    <hr class="section"/>')
            parts.append(_render_epub_day_section(mname, day, "evening"))
        title = " &amp; ".join(m.group(1) for m in h3s)
        new_xhtml = f"""<?xml version='1.0' encoding='utf-8'?>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>{title}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="../../stylesheet.css"/>
    <link rel="stylesheet" type="text/css" href="../../page_styles.css"/>
  </head>
  <body class="calibre">
{chr(10).join(parts)}
  </body>
</html>
"""
        raw_bytes = new_xhtml.encode("utf-8")
        ET.fromstring(raw_bytes)
        replacements[f"OEBPS/text/{f.name}"] = raw_bytes

    dest_epub.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(dest_epub, "w", zipfile.ZIP_DEFLATED) as zout:
        mimetype_path = LEGACY_EXTRACTED / "mimetype"
        if mimetype_path.exists():
            zout.writestr("mimetype", mimetype_path.read_bytes(), compress_type=zipfile.ZIP_STORED)
        for full_path in sorted(LEGACY_EXTRACTED.rglob("*")):
            if full_path.is_dir():
                continue
            rel_path = full_path.relative_to(LEGACY_EXTRACTED).as_posix()
            if rel_path == "mimetype":
                continue
            if rel_path in replacements:
                zout.writestr(rel_path, replacements[rel_path])
            elif rel_path == "content.opf":
                opf = full_path.read_text(encoding="utf-8").replace(
                    "<dc:title>Morning and Evening</dc:title>",
                    "<dc:title>Morning and Evening (Modern English Edition - BSB)</dc:title>",
                )
                zout.writestr(rel_path, opf.encode("utf-8"))
            else:
                zout.writestr(rel_path, full_path.read_bytes())
    return True


def report(name, data):
    missing = []
    for mi, n in enumerate(DAYS_IN, 1):
        for d in range(1, n + 1):
            e = data.get(f"{mi}-{d}", {})
            for s in ("m", "e"):
                if s not in e or not e[s]["paras"] or not e[s]["verse"]:
                    missing.append(f"{mi}-{d}{s}")
    print(f"{name}: {len(data)} days parsed, {len(missing)} missing sections")
    if missing:
        print("   missing:", ", ".join(missing[:40]), "..." if len(missing) > 40 else "")


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / "editions").mkdir(exist_ok=True)
    modern, classic = read_modern(), read_classic()
    report("modern", modern)
    report("classic", classic)
    for name, data in (("modern", modern), ("classic", classic)):
        (ASSETS / f"{name}.json").write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (ASSETS / "about.json").write_text(json.dumps(about_text(), ensure_ascii=False), encoding="utf-8")
    if build_modern_epub(MODERN_EPUB):
        shutil.copy(MODERN_EPUB, ASSETS / "editions" / "modern.epub")
        print("Modern EPUB built and copied to", ASSETS / "editions" / "modern.epub")
    elif MODERN_EPUB.exists():
        shutil.copy(MODERN_EPUB, ASSETS / "editions" / "modern.epub")
    if CLASSIC_EPUB.exists():
        shutil.copy(CLASSIC_EPUB, ASSETS / "editions" / "classic.epub")
    print("Assets written to", ASSETS)


if __name__ == "__main__":
    sys.exit(main())
