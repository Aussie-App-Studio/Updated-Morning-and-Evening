package com.jesse.updatedmorningandevening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    settings: ReaderSettings,
    library: Library,
    openSlotRequest: Pair<Boolean, Long>? = null,
    onExport: (Edition) -> Unit,
) {
    val theme = settings.theme
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = settings.currentPage()) { Cal.PAGES }
    LaunchedEffect(openSlotRequest) {
        val req = openSlotRequest ?: return@LaunchedEffect
        val todayDay = settings.currentPage() / 2
        pager.scrollToPage(todayDay * 2 + if (req.first) 1 else 0)
    }
    var chrome by remember { mutableStateOf(true) }
    var showAa by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDates by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var editingNotePage by remember { mutableStateOf<Int?>(null) }

    val todayPage = settings.currentPage()
    val page = pager.currentPage
    val dayIndex = page / 2
    val evening = page % 2 == 1
    settings.viewingEvening = evening
    val isFav = settings.isFavorite(page)

    // Automatically keep the screen awake for 5 minutes from the last interaction (tap, scroll, or page change).
    val view = LocalView.current
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(page, chrome) { lastInteraction = System.currentTimeMillis() }
    LaunchedEffect(lastInteraction) {
        view.keepScreenOn = true
        delay(5 * 60_000L)
        view.keepScreenOn = false
    }
    DisposableEffect(view) {
        onDispose { view.keepScreenOn = false }
    }

    // Remember the last reading (used by "Resume where I left off").
    LaunchedEffect(page) {
        if (settings.lastPage != page) { settings.lastPage = page; settings.save() }
    }
    // Changing the reading mode / start date jumps straight to the new "today".
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(settings.mode, settings.planStartDay, settings.planStartEpochDay) {
        if (firstRun) { firstRun = false; return@LaunchedEffect }
        if (settings.mode != ReadingMode.Resume) pager.scrollToPage(settings.currentPage())
    }

    // Returning after a long break: jump to whatever reading is current now.
    var leftAt by remember { mutableStateOf(0L) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { leftAt = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (leftAt != 0L && settings.mode != ReadingMode.Resume && System.currentTimeMillis() - leftAt > 30 * 60_000L) {
            scope.launch { pager.scrollToPage(settings.currentPage()) }
        }
    }

    // Material components inside the chrome take their colours from the reading theme.
    val scheme = if (theme.dark) darkColorScheme(primary = theme.accent, onSurface = theme.fg, surface = theme.bg, onSurfaceVariant = theme.muted, secondaryContainer = theme.fg.copy(alpha = .14f), onSecondaryContainer = theme.fg, outline = theme.muted)
    else lightColorScheme(primary = theme.accent, onSurface = theme.fg, surface = theme.bg, onSurfaceVariant = theme.muted, secondaryContainer = theme.fg.copy(alpha = .10f), onSecondaryContainer = theme.fg, outline = theme.muted)

    fun turnToPage(target: Int) {
        val clamped = target.coerceIn(0, Cal.PAGES - 1)
        if (clamped == pager.currentPage) return
        scope.launch {
            when (settings.pageTurnStyle) {
                PageTurnStyle.None -> pager.scrollToPage(clamped)
                PageTurnStyle.PageCurl -> pager.animateScrollToPage(clamped, animationSpec = tween(480))
                PageTurnStyle.BookCurl -> pager.animateScrollToPage(clamped, animationSpec = tween(420))
                PageTurnStyle.Depth -> pager.animateScrollToPage(clamped, animationSpec = tween(360))
                PageTurnStyle.Slide -> pager.animateScrollToPage(clamped, animationSpec = tween(320))
            }
        }
    }

    MaterialTheme(colorScheme = scheme) {
        Box(Modifier.fillMaxSize().background(theme.bg)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { p ->
                val pageOffset = ((p - pager.currentPage) - pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                val turnStyle = settings.pageTurnStyle
                val pagePalette = settings.themeFor(p % 2 == 1)
                val zOrder = when (turnStyle) {
                    PageTurnStyle.PageCurl, PageTurnStyle.BookCurl, PageTurnStyle.Depth -> if (pageOffset <= 0f) 2f else 1f
                    else -> 0f
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(zOrder)
                        .graphicsLayer {
                            when (turnStyle) {
                                PageTurnStyle.Slide -> Unit
                                PageTurnStyle.PageCurl -> {
                                    translationX = -pageOffset * size.width
                                }
                                PageTurnStyle.BookCurl -> {
                                    translationX = -pageOffset * size.width
                                    if (pageOffset < 0f) {
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        cameraDistance = 18f * density
                                        rotationY = pageOffset * 84f
                                        alpha = (1f + pageOffset * 0.22f).coerceIn(0f, 1f)
                                    }
                                }
                                PageTurnStyle.Depth -> {
                                    if (pageOffset < 0f) {
                                        shadowElevation = (-pageOffset * 20f).dp.toPx()
                                    } else if (pageOffset > 0f) {
                                        translationX = -pageOffset * size.width
                                        val s = 1f - 0.09f * pageOffset
                                        scaleX = s
                                        scaleY = s
                                        alpha = (1f - 0.45f * pageOffset).coerceIn(0.4f, 1f)
                                    }
                                }
                                PageTurnStyle.None -> {
                                    translationX = -pageOffset * size.width
                                    alpha = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0f
                                }
                            }
                        }
                        .drawWithContent {
                            if (turnStyle == PageTurnStyle.PageCurl && pageOffset < -0.001f && pageOffset > -0.999f) {
                                val w = size.width
                                val h = size.height
                                val progress = (-pageOffset).coerceIn(0f, 1f)
                                val arch = kotlin.math.sin(progress * Math.PI).toFloat()

                                // Diagonal crease line sweeping from right edge (w) to left spine (0),
                                // with the lower corner leading slightly like a natural finger peel.
                                val foldX = w * (1f - progress)
                                val tiltX = w * 0.14f * arch
                                val topFoldX = foldX + tiltX * 0.55f
                                val botFoldX = foldX - tiltX * 0.45f

                                // Conical paper curl width (wider near the bottom peel, narrower near the top)
                                val baseCurlW = w * (0.25f * arch + 0.07f * (1f - progress) * progress * 4f)
                                val topCurlW = baseCurlW * 0.72f
                                val botCurlW = baseCurlW * 1.14f
                                val topFlapX = topFoldX - topCurlW
                                val botFlapX = botFoldX - botCurlW

                                // Normal vector perpendicular to the crease (pointing rightwards)
                                val dx = botFoldX - topFoldX
                                val dy = h
                                val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
                                val nx = dy / len
                                val ny = -dx / len
                                val midFoldX = (topFoldX + botFoldX) * 0.5f
                                val midFoldY = h * 0.5f

                                // 1. Unpeeled flat front of the current page (to the left of the crease)
                                val frontPath = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(topFoldX, 0f)
                                    lineTo(botFoldX, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                clipPath(frontPath) {
                                    this@drawWithContent.drawContent()

                                    // Shadow cast by the curled flap back onto the flat front page
                                    val backShadowW = (baseCurlW * 0.45f).coerceAtLeast(18f)
                                    val midFlapX = (topFlapX + botFlapX) * 0.5f
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.22f * arch),
                                            ),
                                            start = Offset(midFlapX - nx * backShadowW, midFoldY - ny * backShadowW),
                                            end = Offset(midFlapX, midFoldY),
                                        ),
                                    )
                                }

                                // 2. Cast shadow onto the next devotion underneath (to the right of the crease)
                                val castShadowW = w * 0.18f * arch + 24f * (1f - progress)
                                val castPath = Path().apply {
                                    moveTo(topFoldX, 0f)
                                    lineTo(topFoldX + castShadowW, 0f)
                                    lineTo(botFoldX + castShadowW, h)
                                    lineTo(botFoldX, h)
                                    close()
                                }
                                clipPath(castPath) {
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.34f * (1f - progress * 0.35f)),
                                                Color.Black.copy(alpha = 0.10f * (1f - progress * 0.35f)),
                                                Color.Transparent,
                                            ),
                                            start = Offset(midFoldX, midFoldY),
                                            end = Offset(midFoldX + nx * castShadowW, midFoldY + ny * castShadowW),
                                        ),
                                    )
                                }

                                // 3. The 3D curled paper underside flap
                                val flapPath = Path().apply {
                                    moveTo(topFlapX, 0f)
                                    lineTo(topFoldX, 0f)
                                    lineTo(botFoldX, h)
                                    lineTo(botFlapX, h)
                                    close()
                                }
                                val backsideColor = if (pagePalette.dark) {
                                    lerp(pagePalette.bg, Color.White, 0.08f)
                                } else {
                                    lerp(pagePalette.bg, Color(0xFFFDF8EE), 0.35f)
                                }
                                clipPath(flapPath) {
                                    drawRect(backsideColor)

                                    // Faintly mirrored page content bleeding through the back of the paper
                                    val foldAngleDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dy.toDouble())).toFloat()
                                    withTransform({
                                        translate(midFoldX, midFoldY)
                                        rotate(-foldAngleDeg)
                                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
                                        rotate(foldAngleDeg)
                                        translate(-midFoldX, -midFoldY)
                                    }) {
                                        this@drawWithContent.drawContent()
                                    }
                                    // Paper opacity veil over the reversed bleed-through text
                                    drawRect(backsideColor.copy(alpha = if (pagePalette.dark) 0.85f else 0.82f))

                                    // 3D cylindrical roll lighting & crease shading across the flap
                                    val midFlapX = (topFlapX + botFlapX) * 0.5f
                                    val highlightAlpha = if (pagePalette.dark) 0.14f else 0.45f
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            0.00f to Color.Black.copy(alpha = 0.16f),
                                            0.22f to Color.Transparent,
                                            0.68f to Color.White.copy(alpha = highlightAlpha),
                                            0.88f to Color.Black.copy(alpha = 0.05f),
                                            1.00f to Color.Black.copy(alpha = 0.24f),
                                            start = Offset(midFlapX, midFoldY),
                                            end = Offset(midFoldX, midFoldY),
                                        ),
                                    )
                                }
                            } else {
                                drawContent()
                                when (turnStyle) {
                                    PageTurnStyle.PageCurl -> {
                                        if (pageOffset > 0f) {
                                            val underShadow = (pageOffset * 0.12f).coerceIn(0f, 0.12f)
                                            drawRect(Color.Black.copy(alpha = underShadow))
                                        }
                                    }
                                    PageTurnStyle.BookCurl -> {
                                        if (pageOffset < 0f) {
                                            val shade = (-pageOffset * 0.16f).coerceIn(0f, 0.16f)
                                            drawRect(Color.Black.copy(alpha = shade))
                                        } else if (pageOffset > 0f) {
                                            val underShadow = (pageOffset * 0.24f).coerceIn(0f, 0.24f)
                                            drawRect(Color.Black.copy(alpha = underShadow))
                                        }
                                    }
                                    PageTurnStyle.Depth -> {
                                        if (pageOffset > 0f) {
                                            val underShadow = (pageOffset * 0.18f).coerceIn(0f, 0.18f)
                                            drawRect(Color.Black.copy(alpha = underShadow))
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        },
                ) {
                    ReadingPage(
                        settings = settings, library = library, page = p, isCurrentPage = p == page,
                        onEditNote = { editingNotePage = p },
                        onTap = { x, width ->
                            // Edges turn the page, the middle toggles the controls.
                            when {
                                x < width * 0.15f && p > 0 -> turnToPage(p - 1)
                                x > width * 0.85f && p < Cal.PAGES - 1 -> turnToPage(p + 1)
                                else -> chrome = !chrome
                            }
                        },
                        onScrolled = {
                            lastInteraction = System.currentTimeMillis()
                            if (chrome) chrome = false
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = chrome, modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(200)), exit = fadeOut(tween(200)),
            ) {
                Surface(color = theme.bg.copy(alpha = .96f), tonalElevation = 0.dp, shadowElevation = 2.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(WindowInsets.systemBars.asPaddingValues().let { PaddingValues(top = it.calculateTopPadding()) })
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { showDates = true }) {
                            Text(Cal.title(dayIndex), color = theme.fg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            val slotLabel = if (evening) "Evening" else "Morning"
                            Text(
                                if (settings.isRead(page)) "$slotLabel · ✓ Read" else slotLabel,
                                color = if (settings.isRead(page)) theme.accent else theme.muted,
                                fontSize = 12.sp,
                            )
                        }
                        EditionPill(settings)
                        IconButton(onClick = { settings.toggleFavorite(page) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFav) "Remove from saved" else "Save reading",
                                tint = if (isFav) theme.accent else theme.fg,
                            )
                        }
                        TextButton(onClick = { showAa = true }) {
                            Text("Aa", color = theme.fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, "Settings", tint = theme.fg)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = chrome, modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(200)), exit = fadeOut(tween(200)),
            ) {
                Surface(color = theme.bg.copy(alpha = .96f), shadowElevation = 2.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                            listOf("Morning", "Evening").forEachIndexed { i, label ->
                                val slotDone = settings.isRead(dayIndex * 2 + i)
                                SegmentedButton(
                                    selected = (i == 1) == evening,
                                    onClick = { turnToPage(dayIndex * 2 + i) },
                                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                                    modifier = Modifier.weight(1f),
                                    icon = {},
                                    label = { Text(if (slotDone) "$label ✓" else label, maxLines = 1) },
                                )
                            }
                        }
                        if (page != todayPage && settings.mode != ReadingMode.Resume) {
                            TextButton(onClick = { turnToPage(todayPage) }) {
                                Icon(Icons.Filled.Today, null, tint = theme.accent)
                                Spacer(Modifier.padding(start = 4.dp))
                                Text("Today", color = theme.accent)
                            }
                        }
                    }
                }
            }

            // Warm in-app dimmer overlay (dims the reading page without changing phone-wide brightness)
            val dimAlpha = (1f - settings.brightness).coerceIn(0f, 0.78f)
            if (dimAlpha > 0.01f) {
                Box(
                    Modifier.fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF140C04).copy(alpha = dimAlpha)),
                )
            }
        }

        if (showAa) AppearanceSheet(settings) { showAa = false }
        if (showSettings) {
            SettingsSheet(
                s = settings,
                library = library,
                onExport = onExport,
                onOpenFavorites = {
                    showSettings = false
                    showFavorites = true
                },
                onDismiss = { showSettings = false },
            )
        }
        if (showDates) {
            DatePickerDialog(
                currentDay = dayIndex,
                readPages = settings.readPages,
                savedCount = (settings.favorites + settings.notes.keys).size,
                onOpenFavorites = {
                    showDates = false
                    showFavorites = true
                },
                onDismiss = { showDates = false },
            ) { picked ->
                showDates = false
                scope.launch { pager.scrollToPage(picked * 2 + (if (evening) 1 else 0)) }
            }
        }
        if (showFavorites) {
            FavoritesSheet(
                s = settings,
                library = library,
                onSelectPage = { targetPage ->
                    showFavorites = false
                    scope.launch { pager.scrollToPage(targetPage) }
                },
                onDismiss = { showFavorites = false },
            )
        }
        editingNotePage?.let { notePage ->
            NoteDialog(
                s = settings,
                page = notePage,
                onDismiss = { editingNotePage = null },
            )
        }
    }
}

@Composable
private fun PlanDateChip(label: String, t: ReaderPalette) {
    Surface(
        shape = RoundedCornerShape(50), color = t.accent.copy(alpha = .12f),
        modifier = Modifier.padding(bottom = 18.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = t.accent, modifier = Modifier.size(14.dp))
            Text("Your day: $label", color = t.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EditionPill(settings: ReaderSettings) {
    val t = settings.theme
    val modern = settings.edition == Edition.Modern
    Surface(
        shape = RoundedCornerShape(50), color = t.accent.copy(alpha = .16f),
        modifier = Modifier.clickable {
            settings.edition = if (modern) Edition.Classic else Edition.Modern
            settings.save()
        },
    ) {
        Text(
            if (modern) "Modern" else "Classic", color = t.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ReadingPage(
    settings: ReaderSettings, library: Library, page: Int, isCurrentPage: Boolean,
    onEditNote: () -> Unit,
    onTap: (x: Float, width: Float) -> Unit, onScrolled: () -> Unit,
) {
    val (month, day) = Cal.monthDay(page / 2)
    val evening = page % 2 == 1
    val t = settings.themeFor(evening)
    val wanted = settings.edition
    val primary = library.edition(wanted).get(month, day, evening)
    val fallback = if (primary == null) library.edition(Edition.Classic).get(month, day, evening) else null
    val section = primary ?: fallback
    val isFav = settings.isFavorite(page)
    val isRead = settings.isRead(page)
    val note = settings.noteFor(page)
    val hasNote = note.isNotBlank()

    val scroll = rememberScrollState()
    var timerElapsed by remember(page, isCurrentPage) { mutableStateOf(false) }
    var reachedBottom by remember(page, isCurrentPage) { mutableStateOf(false) }

    LaunchedEffect(page, isCurrentPage, settings.readMarkMode) {
        if (!isCurrentPage) return@LaunchedEffect
        val mode = settings.readMarkMode
        if (mode == ReadMarkMode.BottomAndTimer || mode == ReadMarkMode.TimerOnly) {
            delay(15_000L)
            timerElapsed = true
            val fitsOnScreen = scroll.maxValue <= 80
            if (mode == ReadMarkMode.TimerOnly || reachedBottom || fitsOnScreen) {
                settings.markRead(page)
            }
        }
    }

    LaunchedEffect(scroll, isCurrentPage, settings.readMarkMode) {
        var last = 0
        androidx.compose.runtime.snapshotFlow { scroll.value to scroll.maxValue }.collect { (v, max) ->
            if (kotlin.math.abs(v - last) > 24) { onScrolled(); last = v }
            if (isCurrentPage && max > 80 && v >= max - 80) {
                reachedBottom = true
                val mode = settings.readMarkMode
                if (mode == ReadMarkMode.BottomOnly || (mode == ReadMarkMode.BottomAndTimer && timerElapsed)) {
                    settings.markRead(page)
                }
            }
        }
    }
    val bars = WindowInsets.systemBars.asPaddingValues()
    val align = if (settings.justify) TextAlign.Justify else TextAlign.Start
    val body = TextStyle(
        fontFamily = settings.font.family, fontSize = settings.fontSize.sp,
        lineHeight = (settings.fontSize * settings.lineSpacing).sp, color = t.fg, textAlign = align,
    )

    Column(
        Modifier.fillMaxSize()
            .background(t.bg)
            .pointerInput(Unit) { detectTapGestures { off -> onTap(off.x, size.width.toFloat()) } }
            .verticalScroll(scroll)
            .padding(horizontal = settings.margin.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(bars.calculateTopPadding() + 76.dp))
        settings.planDateLabel(page / 2)?.let { PlanDateChip(it, t) }
        SelectionContainer {
            ReadingContent(settings, month, day, evening, section, fallback != null, body)
        }
        if (hasNote) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = t.accent.copy(alpha = if (t.dark) 0.13f else 0.09f),
                border = BorderStroke(1.dp, t.accent.copy(alpha = 0.30f)),
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().clickable { onEditNote() },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "YOUR NOTE",
                        color = t.accent,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        note,
                        style = body.copy(fontSize = (settings.fontSize * 0.90f).sp, lineHeight = (settings.fontSize * 1.35f).sp, textAlign = TextAlign.Start),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isRead) t.accent.copy(alpha = 0.14f) else t.fg.copy(alpha = 0.05f),
                border = if (isRead) BorderStroke(1.dp, t.accent.copy(alpha = 0.45f)) else null,
                modifier = Modifier.clickable { settings.toggleRead(page) },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = if (isRead) t.accent else t.muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (isRead) "Read" else "Mark as read",
                        color = if (isRead) t.accent else t.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isFav) t.accent.copy(alpha = 0.14f) else t.fg.copy(alpha = 0.05f),
                border = if (isFav) BorderStroke(1.dp, t.accent.copy(alpha = 0.45f)) else null,
                modifier = Modifier.clickable { settings.toggleFavorite(page) },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFav) t.accent else t.muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (isFav) "Saved" else "Save",
                        color = if (isFav) t.accent else t.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (hasNote) t.accent.copy(alpha = 0.14f) else t.fg.copy(alpha = 0.05f),
                border = if (hasNote) BorderStroke(1.dp, t.accent.copy(alpha = 0.45f)) else null,
                modifier = Modifier.clickable { onEditNote() },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = if (hasNote) t.accent else t.muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (hasNote) "Note" else "Add a note…",
                        color = if (hasNote) t.accent else t.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(bars.calculateBottomPadding() + 96.dp))
    }
}

/** Minimal <i>/<b> markup used in the content assets. */
fun markup(s: String): AnnotatedString = buildAnnotatedString {
    var italic = false
    var bold = false
    var style = SpanStyle()
    fun apply() { style = SpanStyle(fontStyle = if (italic) FontStyle.Italic else null, fontWeight = if (bold) FontWeight.Bold else null) }
    Regex("<(/?)([ib])>").let { re ->
        var pos = 0
        for (m in re.findAll(s)) {
            withStyle(style) { append(s.substring(pos, m.range.first)) }
            val on = m.groupValues[1].isEmpty()
            if (m.groupValues[2] == "i") italic = on else bold = on
            apply()
            pos = m.range.last + 1
        }
        withStyle(style) { append(s.substring(pos)) }
    }
}
