package com.jesse.updatedmorningandevening

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var openSlotRequest by mutableStateOf<Pair<Boolean, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = ReaderSettings(applicationContext)
        Reminders.scheduleAll(applicationContext, settings)
        handleReminderIntent(intent)
        setContent { App(settings, openSlotRequest) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleReminderIntent(intent)
    }

    private fun handleReminderIntent(intent: android.content.Intent?) {
        if (intent?.hasExtra(Reminders.EXTRA_EVENING) == true) {
            val ev = intent.getBooleanExtra(Reminders.EXTRA_EVENING, false)
            openSlotRequest = ev to System.currentTimeMillis()
            intent.removeExtra(Reminders.EXTRA_EVENING)
        }
    }
}

@Composable
private fun App(settings: ReaderSettings, openSlotRequest: Pair<Boolean, Long>?) {
    val ctx = LocalContext.current
    settings.systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()
    val theme = settings.theme
    var library by remember { mutableStateOf<Library?>(null) }
    LaunchedEffect(Unit) { library = withContext(Dispatchers.IO) { Library.load(ctx) } }

    // Match the system bar icons to the page colour.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !theme.dark
            isAppearanceLightNavigationBars = !theme.dark
        }
    }

    // Save-to-device for the bundled EPUBs (Storage Access Framework: no permissions needed).
    var pendingExport by remember { mutableStateOf<Edition?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri: Uri? ->
        val edition = pendingExport
        if (uri != null && edition != null) {
            val ok = runCatching {
                ctx.assets.open(edition.epub).use { input ->
                    ctx.contentResolver.openOutputStream(uri)!!.use { input.copyTo(it) }
                }
            }.isSuccess
            Toast.makeText(ctx, if (ok) "Saved ${edition.label} edition" else "Couldn't save the file", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(theme.bg)) {
        val lib = library
        if (lib == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = theme.accent)
        } else {
            ReaderScreen(
                settings = settings,
                library = lib,
                openSlotRequest = openSlotRequest,
                onExport = { e ->
                    pendingExport = e
                    exporter.launch(
                        if (e == Edition.Modern) "Morning and Evening - Modern English.epub"
                        else "Morning and Evening - Classic.epub",
                    )
                },
            )
        }
    }
}
