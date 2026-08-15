package com.kamsiob.meedwell

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.meedwell.ui.screens.WelcomeScreen
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.ThemeChoice

/**
 * Single activity, as specified. Everything else is Compose.
 *
 * Phase 0 state: this launches the Welcome screen so the theme, the bundled
 * fonts and the mark are proven on a real device. The routing that replaces it
 * arrives in Phase 1 with the Connect flow and the shelf.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeedwellApp()
        }
    }
}

@Composable
private fun MeedwellApp() {
    val reducedMotion = rememberReducedMotion()

    MeedwellTheme(
        // Dark is the default, and stays the default until Settings can change it.
        themeChoice = ThemeChoice.Dark,
        reducedMotion = reducedMotion,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MeedwellTheme.colors.background)
        ) {
            WelcomeScreen(
                onConnect = { /* Phase 1: the Connect flow */ },
                onLocalOnly = { /* Phase 1: local files only */ },
                modifier = Modifier.systemBarsPadding(),
            )
        }
    }
}

/**
 * Reads the system's reduced motion preference.
 *
 * `DESIGN.md` section 10 requires this to be respected everywhere: no drift on
 * the ambient washes, no shimmer on waiting tiles, and a static waveform
 * envelope. Reading it once here and providing it through the theme means a new
 * animated surface cannot forget to ask.
 */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
