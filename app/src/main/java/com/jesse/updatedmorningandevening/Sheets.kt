package com.jesse.updatedmorningandevening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceSheet(s: ReaderSettings, onDismiss: () -> Unit) {
    val t = s.theme
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Theme", "Text", "Page")

    ModalBottomSheet(
        onDismissRequest = { s.save(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = t.bg,
        contentColor = t.fg,
        scrimColor = Color.Black.copy(alpha = 0.16f),
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = tab == i,
                        onClick = { tab = i },
                        shape = SegmentedButtonDefaults.itemShape(i, tabs.size),
                        modifier = Modifier.weight(1f),
                        icon = {},
                        label = { Text(label, fontSize = 13.sp, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            when (tab) {
                0 -> {
                    SectionLabel("COLOR MODE")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(
                                selected = s.themeMode == m,
                                onClick = { s.themeMode = m; s.save() },
                                label = { Text(m.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (s.themeMode) {
                        ThemeMode.Single -> {
                            ThemeSwatchRow(selected = s.singleTheme, currentTheme = t) {
                                s.singleTheme = it
                                s.save()
                            }
                        }
                        ThemeMode.MorningEvening -> {
                            Text("Morning reading", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            ThemeSwatchRow(selected = s.morningTheme, currentTheme = t, swatchSize = 44.dp) {
                                s.morningTheme = it
                                s.save()
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Evening reading", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            ThemeSwatchRow(selected = s.eveningTheme, currentTheme = t, swatchSize = 44.dp) {
                                s.eveningTheme = it
                                s.save()
                            }
                        }
                        ThemeMode.System -> {
                            Text("When phone is in Light mode", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            ThemeSwatchRow(selected = s.systemLightTheme, currentTheme = t, swatchSize = 44.dp) {
                                s.systemLightTheme = it
                                s.save()
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("When phone is in Dark mode", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            ThemeSwatchRow(selected = s.systemDarkTheme, currentTheme = t, swatchSize = 44.dp) {
                                s.systemDarkTheme = it
                                s.save()
                            }
                        }
                    }

                    SectionLabel("BRIGHTNESS")
                    LabeledSlider("☾", "☀", s.brightness, 0.25f..1.0f, t.accent) { s.brightness = it }
                }

                1 -> {
                    SectionLabel("FONT")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderFont.entries.forEach { f ->
                            FilterChip(
                                selected = f == s.font,
                                onClick = { s.font = f },
                                label = { Text(f.label, fontFamily = f.family) },
                            )
                        }
                    }

                    SectionLabel("TEXT SIZE")
                    LabeledSlider("A", "A", s.fontSize, 14f..34f, t.accent) { s.fontSize = it }

                    SectionLabel("LINE SPACING")
                    LabeledSlider("▤", "☰", s.lineSpacing, 1.2f..2.4f, t.accent) { s.lineSpacing = it }
                }

                2 -> {
                    SectionLabel("PAGE VIEW")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageLayout.entries.forEach { l ->
                            FilterChip(
                                selected = l == s.layout,
                                onClick = { s.layout = l; s.save() },
                                label = { Text(l.label) },
                            )
                        }
                    }
                    Text(
                        s.layout.blurb,
                        color = t.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    SectionLabel("PAGE TURN")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageTurnStyle.entries.forEach { pt ->
                            FilterChip(
                                selected = pt == s.pageTurnStyle,
                                onClick = { s.pageTurnStyle = pt; s.save() },
                                label = { Text(pt.label) },
                            )
                        }
                    }

                    SectionLabel("MARGINS")
                    LabeledSlider("▏▕", "▕   ▏", s.margin, 8f..56f, t.accent) { s.margin = it }

                    SectionLabel("ALIGNMENT")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !s.justify, onClick = { s.justify = false }, label = { Text("Left") })
                        FilterChip(selected = s.justify, onClick = { s.justify = true }, label = { Text("Justified") })
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchRow(
    selected: ReaderTheme,
    currentTheme: ReaderPalette,
    swatchSize: androidx.compose.ui.unit.Dp = 52.dp,
    onSelect: (ReaderTheme) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ReaderTheme.entries.forEach { th ->
            val isSel = th == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(th) },
            ) {
                Box(
                    Modifier.size(swatchSize).clip(CircleShape).background(th.bg)
                        .border(if (isSel) 3.dp else 1.dp, if (isSel) currentTheme.accent else currentTheme.muted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Aa", color = th.fg, fontWeight = FontWeight.SemiBold)
                }
                Text(th.label, color = currentTheme.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun LabeledSlider(lo: String, hi: String, value: Float, range: ClosedFloatingPointRange<Float>, accent: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(lo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Slider(value, onChange, valueRange = range, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
        Text(hi, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
    }
}

private fun hourLabel(h: Int) = when {
    h == 0 -> "12:00 AM"; h < 12 -> "$h:00 AM"; h == 12 -> "12:00 PM"; else -> "${h - 12}:00 PM"
}

private fun timeLabel(h: Int, m: Int): String {
    val ampm = if (h < 12) "AM" else "PM"
    val hr = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return "$hr:${m.toString().padStart(2, '0')} $ampm"
}

@Composable
private fun ExpandableSettingCard(
    title: String,
    summary: String,
    expanded: Boolean,
    t: ReaderPalette,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.04f),
        border = BorderStroke(
            1.dp,
            if (expanded) t.accent.copy(alpha = 0.45f) else t.fg.copy(alpha = 0.08f),
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = t.fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(summary, color = t.muted, fontSize = 13.sp, lineHeight = 17.sp)
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (expanded) t.accent else t.muted,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(
                        Modifier.padding(bottom = 12.dp),
                        color = t.muted.copy(alpha = 0.18f),
                    )
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    s: ReaderSettings,
    library: Library,
    onExport: (Edition) -> Unit,
    onOpenFavorites: () -> Unit,
    onDismiss: () -> Unit,
) {
    val t = s.theme
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var about by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    fun toggleSection(key: String) {
        expandedSection = if (expandedSection == key) null else key
    }

    val backupExporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val json = s.exportBackupJson()
                ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
            }.isSuccess
            android.widget.Toast.makeText(
                ctx,
                if (ok) "Backup saved" else "Couldn't save backup",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val backupImporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = ctx.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
                s.importBackupJson(raw)
            }.onSuccess { msg ->
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
            }.onFailure {
                android.widget.Toast.makeText(ctx, "Invalid backup file", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    if (pickStart) {
        DatePickerDialog(
            currentDay = s.currentPage() / 2, title = "Which reading do you want to read today?",
            message = "Tap a day. The app will show that reading today, then move forward one day at a time from there.",
            onDismiss = { pickStart = false },
        ) { day -> pickStart = false; s.startPlan(day) }
    }
    ModalBottomSheet(onDismissRequest = { s.save(); onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = t.bg, contentColor = t.fg) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            Text(
                "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 10.dp),
            )

            // 1. Saved readings & notes (direct tap)
            val savedOrNotedCount = (s.favorites + s.notes.keys).size
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.04f),
                border = BorderStroke(1.dp, t.fg.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onOpenFavorites),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = t.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saved readings & notes", color = t.fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (savedOrNotedCount == 0) "None saved yet"
                            else "$savedOrNotedCount saved",
                            color = t.muted,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = t.muted,
                    )
                }
            }

            // 2. Progress & backup
            ExpandableSettingCard(
                title = "Progress & backup",
                summary = "${s.readPages.size} of ${Cal.PAGES} read  ·  ${s.readMarkMode.label}",
                expanded = expandedSection == "progress",
                t = t,
                onToggle = { toggleSection("progress") },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${s.readPages.size} of ${Cal.PAGES} readings completed",
                        color = t.muted,
                        fontSize = 13.sp,
                    )
                    if (s.readPages.isNotEmpty()) {
                        TextButton(onClick = { s.clearReadPages() }) {
                            Text("Reset", color = t.accent, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Mark readings as read:", color = t.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadMarkMode.entries.forEach { m ->
                        FilterChip(
                            selected = s.readMarkMode == m,
                            onClick = { s.readMarkMode = m; s.save() },
                            label = { Text(m.label) },
                        )
                    }
                }
                Text(
                    s.readMarkMode.blurb,
                    color = t.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val date = java.time.LocalDate.now().toString()
                            backupExporter.launch("morning-and-evening-backup-$date.json")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Export backup", color = t.fg)
                    }
                    OutlinedButton(
                        onClick = { backupImporter.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import backup", color = t.fg)
                    }
                }
            }

            // 3. Edition & Scripture links
            ExpandableSettingCard(
                title = "Edition & Scripture links",
                summary = "${s.edition.label}  ·  ${s.bibleProvider.label}",
                expanded = expandedSection == "edition",
                t = t,
                onToggle = { toggleSection("edition") },
            ) {
                Text("EDITION", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Edition.entries.forEach { e ->
                        FilterChip(selected = e == s.edition, onClick = { s.edition = e; s.save() }, label = { Text(e.label) })
                    }
                }
                Text(s.edition.subtitle, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.height(14.dp))
                Text("SCRIPTURE LINKS (TAP A REFERENCE)", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BibleProvider.entries.forEach { p ->
                        FilterChip(
                            selected = p == s.bibleProvider,
                            onClick = { s.bibleProvider = p; s.save() },
                            label = { Text(p.label) },
                        )
                    }
                }
                Text(s.bibleProvider.blurb, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            // 4. Page view & page turn (kept in both Aa and Settings, collapsed to a single summary row)
            ExpandableSettingCard(
                title = "Page view & page turn",
                summary = "${s.layout.label}  ·  ${s.pageTurnStyle.label}",
                expanded = expandedSection == "layout",
                t = t,
                onToggle = { toggleSection("layout") },
            ) {
                Text("PAGE VIEW / LAYOUT", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                PageLayout.entries.forEach { l ->
                    val selected = s.layout == l
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) t.accent.copy(alpha = 0.14f) else Color.Transparent,
                        border = if (selected) BorderStroke(1.dp, t.accent) else null,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                            s.layout = l
                            s.save()
                            onDismiss()
                        },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selected,
                                onClick = { s.layout = l; s.save(); onDismiss() },
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(l.label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                Text(l.blurb, color = t.muted, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("PAGE TURN ANIMATION", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageTurnStyle.entries.forEach { pt ->
                        FilterChip(
                            selected = pt == s.pageTurnStyle,
                            onClick = { s.pageTurnStyle = pt; s.save() },
                            label = { Text(pt.label) },
                        )
                    }
                }
                Text(s.pageTurnStyle.blurb, color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            // 5. Reading schedule (Which day to open to + Switch to evening at)
            val scheduleModeSummary = when (s.mode) {
                ReadingMode.Calendar -> "Follow calendar"
                ReadingMode.Plan -> "Plan (${Cal.title(s.currentPage() / 2)})"
                ReadingMode.Resume -> "Resume last reading"
            }
            ExpandableSettingCard(
                title = "Reading schedule",
                summary = "$scheduleModeSummary  ·  Evening at ${hourLabel(s.switchHour)}",
                expanded = expandedSection == "schedule",
                t = t,
                onToggle = { toggleSection("schedule") },
            ) {
                Text("WHICH DAY TO OPEN TO", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                ReadingMode.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (m == ReadingMode.Plan) pickStart = true else { s.mode = m; s.save() }
                        }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = s.mode == m,
                            onClick = { if (m == ReadingMode.Plan) pickStart = true else { s.mode = m; s.save() } },
                        )
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(m.label, fontSize = 15.sp)
                            Text(m.blurb, color = t.muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
                if (s.mode == ReadingMode.Plan) {
                    val todayDay = s.currentPage() / 2
                    val tomorrowDay = (todayDay + 1) % 366
                    val started = java.time.LocalDate.ofEpochDay(s.planStartEpochDay)
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = t.accent.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Today you're reading: ${Cal.title(todayDay)}", color = t.fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tomorrow: ${Cal.title(tomorrowDay)}", color = t.fg, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                            Text("Your plan began on $started at ${Cal.title(s.planStartDay)}.", color = t.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    TextButton(onClick = { pickStart = true }) { Text("Start again from a different day…", color = t.accent) }
                }

                Spacer(Modifier.height(14.dp))
                Text("SWITCH TO EVENING AT", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { s.switchHour = (s.switchHour - 1).coerceAtLeast(0) }) { Text("−") }
                    Text(hourLabel(s.switchHour), fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { s.switchHour = (s.switchHour + 1).coerceAtMost(23) }) { Text("+") }
                }
                Text(
                    "The app opens to the morning reading before this time and the evening reading after it.",
                    color = t.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // 6. Daily reminders
            val reminderSummary = when {
                s.morningReminder && s.eveningReminder ->
                    "Morning ${timeLabel(s.morningHour, s.morningMinute)}  ·  Evening ${timeLabel(s.eveningHour, s.eveningMinute)}"
                s.morningReminder -> "Morning at ${timeLabel(s.morningHour, s.morningMinute)}"
                s.eveningReminder -> "Evening at ${timeLabel(s.eveningHour, s.eveningMinute)}"
                else -> "Off"
            }
            ExpandableSettingCard(
                title = "Daily reminders",
                summary = reminderSummary,
                expanded = expandedSection == "reminders",
                t = t,
                onToggle = { toggleSection("reminders") },
            ) {
                ReminderSection(s, t)
            }

            // 7. Home screen widget & Offline book (EPUB)
            val widgetPinned = remember { VerseWidgetProvider.isPinned(ctx) }
            ExpandableSettingCard(
                title = "Home screen widget & EPUB book",
                summary = if (widgetPinned) "Widget active  ·  Download Modern or Classic EPUB"
                else "Add Today's Verse widget  ·  Download EPUB",
                expanded = expandedSection == "extras",
                t = t,
                onToggle = { toggleSection("extras") },
            ) {
                Text("HOME SCREEN WIDGET", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                WidgetSection(t)

                Spacer(Modifier.height(16.dp))
                Text("DOWNLOAD THE FULL BOOK (EPUB)", color = t.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onExport(Edition.Modern) }, modifier = Modifier.weight(1f)) { Text("Modern") }
                    Button(onClick = { onExport(Edition.Classic) }, modifier = Modifier.weight(1f)) { Text("Classic") }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { about = true }) {
                    Text("About this edition", color = t.accent)
                }
                Text("·", color = t.muted)
                TextButton(onClick = { privacy = true }) {
                    Text("Privacy Policy", color = t.accent)
                }
            }
        }
    }
    if (about) {
        AlertDialog(
            onDismissRequest = { about = false },
            containerColor = t.bg, titleContentColor = t.fg, textContentColor = t.fg,
            confirmButton = { TextButton(onClick = { about = false }) { Text("Close", color = t.accent) } },
            title = { Text("About Updated Morning and Evening") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    library.about.forEach {
                        if (it.heading) Text(it.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        else Text(markup(it.text), fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            },
        )
    }
    if (privacy) {
        AlertDialog(
            onDismissRequest = { privacy = false },
            containerColor = t.bg, titleContentColor = t.fg, textContentColor = t.fg,
            confirmButton = { TextButton(onClick = { privacy = false }) { Text("Close", color = t.accent) } },
            title = { Text("Privacy Policy") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("100% Offline & Private", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Updated Morning and Evening is designed with complete privacy by default. The app does not request Internet permission and cannot transmit any data off your device.",
                        fontSize = 14.sp, lineHeight = 21.sp,
                    )
                    Text("Zero Data Collection", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "We do not collect, store, share, or sell any personal information, usage analytics, or diagnostics. There are no user accounts, no advertisements, and no third-party tracking SDKs.",
                        fontSize = 14.sp, lineHeight = 21.sp,
                    )
                    Text("Local On-Device Storage", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Your reading preferences, completed devotionals, saved readings, and personal reflection notes are stored 100% locally on your device. When you export a JSON backup or EPUB book, the file is saved directly to the location you choose on your device.",
                        fontSize = 14.sp, lineHeight = 21.sp,
                    )
                    Text("Permissions & External Links", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Notification permission is requested only if you enable optional daily reading reminders, which are scheduled locally on your device. Tapping a Scripture reference opens your chosen Bible provider (YouVersion, Bible Gateway, or Blue Letter Bible) in your external browser or Bible app.",
                        fontSize = 14.sp, lineHeight = 21.sp,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatePickerDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    title: String = "Jump to a day",
    message: String? = null,
    readPages: Set<Int> = emptySet(),
    savedCount: Int = 0,
    onOpenFavorites: (() -> Unit)? = null,
    onPick: (dayIndex: Int) -> Unit,
) {
    var month by remember { mutableIntStateOf(Cal.monthDay(currentDay).first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (onOpenFavorites != null) {
                    OutlinedButton(
                        onClick = onOpenFavorites,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (savedCount == 0) "Saved readings" else "Saved readings ($savedCount)")
                    }
                }
                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cal.monthNames.forEachIndexed { i, name ->
                        FilterChip(selected = month == i + 1, onClick = { month = i + 1 }, label = { Text(name.take(3)) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                val (curM, curD) = Cal.monthDay(currentDay)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in 1..Cal.daysIn[month - 1]) {
                        val selected = month == curM && d == curD
                        val dIdx = Cal.dayIndex(month, d)
                        val mDone = (dIdx * 2) in readPages
                        val eDone = (dIdx * 2 + 1) in readPages
                        val dotActive = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                        val dotIdle = (if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.25f)
                        Column(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onPick(dIdx) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "$d",
                                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                            )
                            if (mDone || eDone) {
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (mDone) dotActive else dotIdle))
                                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (eDone) dotActive else dotIdle))
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ReminderSection(s: ReaderSettings, t: ReaderPalette) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var pendingSlot by remember { mutableStateOf<Boolean?>(null) }
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val evening = pendingSlot
        pendingSlot = null
        if (granted && evening != null) {
            if (evening) s.eveningReminder = true else s.morningReminder = true
            s.save()
            Reminders.scheduleAll(ctx, s)
        } else if (!granted) {
            android.widget.Toast.makeText(ctx, "Notification permission is needed for daily reminders", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun toggle(evening: Boolean, enabled: Boolean) {
        if (enabled && android.os.Build.VERSION.SDK_INT >= 33 && !Reminders.hasNotificationPermission(ctx)) {
            pendingSlot = evening
            permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (evening) s.eveningReminder = enabled else s.morningReminder = enabled
        s.save()
        Reminders.scheduleAll(ctx, s)
    }

    fun pickTime(evening: Boolean) {
        val initH = if (evening) s.eveningHour else s.morningHour
        val initM = if (evening) s.eveningMinute else s.morningMinute
        android.app.TimePickerDialog(
            ctx,
            { _, h, m ->
                if (evening) {
                    s.eveningHour = h
                    s.eveningMinute = m
                } else {
                    s.morningHour = h
                    s.morningMinute = m
                }
                s.save()
                Reminders.scheduleAll(ctx, s)
            },
            initH,
            initM,
            false,
        ).show()
    }

    ReminderRow(
        label = "Morning reading",
        time = timeLabel(s.morningHour, s.morningMinute),
        enabled = s.morningReminder,
        onTimeClick = { pickTime(evening = false) },
        onToggle = { toggle(evening = false, enabled = it) },
    )
    Spacer(Modifier.height(8.dp))
    ReminderRow(
        label = "Evening reading",
        time = timeLabel(s.eveningHour, s.eveningMinute),
        enabled = s.eveningReminder,
        onTimeClick = { pickTime(evening = true) },
        onToggle = { toggle(evening = true, enabled = it) },
    )
    Text(
        "Sends a gentle notification with the day's Scripture verse.",
        color = t.muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp),
    )
    if (s.morningReminder || s.eveningReminder) {
        TextButton(
            onClick = {
                val eveningNow = java.time.LocalDateTime.now().hour >= s.switchHour
                Reminders.showNotification(ctx, eveningNow)
            },
        ) {
            Text("Send test notification", color = t.accent)
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    time: String,
    enabled: Boolean,
    onTimeClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onTimeClick, modifier = Modifier.padding(end = 12.dp)) {
            Text(time)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun WidgetSection(t: ReaderPalette) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pinned = remember { VerseWidgetProvider.isPinned(ctx) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.045f),
        border = BorderStroke(1.dp, t.fg.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (pinned) "Today's Verse widget is on your Home screen"
                else "Today's Verse on your Home screen",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Shows the current Morning or Evening Scripture verse and reference right on your Home screen, matching your chosen edition and theme.",
                color = t.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val ok = VerseWidgetProvider.requestPin(ctx)
                    if (!ok) {
                        android.widget.Toast.makeText(
                            ctx,
                            "Long-press an empty spot on your Home screen → Widgets → Today's Verse",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (pinned) "Add another widget to Home screen" else "Add widget to Home screen")
            }
            Text(
                "Tip: You can also long-press an empty space on your Home screen → Widgets → Updated Morning and Evening.",
                color = t.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    s: ReaderSettings,
    library: Library,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = s.theme
    val sorted = remember(s.favorites, s.notes) { (s.favorites + s.notes.keys).sorted() }
    val edData = library.edition(s.edition)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = t.bg,
        contentColor = t.fg,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = t.accent)
                Text("Saved readings & notes", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))

            if (sorted.isEmpty()) {
                Text(
                    "You haven't saved any readings or written any notes yet.\n\nTap ♡ to save a reading, or tap \"Add a note…\" at the bottom of any devotion.",
                    color = t.muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                sorted.forEach { page ->
                    val dayIdx = page / 2
                    val evening = page % 2 == 1
                    val (month, day) = Cal.monthDay(dayIdx)
                    val sec = edData.get(month, day, evening) ?: library.edition(Edition.Classic).get(month, day, evening)
                    val isFav = s.isFavorite(page)
                    val note = s.noteFor(page)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = t.fg.copy(alpha = if (t.dark) 0.06f else 0.045f),
                        border = BorderStroke(1.dp, t.fg.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelectPage(page) },
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${Cal.title(dayIdx)} · ${if (evening) "Evening" else "Morning"}",
                                    color = t.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (sec != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        markup(sec.verse),
                                        color = t.fg,
                                        fontSize = 15.sp,
                                        lineHeight = 21.sp,
                                        maxLines = 2,
                                    )
                                    if (sec.ref.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "— ${sec.ref}",
                                            color = t.muted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                if (note.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = t.accent.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            note,
                                            color = t.fg,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { s.toggleFavorite(page) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFav) "Remove from saved" else "Save reading",
                                    tint = if (isFav) t.accent else t.muted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteDialog(
    s: ReaderSettings,
    page: Int,
    onDismiss: () -> Unit,
) {
    val t = s.theme
    val dayIdx = page / 2
    val evening = page % 2 == 1
    val existing = s.noteFor(page)
    var text by remember(page) { mutableStateOf(existing) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.bg,
        titleContentColor = t.fg,
        textContentColor = t.fg,
        title = {
            Column {
                Text("Personal Note", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${Cal.title(dayIdx)} · ${if (evening) "Evening" else "Morning"}",
                    color = t.accent,
                    fontSize = 13.sp,
                )
            }
        },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write a reflection, prayer, or thought…", color = t.muted) },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    s.setNote(page, text)
                    onDismiss()
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existing.isNotBlank()) {
                    TextButton(
                        onClick = {
                            s.setNote(page, "")
                            onDismiss()
                        },
                    ) {
                        Text("Delete", color = t.muted)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = t.accent)
                }
            }
        },
    )
}
