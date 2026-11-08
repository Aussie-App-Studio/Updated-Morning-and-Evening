package com.jesse.updatedmorningandevening

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

enum class BibleProvider(val label: String, val blurb: String) {
    YouVersion(
        "YouVersion",
        "Opens the chapter in the YouVersion Bible app (or bible.com in your browser).",
    ),
    BibleHub(
        "Bible Hub",
        "Opens the full chapter on BibleHub.com (BSB for Modern, KJV for Classic).",
    ),
}

object BibleLinks {
    // Map canonical lowercase book name -> Pair(USFM code for YouVersion, slug for BibleHub)
    private val BOOKS: Map<String, Pair<String, String>> = mapOf(
        "genesis" to ("GEN" to "genesis"),
        "exodus" to ("EXO" to "exodus"),
        "leviticus" to ("LEV" to "leviticus"),
        "numbers" to ("NUM" to "numbers"),
        "deuteronomy" to ("DEU" to "deuteronomy"),
        "joshua" to ("JOS" to "joshua"),
        "judges" to ("JDG" to "judges"),
        "ruth" to ("RUT" to "ruth"),
        "1 samuel" to ("1SA" to "1_samuel"),
        "2 samuel" to ("2SA" to "2_samuel"),
        "1 kings" to ("1KI" to "1_kings"),
        "2 kings" to ("2KI" to "2_kings"),
        "1 chronicles" to ("1CH" to "1_chronicles"),
        "2 chronicles" to ("2CH" to "2_chronicles"),
        "ezra" to ("EZR" to "ezra"),
        "nehemiah" to ("NEH" to "nehemiah"),
        "esther" to ("EST" to "esther"),
        "job" to ("JOB" to "job"),
        "psalm" to ("PSA" to "psalms"),
        "psalms" to ("PSA" to "psalms"),
        "proverbs" to ("PRO" to "proverbs"),
        "ecclesiastes" to ("ECC" to "ecclesiastes"),
        "song" to ("SNG" to "songs"),
        "song of solomon" to ("SNG" to "songs"),
        "song of songs" to ("SNG" to "songs"),
        "isaiah" to ("ISA" to "isaiah"),
        "jeremiah" to ("JER" to "jeremiah"),
        "lamentations" to ("LAM" to "lamentations"),
        "ezekiel" to ("EZK" to "ezekiel"),
        "daniel" to ("DAN" to "daniel"),
        "hosea" to ("HOS" to "hosea"),
        "joel" to ("JOL" to "joel"),
        "amos" to ("AMO" to "amos"),
        "obadiah" to ("OBA" to "obadiah"),
        "jonah" to ("JON" to "jonah"),
        "micah" to ("MIC" to "micah"),
        "nahum" to ("NAM" to "nahum"),
        "habakkuk" to ("HAB" to "habakkuk"),
        "zephaniah" to ("ZEP" to "zephaniah"),
        "haggai" to ("HAG" to "haggai"),
        "zechariah" to ("ZEC" to "zechariah"),
        "malachi" to ("MAL" to "malachi"),
        "matthew" to ("MAT" to "matthew"),
        "mark" to ("MRK" to "mark"),
        "luke" to ("LUK" to "luke"),
        "john" to ("JHN" to "john"),
        "acts" to ("ACT" to "acts"),
        "romans" to ("ROM" to "romans"),
        "1 corinthians" to ("1CO" to "1_corinthians"),
        "2 corinthians" to ("2CO" to "2_corinthians"),
        "galatians" to ("GAL" to "galatians"),
        "ephesians" to ("EPH" to "ephesians"),
        "philippians" to ("PHP" to "philippians"),
        "colossians" to ("COL" to "colossians"),
        "1 timothy" to ("1TI" to "1_timothy"),
        "2 timothy" to ("2TI" to "2_timothy"),
        "1 thessalonians" to ("1TH" to "1_thessalonians"),
        "2 thessalonians" to ("2TH" to "2_thessalonians"),
        "titus" to ("TIT" to "titus"),
        "philemon" to ("PHM" to "philemon"),
        "hebrews" to ("HEB" to "hebrews"),
        "james" to ("JAS" to "james"),
        "1 peter" to ("1PE" to "1_peter"),
        "2 peter" to ("2PE" to "2_peter"),
        "1 john" to ("1JN" to "1_john"),
        "2 john" to ("2JN" to "2_john"),
        "3 john" to ("3JN" to "3_john"),
        "jude" to ("JUD" to "jude"),
        "revelation" to ("REV" to "revelation"),
    )

    private val SINGLE_CHAPTER_BOOKS = setOf("OBA", "PHM", "2JN", "3JN", "JUD")
    private val REF_REGEX = Regex("""^((?:[123]\s*)?[A-Za-z\s\.]+?)\s+(\d+)(?::(\d+))?.*$""")

    fun urlFor(ref: String, edition: Edition, provider: BibleProvider): String? {
        val m = REF_REGEX.matchEntire(ref.trim()) ?: return null
        val rawBook = m.groupValues[1].trim().trimEnd('.').replace(Regex("""^([123])\s*([A-Za-z])"""), "$1 $2").lowercase()
        val (usfm, hubSlug) = BOOKS[rawBook] ?: return null
        val num1 = m.groupValues[2].toIntOrNull() ?: 1
        val num2 = m.groupValues[3].toIntOrNull()

        // Single-chapter books sometimes write 'Jude 24' instead of 'Jude 1:24'
        val chapter = if (usfm in SINGLE_CHAPTER_BOOKS) 1 else num1
        val verse = if (usfm in SINGLE_CHAPTER_BOOKS && num2 == null && num1 > 1) num1 else num2

        return when (provider) {
            BibleProvider.YouVersion -> {
                val verId = if (edition == Edition.Modern) 3034 else 1
                val verCode = if (edition == Edition.Modern) "BSB" else "KJV"
                if (verse != null) {
                    "https://www.bible.com/bible/$verId/$usfm.$chapter.$verse.$verCode"
                } else {
                    "https://www.bible.com/bible/$verId/$usfm.$chapter.$verCode"
                }
            }
            BibleProvider.BibleHub -> {
                val verSlug = if (edition == Edition.Modern) "bsb" else "kjv"
                "https://biblehub.com/$verSlug/$hubSlug/$chapter.htm"
            }
        }
    }

    fun open(ctx: Context, ref: String, edition: Edition, provider: BibleProvider) {
        val url = urlFor(ref, edition, provider)
        if (url == null) {
            Toast.makeText(ctx, "Couldn't open reference: $ref", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(ctx, "No app found to open link", Toast.LENGTH_SHORT).show()
        }
    }
}
