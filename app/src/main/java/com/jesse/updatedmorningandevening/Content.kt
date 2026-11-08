package com.jesse.updatedmorningandevening

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** One reading (a morning or an evening devotion). */
data class Section(val verse: String, val ref: String, val paras: List<String>)

enum class Edition(val label: String, val subtitle: String, val asset: String, val epub: String) {
    Modern("Modern", "Modern English · BSB", "modern.json", "editions/modern.epub"),
    Classic("Classic", "Original English · KJV", "classic.json", "editions/classic.epub"),
}

/** A whole edition, keyed by "month-day" (Feb 29 always exists). */
class EditionData(private val root: JSONObject) {
    fun get(month: Int, day: Int, evening: Boolean): Section? {
        val d = root.optJSONObject("$month-$day") ?: return null
        val s = d.optJSONObject(if (evening) "e" else "m") ?: return null
        val arr = s.getJSONArray("paras")
        return Section(s.getString("verse"), s.optString("ref"), List(arr.length()) { arr.getString(it) })
    }
}

class AboutLine(val heading: Boolean, val text: String)

class Library(val modern: EditionData, val classic: EditionData, val about: List<AboutLine>) {
    fun edition(e: Edition) = if (e == Edition.Modern) modern else classic

    companion object {
        fun load(ctx: Context): Library {
            fun read(name: String) = ctx.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val about = org.json.JSONArray(read("about.json")).let { a ->
                List(a.length()) {
                    val o = a.getJSONObject(it)
                    AboutLine(o.getString("tag") != "p", o.getString("text"))
                }
            }
            return Library(
                EditionData(JSONObject(read(Edition.Modern.asset))),
                EditionData(JSONObject(read(Edition.Classic.asset))),
                about,
            )
        }
    }
}

/** Calendar helpers. Pages are numbered day*2 + (0 = morning, 1 = evening) over a 366-day year. */
object Cal {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val daysIn = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val startOfMonth = IntArray(12).also { var t = 0; for (i in 0 until 12) { it[i] = t; t += daysIn[i] } }
    const val PAGES = 366 * 2

    fun dayIndex(month: Int, day: Int) = startOfMonth[month - 1] + day - 1

    /** @return Pair(month 1..12, day 1..n) */
    fun monthDay(dayIndex: Int): Pair<Int, Int> {
        var m = 11
        while (startOfMonth[m] > dayIndex) m--
        return (m + 1) to (dayIndex - startOfMonth[m] + 1)
    }

    fun todayDayIndex(today: LocalDate = LocalDate.now()) = dayIndex(today.monthValue, today.dayOfMonth)

    fun pageFor(now: java.time.LocalDateTime, switchHour: Int): Int =
        todayDayIndex(now.toLocalDate()) * 2 + if (now.hour >= switchHour) 1 else 0

    fun title(dayIndex: Int): String = monthDay(dayIndex).let { "${monthNames[it.first - 1]} ${it.second}" }
}
