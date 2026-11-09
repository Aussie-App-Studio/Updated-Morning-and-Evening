package com.jesse.updatedmorningandevening

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

enum class ReadingMode(val label: String, val blurb: String) {
    Calendar("Follow the calendar", "Opens to today's date."),
    Plan(
        "Start from a day I choose",
        "Pick which reading you want to read today. Tomorrow the app moves to the next one, and so on, whatever the real date is. " +
            "For example, start at January 1 today and you'll see January 2 tomorrow.",
    ),
    Resume("Resume where I left off", "Always reopens to the last reading you viewed."),
}

enum class ReadMarkMode(val label: String, val blurb: String) {
    BottomAndTimer(
        "Reach bottom + 15s",
        "Marks a devotion as read once you have reached the bottom and spent 15 seconds on the page.",
    ),
    TimerOnly(
        "After 15s",
        "Automatically marks a devotion as read after 15 seconds on the page.",
    ),
    BottomOnly(
        "Scroll to bottom",
        "Automatically marks a devotion as read as soon as you scroll to the bottom.",
    ),
    Manual(
        "Manual only",
        "Only marks a devotion as read when you tap the Read button at the bottom of the page.",
    ),
}

enum class PageTurnStyle(val label: String, val blurb: String) {
    Slide(
        "Slide",
        "Standard horizontal slide used by Kindle and Apple Books.",
    ),
    PageCurl(
        "Page Curl",
        "Realistic curling paper fold with a curved peel, paper underside, and cast shadow.",
    ),
    BookCurl(
        "Book Page",
        "3D page turn hinged at the left spine with a soft shadow over the next devotion.",
    ),
    Depth(
        "Depth Stack",
        "The current page glides away while the next devotion rises smoothly from underneath.",
    ),
    None(
        "None (Instant)",
        "Switches immediately with no sliding motion, like an E-Ink reader.",
    ),
}

enum class ThemeMode(val label: String) {
    Single("Single"),
    MorningEvening("Morning / Evening"),
    System("Auto (Phone)"),
}

data class ReaderPalette(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val accent: Color,
    val dark: Boolean,
)

enum class ReaderTheme(val label: String, val bg: Color, val fg: Color, val muted: Color, val accent: Color, val dark: Boolean) {
    Light("Light", Color(0xFFFFFFFF), Color(0xFF1B1B1B), Color(0xFF6B6B6B), Color(0xFF8A5A00), false),
    Sepia("Sepia", Color(0xFFF4ECD8), Color(0xFF4B3B2A), Color(0xFF8C7A63), Color(0xFF9A5B13), false),
    Dark("Dark", Color(0xFF2A2B2E), Color(0xFFD9D9D9), Color(0xFF9A9A9A), Color(0xFFE0B25A), true),
    Black("Black", Color(0xFF000000), Color(0xFFBDBDBD), Color(0xFF7C7C7C), Color(0xFFD9A441), true);

    fun palette(brightness: Float = 1f): ReaderPalette {
        val dim = (1f - brightness).coerceIn(0f, 0.78f)
        if (dim <= 0.005f) return ReaderPalette(bg, fg, muted, accent, dark)
        val scrim = Color(0xFF140C04)
        return ReaderPalette(
            bg = lerp(bg, scrim, dim),
            fg = lerp(fg, scrim, dim),
            muted = lerp(muted, scrim, dim),
            accent = lerp(accent, scrim, dim),
            dark = dark,
        )
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, style: FontStyle = FontStyle.Normal) =
    Font(res, FontWeight(weight), style, variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

enum class ReaderFont(val label: String, val family: FontFamily) {
    Literata(
        "Serif",
        FontFamily(
            variable(R.font.literata, 400), variable(R.font.literata, 700),
            variable(R.font.literata_italic, 400, FontStyle.Italic), variable(R.font.literata_italic, 700, FontStyle.Italic),
        ),
    ),
    Garamond(
        "Garamond",
        FontFamily(
            variable(R.font.ebgaramond, 400), variable(R.font.ebgaramond, 700),
            variable(R.font.ebgaramond_italic, 400, FontStyle.Italic), variable(R.font.ebgaramond_italic, 700, FontStyle.Italic),
        ),
    ),
    Sans("Sans", FontFamily.SansSerif),
    Lexend("Lexend", FontFamily(variable(R.font.lexend, 400), variable(R.font.lexend, 700))),
    Dyslexic(
        "Dyslexia-friendly",
        FontFamily(
            Font(R.font.opendyslexic_regular, FontWeight.Normal),
            Font(R.font.opendyslexic_bold, FontWeight.Bold),
            Font(R.font.opendyslexic_italic, FontWeight.Normal, FontStyle.Italic),
        ),
    ),
}

/** User preferences, persisted in SharedPreferences and observable by Compose. */
class ReaderSettings(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val sp = appCtx.getSharedPreferences("reader", Context.MODE_PRIVATE)
    private val notesSp = appCtx.getSharedPreferences("reader_notes", Context.MODE_PRIVATE)

    var edition by mutableStateOf(enumOr(Edition.Modern, sp.getString("edition", null)))
    var themeMode by mutableStateOf(enumOr(ThemeMode.MorningEvening, sp.getString("themeMode", null)))
    var singleTheme by mutableStateOf(enumOr(ReaderTheme.Sepia, sp.getString("theme", null)))
    var morningTheme by mutableStateOf(enumOr(ReaderTheme.Sepia, sp.getString("morningTheme", null)))
    var eveningTheme by mutableStateOf(enumOr(ReaderTheme.Dark, sp.getString("eveningTheme", null)))
    var systemLightTheme by mutableStateOf(enumOr(ReaderTheme.Sepia, sp.getString("systemLightTheme", null)))
    var systemDarkTheme by mutableStateOf(enumOr(ReaderTheme.Dark, sp.getString("systemDarkTheme", null)))

    // Live context state used when themeMode is MorningEvening or System
    var viewingEvening by mutableStateOf(LocalDateTime.now().hour >= sp.getInt("switchHour", 12))
    var systemIsDark by mutableStateOf(false)

    fun rawThemeFor(evening: Boolean): ReaderTheme = when (themeMode) {
        ThemeMode.Single -> singleTheme
        ThemeMode.MorningEvening -> if (evening) eveningTheme else morningTheme
        ThemeMode.System -> if (systemIsDark) systemDarkTheme else systemLightTheme
    }

    fun themeFor(evening: Boolean): ReaderPalette = rawThemeFor(evening).palette(brightness)

    val theme: ReaderPalette
        get() = themeFor(viewingEvening)

    var font by mutableStateOf(enumOr(ReaderFont.Literata, sp.getString("font", null)))
    var fontSize by mutableFloatStateOf(sp.getFloat("fontSize", 20f))
    var lineSpacing by mutableFloatStateOf(sp.getFloat("lineSpacing", 1.6f))
    var margin by mutableFloatStateOf(sp.getFloat("margin", 24f))
    var brightness by mutableFloatStateOf(sp.getFloat("brightness", 1.0f))
    var justify by mutableStateOf(sp.getBoolean("justify", false))
    var switchHour by mutableIntStateOf(sp.getInt("switchHour", 12))

    /** How "today's reading" is decided; lets people read out of sync with the calendar. */
    var mode by mutableStateOf(enumOr(ReadingMode.Calendar, sp.getString("mode", null)))
    /** Plan mode: on real date [planStartEpochDay], the reader was on devotion day [planStartDay]. */
    var planStartEpochDay by mutableStateOf(sp.getLong("planStartEpochDay", LocalDate.now().toEpochDay()))
    var planStartDay by mutableIntStateOf(sp.getInt("planStartDay", 0))
    var lastPage by mutableIntStateOf(sp.getInt("lastPage", -1))
    var layout by mutableStateOf(enumOr(PageLayout.Elegant, sp.getString("layout", null)))
    var pageTurnStyle by mutableStateOf(enumOr(PageTurnStyle.Slide, sp.getString("pageTurnStyle", null)))
    var bibleProvider by mutableStateOf(enumOr(BibleProvider.YouVersion, sp.getString("bibleProvider", null)))
    var readMarkMode by mutableStateOf(enumOr(ReadMarkMode.BottomAndTimer, sp.getString("readMarkMode", null)))

    // Daily reminder notifications
    var morningReminder by mutableStateOf(sp.getBoolean("morningReminder", false))
    var morningHour by mutableIntStateOf(sp.getInt("morningHour", 7))
    var morningMinute by mutableIntStateOf(sp.getInt("morningMinute", 30))
    var eveningReminder by mutableStateOf(sp.getBoolean("eveningReminder", false))
    var eveningHour by mutableIntStateOf(sp.getInt("eveningHour", 20))
    var eveningMinute by mutableIntStateOf(sp.getInt("eveningMinute", 0))

    // Bookmarked / favourite readings (stored as page indices 0 until 732)
    var favorites by mutableStateOf(
        sp.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
    )

    fun isFavorite(page: Int): Boolean = page in favorites

    fun toggleFavorite(page: Int) {
        favorites = if (page in favorites) favorites - page else favorites + page
        save()
    }

    // Completed / read devotionals (stored as page indices 0 until 732)
    var readPages by mutableStateOf(
        sp.getStringSet("readPages", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
    )

    fun isRead(page: Int): Boolean = page in readPages

    fun markRead(page: Int) {
        if (page !in readPages) {
            readPages = readPages + page
            save()
        }
    }

    fun toggleRead(page: Int) {
        readPages = if (page in readPages) readPages - page else readPages + page
        save()
    }

    fun clearReadPages() {
        readPages = emptySet()
        save()
    }

    // Personal reflection notes per reading (keyed by page index 0 until 732)
    var notes by mutableStateOf<Map<Int, String>>(
        notesSp.all.mapNotNull { (k, v) ->
            val page = k.toIntOrNull()
            val text = (v as? String)?.takeIf { it.isNotBlank() }
            if (page != null && text != null) page to text else null
        }.toMap(),
    )

    fun noteFor(page: Int): String = notes[page].orEmpty()

    fun setNote(page: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            notes = notes - page
            notesSp.edit().remove(page.toString()).apply()
        } else {
            notes = notes + (page to trimmed)
            notesSp.edit().putString(page.toString(), trimmed).apply()
        }
    }

    /** The page that counts as "now" for the current reading mode. */
    fun currentPage(now: LocalDateTime = LocalDateTime.now()): Int {
        val slot = if (now.hour >= switchHour) 1 else 0
        return when (mode) {
            ReadingMode.Calendar -> Cal.pageFor(now, switchHour)
            ReadingMode.Plan -> {
                val elapsed = now.toLocalDate().toEpochDay() - planStartEpochDay
                val day = Math.floorMod(planStartDay + elapsed, 366L).toInt()
                day * 2 + slot
            }
            ReadingMode.Resume -> if (lastPage in 0 until Cal.PAGES) lastPage else Cal.pageFor(now, switchHour)
        }
    }

    /**
     * "Start from a day I choose" only: the real calendar date on which the reader will read [dayIndex],
     * e.g. "Today · Fri 6 Nov". Null in every other mode so nothing extra is shown.
     */
    fun planDateLabel(dayIndex: Int, now: LocalDate = LocalDate.now()): String? {
        if (mode != ReadingMode.Plan) return null
        val current = currentPage(now.atStartOfDay()) / 2
        var delta = Math.floorMod(dayIndex - current, 366)
        if (delta > 183) delta -= 366
        val date = now.plusDays(delta.toLong())
        val text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.getDefault()))
        return when (delta) {
            0 -> "Today · $text"
            1 -> "Tomorrow · $text"
            -1 -> "Yesterday · $text"
            else -> text
        }
    }

    fun startPlan(dayIndex: Int) {
        planStartDay = dayIndex
        planStartEpochDay = LocalDate.now().toEpochDay()
        mode = ReadingMode.Plan
        save()
    }

    fun save() {
        sp.edit()
            .putString("mode", mode.name).putString("layout", layout.name).putString("pageTurnStyle", pageTurnStyle.name)
            .putString("bibleProvider", bibleProvider.name)
            .putString("readMarkMode", readMarkMode.name).putLong("planStartEpochDay", planStartEpochDay)
            .putInt("planStartDay", planStartDay).putInt("lastPage", lastPage)
            .putString("edition", edition.name).putString("theme", singleTheme.name).putString("font", font.name)
            .putString("themeMode", themeMode.name)
            .putString("morningTheme", morningTheme.name).putString("eveningTheme", eveningTheme.name)
            .putString("systemLightTheme", systemLightTheme.name).putString("systemDarkTheme", systemDarkTheme.name)
            .putFloat("fontSize", fontSize).putFloat("lineSpacing", lineSpacing).putFloat("margin", margin)
            .putFloat("brightness", brightness)
            .putBoolean("justify", justify).putInt("switchHour", switchHour)
            .putBoolean("morningReminder", morningReminder).putInt("morningHour", morningHour).putInt("morningMinute", morningMinute)
            .putBoolean("eveningReminder", eveningReminder).putInt("eveningHour", eveningHour).putInt("eveningMinute", eveningMinute)
            .putStringSet("favorites", favorites.map { it.toString() }.toSet())
            .putStringSet("readPages", readPages.map { it.toString() }.toSet())
            .apply()
        VerseWidgetProvider.updateAll(appCtx)
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", LocalDateTime.now().toString())
        root.put("favorites", JSONArray(favorites.sorted()))
        root.put("readPages", JSONArray(readPages.sorted()))
        val notesObj = JSONObject()
        notes.toSortedMap().forEach { (k, v) -> notesObj.put(k.toString(), v) }
        root.put("notes", notesObj)
        val prefs = JSONObject().apply {
            put("edition", edition.name)
            put("themeMode", themeMode.name)
            put("singleTheme", singleTheme.name)
            put("morningTheme", morningTheme.name)
            put("eveningTheme", eveningTheme.name)
            put("systemLightTheme", systemLightTheme.name)
            put("systemDarkTheme", systemDarkTheme.name)
            put("font", font.name)
            put("fontSize", fontSize.toDouble())
            put("lineSpacing", lineSpacing.toDouble())
            put("margin", margin.toDouble())
            put("brightness", brightness.toDouble())
            put("justify", justify)
            put("switchHour", switchHour)
            put("mode", mode.name)
            put("planStartEpochDay", planStartEpochDay)
            put("planStartDay", planStartDay)
            put("layout", layout.name)
            put("pageTurnStyle", pageTurnStyle.name)
            put("bibleProvider", bibleProvider.name)
            put("readMarkMode", readMarkMode.name)
        }
        root.put("preferences", prefs)
        return root.toString(2)
    }

    fun importBackupJson(raw: String): String {
        val root = JSONObject(raw)
        val importedFavs = mutableSetOf<Int>()
        root.optJSONArray("favorites")?.let { arr ->
            for (i in 0 until arr.length()) {
                val p = arr.optInt(i, -1)
                if (p in 0 until Cal.PAGES) importedFavs += p
            }
        }
        val importedRead = mutableSetOf<Int>()
        root.optJSONArray("readPages")?.let { arr ->
            for (i in 0 until arr.length()) {
                val p = arr.optInt(i, -1)
                if (p in 0 until Cal.PAGES) importedRead += p
            }
        }
        val importedNotes = mutableMapOf<Int, String>()
        root.optJSONObject("notes")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val p = k.toIntOrNull()
                val text = obj.optString(k).trim()
                if (p != null && p in 0 until Cal.PAGES && text.isNotEmpty()) {
                    importedNotes[p] = text
                }
            }
        }

        favorites = favorites + importedFavs
        readPages = readPages + importedRead
        val mergedNotes = notes.toMutableMap().apply { putAll(importedNotes) }
        notes = mergedNotes
        val noteEditor = notesSp.edit()
        mergedNotes.forEach { (k, v) -> noteEditor.putString(k.toString(), v) }
        noteEditor.apply()

        root.optJSONObject("preferences")?.let { p ->
            edition = enumOr(edition, p.optString("edition"))
            themeMode = enumOr(themeMode, p.optString("themeMode"))
            singleTheme = enumOr(singleTheme, p.optString("singleTheme"))
            morningTheme = enumOr(morningTheme, p.optString("morningTheme"))
            eveningTheme = enumOr(eveningTheme, p.optString("eveningTheme"))
            systemLightTheme = enumOr(systemLightTheme, p.optString("systemLightTheme"))
            systemDarkTheme = enumOr(systemDarkTheme, p.optString("systemDarkTheme"))
            font = enumOr(font, p.optString("font"))
            if (p.has("fontSize")) fontSize = p.optDouble("fontSize", fontSize.toDouble()).toFloat()
            if (p.has("lineSpacing")) lineSpacing = p.optDouble("lineSpacing", lineSpacing.toDouble()).toFloat()
            if (p.has("margin")) margin = p.optDouble("margin", margin.toDouble()).toFloat()
            if (p.has("brightness")) brightness = p.optDouble("brightness", brightness.toDouble()).toFloat()
            if (p.has("justify")) justify = p.optBoolean("justify", justify)
            if (p.has("switchHour")) switchHour = p.optInt("switchHour", switchHour)
            mode = enumOr(mode, p.optString("mode"))
            if (p.has("planStartEpochDay")) planStartEpochDay = p.optLong("planStartEpochDay", planStartEpochDay)
            if (p.has("planStartDay")) planStartDay = p.optInt("planStartDay", planStartDay)
            layout = enumOr(layout, p.optString("layout"))
            pageTurnStyle = enumOr(pageTurnStyle, p.optString("pageTurnStyle"))
            bibleProvider = enumOr(bibleProvider, p.optString("bibleProvider"))
            readMarkMode = enumOr(readMarkMode, p.optString("readMarkMode"))
        }
        save()
        return "Restored ${importedFavs.size} saved, ${importedRead.size} read marks, and ${importedNotes.size} notes"
    }

    private companion object {
        inline fun <reified T : Enum<T>> enumOr(default: T, name: String?): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
