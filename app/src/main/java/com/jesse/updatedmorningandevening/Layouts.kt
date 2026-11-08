package com.jesse.updatedmorningandevening

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Six distinct ways to present the date & time of day, Scripture verse & reference,
 * and Spurgeon's devotional prose (Original + 5 readability-focused designs).
 */
enum class PageLayout(val label: String, val blurb: String) {
    Elegant(
        "Classic Book",
        "Fine-press book page: centered chapter heading, framed epigraph, drop-cap initial, and literary paragraphs.",
    ),
    Standard(
        "Standard Devotional",
        "Intuitive modern layout: Morning/Evening header, accent-ruled verse box, and shorter 2–3 sentence paragraphs.",
    ),
    Cadence(
        "Sentence Cadence",
        "Tames long Victorian sentences: every sentence starts on a fresh line, and long multi-clause sentences step gently.",
    ),
    Guided(
        "Guided Editorial",
        "Calendar masthead, quoted Scripture frame, and numbered sections with bold lead-in sentences.",
    ),
    Cards(
        "Reflection Cards",
        "Calm, focused cards: pill header, highlighted Scripture card, and bite-sized 2–3 sentence reflection cards.",
    ),
    Original(
        "Spurgeon's Paragraphs",
        "Preserves Spurgeon's original unbroken, full-length paragraph formatting.",
    ),
}

private sealed interface DocBlock {
    data class Prose(val sentences: List<AnnotatedString>) : DocBlock
    data class Poem(val lines: List<AnnotatedString>) : DocBlock
}

private val NOT_SENTENCE_ENDERS = setOf(
    "mr", "mrs", "ms", "dr", "st", "vs", "rev", "ps", "v", "vv", "cf", "ch", "no", "gen", "ex", "lev", "num", "deut", "i.e", "e.g",
)

/** Splits an [AnnotatedString] into sentences while preserving all italic/bold spans. */
fun splitAnnotatedSentences(text: AnnotatedString): List<AnnotatedString> {
    val s = text.text
    if (s.isBlank()) return emptyList()
    val out = mutableListOf<AnnotatedString>()
    val keepAfter = setOf('"', '”', '’', '\'', ')', ']')
    var start = 0
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '.' || c == '!' || c == '?') {
            var j = i + 1
            while (j < s.length && s[j] in keepAfter) j++
            var k = j
            while (k < s.length && s[k].isWhitespace()) k++
            val spaced = k > j
            var n = k
            while (n < s.length && s[n] in "\"“‘'([") n++
            val nextStartsSentence = n < s.length && (s[n].isUpperCase() || s[n].isDigit())
            val prevWord = s.substring(start, i).trimEnd()
                .substringAfterLast(' ')
                .trimStart('"', '“', '‘', '\'', '(')
                .lowercase()
            val isAbbrev = c == '.' && (prevWord in NOT_SENTENCE_ENDERS || (prevWord.length == 1 && prevWord[0].isLetter()))
            if (spaced && nextStartsSentence && !isAbbrev) {
                val slice = trimAnnotated(text, start, j)
                if (slice.isNotEmpty()) out += slice
                start = k
                i = k
                continue
            }
            i = j
            continue
        }
        i++
    }
    if (start < s.length) {
        val tail = trimAnnotated(text, start, s.length)
        if (tail.isNotEmpty()) out += tail
    }
    return out
}

/** Breaks extra-long Victorian sentences (>= 26 words) at major '; ', ': ', or ' — ' pivots. */
private fun splitClauses(sentence: AnnotatedString): List<AnnotatedString> {
    val s = sentence.text
    val wordCount = s.split(Regex("\\s+")).size
    if (wordCount < 26) return listOf(sentence)

    val parts = mutableListOf<AnnotatedString>()
    var start = 0
    var wordsSinceSplit = 0
    var i = 0
    while (i < s.length) {
        if (s[i].isWhitespace() && (i == 0 || !s[i - 1].isWhitespace())) wordsSinceSplit++
        val semiOrColon = (s[i] == ';' || s[i] == ':') && i + 1 < s.length && s[i + 1] == ' '
        val emDash = (s[i] == '—' && i > 0 && i + 1 < s.length)
        if ((semiOrColon || emDash) && wordsSinceSplit >= 7) {
            val cutEnd = i + 1
            val remainingWords = s.substring(cutEnd).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            if (remainingWords >= 6) {
                val piece = trimAnnotated(sentence, start, cutEnd)
                if (piece.isNotEmpty()) parts += piece
                start = cutEnd
                while (start < s.length && s[start].isWhitespace()) start++
                wordsSinceSplit = 0
                i = start
                continue
            }
        }
        i++
    }
    if (start < s.length) {
        val tail = trimAnnotated(sentence, start, s.length)
        if (tail.isNotEmpty()) parts += tail
    }
    return if (parts.isEmpty()) listOf(sentence) else parts
}

private fun trimAnnotated(a: AnnotatedString, from: Int, to: Int): AnnotatedString {
    var s = from.coerceIn(0, a.length)
    var e = to.coerceIn(s, a.length)
    while (s < e && a.text[s].isWhitespace()) s++
    while (e > s && a.text[e - 1].isWhitespace()) e--
    return a.subSequence(s, e)
}

private fun joinAnnotated(items: List<AnnotatedString>, sep: String = " "): AnnotatedString = buildAnnotatedString {
    items.forEachIndexed { idx, item ->
        if (idx > 0) append(sep)
        append(item)
    }
}

private fun wordCount(a: AnnotatedString): Int =
    a.text.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }

/**
 * Parses raw paragraphs into prose & hymn blocks, breaking marathon 250–350 word paragraphs
 * into digestible ~2–3 sentence blocks (~65 words) while keeping poetry stanzas intact.
 */
private fun buildReadableBlocks(paras: List<String>, targetWords: Int = 68): List<DocBlock> {
    val raw = mutableListOf<DocBlock>()

    fun isPoeticLine(p: String): Boolean {
        if ('\n' in p) return true
        val words = p.trim().split(Regex("\\s+")).size
        return words in 2..13 && !p.trimEnd().endsWith(".")
    }

    var idx = 0
    while (idx < paras.size) {
        val p = paras[idx]
        val looksLikeShortVerseLine = p.trim().split(Regex("\\s+")).size <= 14 &&
            (('\n' in p) || (idx + 1 < paras.size && paras[idx + 1].trim().split(Regex("\\s+")).size <= 14) ||
                (idx > 0 && raw.lastOrNull() is DocBlock.Poem))
        if (isPoeticLine(p) || looksLikeShortVerseLine) {
            val poemLines = mutableListOf<AnnotatedString>()
            while (idx < paras.size) {
                val cur = paras[idx]
                val wc = cur.trim().split(Regex("\\s+")).size
                if ('\n' in cur || wc <= 14) {
                    cur.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                        poemLines += markup(line)
                    }
                    idx++
                } else {
                    break
                }
            }
            if (poemLines.isNotEmpty()) raw += DocBlock.Poem(poemLines)
            continue
        }

        val annotated = markup(p)
        val sentences = splitAnnotatedSentences(annotated)
        if (sentences.size <= 3 || wordCount(annotated) <= targetWords + 25) {
            raw += DocBlock.Prose(sentences.ifEmpty { listOf(annotated) })
        } else {
            val chunks = mutableListOf<MutableList<AnnotatedString>>()
            var current = mutableListOf<AnnotatedString>()
            var currentWords = 0
            for (sent in sentences) {
                val w = wordCount(sent)
                if (current.isNotEmpty() && (currentWords + w > targetWords || current.size >= 3) && currentWords >= 32) {
                    chunks += current
                    current = mutableListOf()
                    currentWords = 0
                }
                current += sent
                currentWords += w
            }
            if (current.isNotEmpty()) {
                // Avoid a tiny 1-sentence orphan at the end of a split paragraph.
                if (chunks.isNotEmpty() && current.size == 1 && currentWords < 28) {
                    chunks.last() += current
                } else {
                    chunks += current
                }
            }
            chunks.forEach { raw += DocBlock.Prose(it) }
        }
        idx++
    }
    return raw
}

@Composable
fun ReadingContent(
    s: ReaderSettings,
    month: Int,
    day: Int,
    evening: Boolean,
    section: Section?,
    isFallback: Boolean,
    body: TextStyle,
) {
    val t = s.themeFor(evening)
    val ctx = LocalContext.current
    val openRef = {
        if (section != null && section.ref.isNotBlank()) {
            BibleLinks.open(ctx, section.ref, s.edition, s.bibleProvider)
        }
    }
    val monthName = Cal.monthNames[month - 1]
    val date = "$monthName $day"
    val time = if (evening) "Evening" else "Morning"
    val fs = s.fontSize
    val start = body.copy(textAlign = TextAlign.Start)

    Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        if (section == null) {
            Text("This reading isn't available yet.", style = body)
            return@Column
        }

        when (s.layout) {
            // 0. ORIGINAL — Kept intact as the baseline option
            PageLayout.Original -> {
                Text(
                    "$date · $time".uppercase(),
                    style = start.copy(fontSize = 12.sp, letterSpacing = 1.6.sp, color = t.accent, fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(18.dp))
                FallbackNote(isFallback, body, t)
                Text(markup(section.verse), style = start.copy(fontStyle = FontStyle.Italic, fontSize = (fs * 1.05f).sp))
                if (section.ref.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "— ${section.ref} ↗",
                        style = start.copy(color = t.muted, fontSize = (fs * .85f).sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clickable { openRef() },
                    )
                }
                Spacer(Modifier.height(22.dp))
                section.paras.forEach { p ->
                    Text(markup(p), style = body, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height((fs * .8f).dp))
                }
            }

            // 1. STANDARD DEVOTIONAL — Intuitive modern devotional app layout + breathable 2–3 sentence paragraphs
            PageLayout.Standard -> {
                val blocks = remember(section) { buildReadableBlocks(section.paras, targetWords = 68) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (evening) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = t.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "$time Devotion".uppercase(),
                        style = start.copy(fontSize = 12.sp, letterSpacing = 1.8.sp, color = t.accent, fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(date, style = start.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(18.dp))
                FallbackNote(isFallback, body, t)

                // Ruled Scripture callout with subtle tint
                Surface(
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    color = t.accent.copy(alpha = if (t.dark) 0.12f else 0.08f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(t.accent))
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                markup(section.verse),
                                style = start.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = (fs * 1.06f).sp,
                                    lineHeight = (fs * s.lineSpacing * 1.02f).sp,
                                ),
                            )
                            if (section.ref.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${section.ref} ↗",
                                    style = start.copy(color = t.accent, fontSize = (fs * .82f).sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                                    modifier = Modifier.clickable { openRef() },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                blocks.forEach { b ->
                    when (b) {
                        is DocBlock.Prose -> {
                            Text(joinAnnotated(b.sentences), style = body, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height((fs * 0.95f).dp))
                        }
                        is DocBlock.Poem -> PoemBlock(b.lines, start, t, centered = false)
                    }
                }
            }

            // 2. CLASSIC BOOK — Elegant centered typography, framed epigraph, drop-cap initial, literary chunks
            PageLayout.Elegant -> {
                val blocks = remember(section) { buildReadableBlocks(section.paras, targetWords = 78) }
                val center = body.copy(textAlign = TextAlign.Center)

                Spacer(Modifier.height(8.dp))
                Text(
                    "—  ${time.uppercase()}  —",
                    style = center.copy(fontSize = 11.sp, letterSpacing = 3.5.sp, color = t.accent, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    date,
                    style = center.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Ornament(t, space = 16.dp)
                FallbackNote(isFallback, body, t)

                // Framed centered epigraph
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        markup(section.verse),
                        style = center.copy(fontStyle = FontStyle.Italic, fontSize = (fs * 1.06f).sp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (section.ref.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${section.ref.uppercase()} ↗",
                            style = center.copy(color = t.muted, fontSize = (fs * .64f).sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth().clickable { openRef() },
                        )
                    }
                }
                Ornament(t, space = 18.dp)

                var firstProseRendered = false
                blocks.forEach { b ->
                    when (b) {
                        is DocBlock.Prose -> {
                            val paraText = joinAnnotated(b.sentences)
                            if (!firstProseRendered && paraText.isNotEmpty()) {
                                firstProseRendered = true
                                // Find first visible letter (skipping leading quote mark if present)
                                val leadLen = if (paraText.text.first() in "\"“‘'" && paraText.length >= 2) 2 else 1
                                val styled = buildAnnotatedString {
                                    withStyle(SpanStyle(fontSize = (fs * 2.15f).sp, color = t.accent, fontWeight = FontWeight.SemiBold)) {
                                        append(paraText.subSequence(0, leadLen))
                                    }
                                    append(paraText.subSequence(leadLen, paraText.length))
                                }
                                Text(styled, style = body, modifier = Modifier.fillMaxWidth())
                            } else {
                                Text(paraText, style = body, modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height((fs * 0.95f).dp))
                        }
                        is DocBlock.Poem -> PoemBlock(b.lines, center, t, centered = true)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("❦", style = center.copy(color = t.accent, fontSize = 22.sp), modifier = Modifier.fillMaxWidth())
            }

            // 3. SENTENCE CADENCE — Tames long sentences: 1 sentence per block + clause stepping on '; ' / ': ' / '—'
            PageLayout.Cadence -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(date, style = start.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold))
                    Surface(shape = RoundedCornerShape(50), color = t.accent.copy(alpha = 0.16f)) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (evening) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = t.accent,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(time, color = t.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = t.muted.copy(alpha = 0.25f))
                FallbackNote(isFallback, body, t)

                // Reference eyebrow above verse for immediate context
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.04f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (section.ref.isNotBlank()) {
                            Text(
                                "${section.ref.uppercase()} ↗",
                                style = start.copy(fontSize = 11.sp, letterSpacing = 1.6.sp, color = t.accent, fontWeight = FontWeight.Bold),
                                modifier = Modifier.clickable { openRef() },
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(markup(section.verse), style = start.copy(fontStyle = FontStyle.Italic, fontSize = (fs * 1.04f).sp))
                    }
                }
                Spacer(Modifier.height(22.dp))

                section.paras.forEachIndexed { pIdx, p ->
                    val words = p.trim().split(Regex("\\s+")).size
                    if ('\n' in p || (words <= 13 && !p.trimEnd().endsWith("."))) {
                        val lines = p.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.map { markup(it) }
                        PoemBlock(lines, start, t, centered = false)
                    } else {
                        val sentences = splitAnnotatedSentences(markup(p))
                        sentences.forEach { sent ->
                            val clauses = splitClauses(sent)
                            if (clauses.size == 1) {
                                Text(clauses[0], style = start, modifier = Modifier.fillMaxWidth())
                            } else {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(clauses[0], style = start, modifier = Modifier.fillMaxWidth())
                                    clauses.drop(1).forEach { cl ->
                                        Spacer(Modifier.height(4.dp))
                                        Row(Modifier.fillMaxWidth().padding(start = 12.dp)) {
                                            Box(
                                                Modifier.padding(top = (fs * 0.48f).dp, end = 8.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(t.accent.copy(alpha = 0.65f)),
                                            )
                                            Text(cl, style = start, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height((fs * 0.68f).dp))
                        }
                        if (pIdx < section.paras.lastIndex) {
                            Spacer(Modifier.height((fs * 0.35f).dp))
                            HorizontalDivider(Modifier.width(44.dp), thickness = 2.dp, color = t.accent.copy(alpha = 0.45f))
                            Spacer(Modifier.height((fs * 0.85f).dp))
                        }
                    }
                }
            }

            // 4. GUIDED EDITORIAL — Calendar masthead, quote frame, numbered thought movements with bold lead sentence
            PageLayout.Guided -> {
                val blocks = remember(section) { buildReadableBlocks(section.paras, targetWords = 72) }

                // Calendar lockup: Big day number | Month & Morning/Evening
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        day.toString().padStart(2, '0'),
                        style = start.copy(fontSize = 42.sp, lineHeight = 44.sp, color = t.accent, fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(1.dp).height(34.dp).background(t.muted.copy(alpha = 0.35f)))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            monthName.uppercase(),
                            style = start.copy(fontSize = 14.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            "$time Reading",
                            style = start.copy(fontSize = 13.sp, color = t.muted),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                FallbackNote(isFallback, body, t)

                // Editorial quote box with decorative quotation mark
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, t.muted.copy(alpha = 0.28f)),
                    color = t.bg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Text(
                            "“",
                            style = start.copy(fontSize = 38.sp, lineHeight = 36.sp, color = t.accent, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 10.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(markup(section.verse), style = start.copy(fontStyle = FontStyle.Italic, fontSize = (fs * 1.04f).sp))
                            if (section.ref.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "— ${section.ref} ↗",
                                    style = start.copy(color = t.accent, fontSize = (fs * 0.82f).sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End),
                                    modifier = Modifier.fillMaxWidth().clickable { openRef() },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                var sectionNum = 0
                blocks.forEach { b ->
                    when (b) {
                        is DocBlock.Prose -> {
                            sectionNum++
                            val lead = b.sentences.first()
                            val rest = b.sentences.drop(1)
                            val combined = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = t.fg)) { append(lead) }
                                if (rest.isNotEmpty()) {
                                    append(" ")
                                    withStyle(SpanStyle(color = t.fg.copy(alpha = 0.90f))) {
                                        append(joinAnnotated(rest))
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    sectionNum.toString().padStart(2, '0'),
                                    style = start.copy(fontSize = 13.sp, color = t.accent, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(32.dp).padding(top = 4.dp),
                                )
                                Text(combined, style = body, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height((fs * 1.0f).dp))
                        }
                        is DocBlock.Poem -> {
                            Row(Modifier.fillMaxWidth()) {
                                Spacer(Modifier.width(32.dp))
                                Box(Modifier.weight(1f)) { PoemBlock(b.lines, start, t, centered = false) }
                            }
                        }
                    }
                }
            }

            // 5. REFLECTION CARDS — Calm pill header, highlighted Scripture card, bite-sized 2–3 sentence cards
            PageLayout.Cards -> {
                val blocks = remember(section) { buildReadableBlocks(section.paras, targetWords = 64) }
                val proseTotal = blocks.count { it is DocBlock.Prose }

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(50), color = t.fg.copy(alpha = if (t.dark) 0.10f else 0.07f)) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (evening) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = t.accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "$time  ·  $date",
                                style = start.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FallbackNote(isFallback, body, t)

                // Highlighted Scripture card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = t.accent.copy(alpha = if (t.dark) 0.16f else 0.12f),
                    border = BorderStroke(1.dp, t.accent.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        if (section.ref.isNotBlank()) {
                            Text(
                                "SCRIPTURE  ·  ${section.ref.uppercase()} ↗",
                                style = start.copy(fontSize = 11.sp, letterSpacing = 1.4.sp, color = t.accent, fontWeight = FontWeight.Bold),
                                modifier = Modifier.clickable { openRef() },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            markup(section.verse),
                            style = start.copy(fontStyle = FontStyle.Italic, fontSize = (fs * 1.06f).sp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                var cardIndex = 0
                blocks.forEach { b ->
                    when (b) {
                        is DocBlock.Prose -> {
                            cardIndex++
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.045f),
                                border = BorderStroke(1.dp, t.fg.copy(alpha = 0.07f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    if (proseTotal > 1) {
                                        Text(
                                            "$cardIndex / $proseTotal",
                                            style = start.copy(fontSize = 11.sp, color = t.muted, fontWeight = FontWeight.SemiBold),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text(joinAnnotated(b.sentences), style = body, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        is DocBlock.Poem -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = t.accent.copy(alpha = 0.07f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    b.lines.forEach { line ->
                                        Text(line, style = start.copy(fontStyle = FontStyle.Italic), modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoemBlock(lines: List<AnnotatedString>, base: TextStyle, t: ReaderPalette, centered: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(
            start = if (centered) 12.dp else 18.dp,
            end = 12.dp,
            top = 4.dp,
            bottom = 16.dp,
        ),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        lines.forEach { line ->
            Text(
                line,
                style = base.copy(fontStyle = FontStyle.Italic, color = t.fg.copy(alpha = 0.92f)),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FallbackNote(show: Boolean, body: TextStyle, t: ReaderPalette) {
    if (!show) return
    Text(
        "This reading hasn't been modernised yet, so the classic text is shown.",
        style = body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = t.muted, fontStyle = FontStyle.Italic, textAlign = TextAlign.Start),
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun Ornament(t: ReaderPalette, space: Dp = 18.dp) {
    Spacer(Modifier.height(space))
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(Modifier.width(36.dp), color = t.accent.copy(alpha = 0.45f))
        Text("  ✦  ", color = t.accent, fontSize = 13.sp)
        HorizontalDivider(Modifier.width(36.dp), color = t.accent.copy(alpha = 0.45f))
    }
    Spacer(Modifier.height(space))
}
