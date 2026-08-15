package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.IconEdge
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius

/**
 * Export and restore.
 *
 * **Restore is given the same weight as export**, which is the whole point.
 * Backup is easy to ship and easy to feel good about; the half nobody tests is
 * the half that matters on the day somebody's phone is in a river.
 *
 * The screen says three things before either button, because all three are
 * surprising if you find them out afterward:
 *
 *  - the file does not contain the music, and why that is right;
 *  - it does not contain the Bandcamp credentials, so it is safe to keep
 *    anywhere;
 *  - restoring **replaces** rather than merges, because merging two divergent
 *    listening histories is genuinely ambiguous and guessing would produce a
 *    history that is neither.
 */
@Composable
fun ExportScreen(
    state: ExportState,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Violet)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            IconButton(
                icon = MeedwellIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                size = 25.dp,
                tint = colors.primaryText,
                edge = IconEdge.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text("Export and restore", style = type.largeHeading, color = colors.primaryText)
            Text(
                state.lastBackupLine,
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            Section("WHAT GOES IN THE FILE")
            Body(
                "Everything you made: your listening history, your hearts, your lists, where you " +
                    "left off in long pieces, your settings, and where your music files were found."
            )

            Section("WHAT DOES NOT")
            Body(
                "The music. It is already yours and it already lives in a folder any app can read, " +
                    "so copying gigabytes of it into a backup of a play log would be a strange thing " +
                    "to do to your storage. The file records where each track was found instead, so " +
                    "a restore can re-link your music rather than fetch it again."
            )
            Body(
                "Your Bandcamp credentials. They never leave the encrypted storage they live in, so " +
                    "this file is safe to keep anywhere: another phone, a memory card, an email to " +
                    "yourself. A restored Meedwell asks you to connect again, which takes a minute.",
                topPadding = 12.dp,
            )

            PillButton(
                label = if (state.working) "Working…" else "Export to a file",
                onClick = { if (!state.working) onExport() },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )

            Section("RESTORING")
            Body(
                "Restoring replaces what is on this phone with what is in the file. It does not " +
                    "merge the two, because two listening histories that have drifted apart cannot " +
                    "be joined into one true answer, and Meedwell would rather say that than guess."
            )
            Body(
                "It happens in one go. If anything fails partway, nothing changes at all.",
                topPadding = 12.dp,
            )

            TextButtonRow(
                label = "Restore from a file",
                onClick = { if (!state.working) onRestore() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            state.result?.let { ResultPanel(it) }

            Box(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MeedwellTheme.typography.capsEyebrow,
        color = MeedwellTheme.colors.secondaryText,
        modifier = Modifier.padding(top = 26.dp, bottom = 6.dp),
    )
}

@Composable
private fun Body(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        style = MeedwellTheme.typography.body,
        color = MeedwellTheme.colors.secondaryText,
        modifier = Modifier.padding(top = topPadding),
    )
}

/**
 * What just happened, kept on screen rather than flashed.
 *
 * A restore is the one action in this app somebody may want to read the
 * outcome of twice, so it does not vanish after four seconds the way an
 * ordinary notice does.
 */
@Composable
private fun ResultPanel(result: String) {
    val colors = MeedwellTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .clip(RoundedCornerShape(Radius.panel))
            .background(colors.surfacePanel)
            .padding(16.dp),
    ) {
        Text(result, style = MeedwellTheme.typography.body, color = colors.primaryText)
    }
}

data class ExportState(
    val lastBackupAt: Long = 0,
    val working: Boolean = false,
    val result: String? = null,
) {
    val lastBackupLine: String
        get() = if (lastBackupAt <= 0) {
            "Not exported yet"
        } else {
            val ago = (System.currentTimeMillis() / 1000) - lastBackupAt
            "Last exported " + when {
                ago < 3600 -> "less than an hour ago"
                ago < 86_400 -> "${ago / 3600} hours ago"
                ago < 172_800 -> "yesterday"
                else -> "${ago / 86_400} days ago"
            }
        }
}
