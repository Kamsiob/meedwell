package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.SupportButton
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.IconEdge
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.ThemeChoice

/**
 * Screen 34: Settings.
 *
 * The value block above the gold button says where things stand **before** any
 * invitation is made. That order is the rule: terms first, then the ask, and
 * the ask is one quiet button with no pressure attached.
 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    onThemeChange: (ThemeChoice) -> Unit,
    onToggleGapless: () -> Unit,
    onToggleLongResume: () -> Unit,
    onOpenLocalFolders: () -> Unit,
    onOpenExport: () -> Unit,
    onToggleShelfView: () -> Unit,
    onSyncNow: () -> Unit,
    onToggleWifiOnly: () -> Unit,
    onEraseHistory: () -> Unit,
    onDisconnect: () -> Unit,
    onSupport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        modifier
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

        Text("Settings", style = type.h1, color = colors.primaryText)

        Section("Look and feel")
        SettingRow(
            "Theme",
            null,
            trailing = state.theme.label(),
            onClick = { onThemeChange(state.theme.next()) },
        )
        SettingRow(
            "Shelf view",
            null,
            trailing = if (state.shelfGrid) "Grid" else "List",
            // Was a dead row showing the current value. It is the same setting
            // the icon at the top of the shelf changes, so it toggles here too
            // rather than being a read-only label wearing a tappable row.
            onClick = onToggleShelfView,
        )

        Section("Playback")
        ToggleRow(
            title = "Gapless playback",
            subtitle = null,
            checked = state.gapless,
            onToggle = onToggleGapless,
        )
        ToggleRow(
            title = "Remember position on long tracks",
            subtitle = "Pieces over 20 minutes",
            checked = state.rememberLongTrackPosition,
            onToggle = onToggleLongResume,
        )

        Section("Library")
        SettingRow(
            "Local music folders",
            state.foldersSubtitle,
            trailing = if (state.watchedFolderCount == 0) CHEVRON else "${state.watchedFolderCount} ›",
            onClick = onOpenLocalFolders,
        )

        Section("Surroundings")
        ToggleRow(
            title = "Download over Wi-Fi only",
            // The number is the point. "Use Wi-Fi only" as a bare switch does
            // not tell anybody what turning it off could cost them.
            subtitle = "The whole library is about 530 MB",
            checked = state.wifiOnlyDownloads,
            onToggle = onToggleWifiOnly,
        )

        Section("Your data")
        SettingRow("Export and restore", state.backupSubtitle, onClick = onOpenExport)
        SettingRow("Erase listening history", state.historySubtitle, onClick = onEraseHistory)

        if (state.connected) {
            Section("Bandcamp")
            // The one control this app most needed and did not have.
            //
            // Sync ran on opening the app and then not again for half an hour.
            // For an app built around Bandcamp Friday, "I just bought this and
            // it is not here" with no way to ask again is the product failing
            // at the exact moment it matters most.
            SettingRow(
                if (state.syncing) "Checking now" else "Check for new music",
                state.lastSyncLabel,
                trailing = if (state.syncing) "" else CHEVRON,
                onClick = { if (!state.syncing) onSyncNow() },
            )
            SettingRow(
                "Disconnect",
                "Removes the credentials from this phone. Your shelf, lists and history stay.",
                onClick = onDisconnect,
            )
        }

        // Terms before the invitation, always.
        // Padding before height, not after. The other order fixes the box at
        // 0.5dp, then consumes it with 24dp of padding, so neither the gap nor
        // the rule ever rendered and the value block butted into the row above.
        Box(Modifier.fillMaxWidth().padding(top = 24.dp).height(0.5.dp).background(colors.hairline))
        Text(
            "Free no matter what. Nothing held back, nothing unlocked later. One person carries it.",
            style = type.meta,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        SupportButton(
            label = "Support this work",
            onClick = onSupport,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 34.dp),
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MeedwellTheme.typography.section,
        color = MeedwellTheme.colors.secondaryText,
        modifier = Modifier.padding(top = 22.dp, bottom = 2.dp),
    )
}

/**
 * A switch row.
 *
 * The switch carries a toggleable state for TalkBack, and the whole row is the
 * target rather than the switch alone, which is what keeps a 40dp control
 * inside a 56dp reach.
 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Switch, onClick = onToggle)
                .padding(vertical = 12.dp)
                .semantics {
                    contentDescription = title
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = type.rowTitle, color = colors.primaryText)
                if (subtitle != null) {
                    Text(subtitle, style = type.meta, color = colors.tertiaryText, modifier = Modifier.padding(top = 3.dp))
                }
            }
            // The state is carried by a word as well as by the switch, so
            // color and position are never the only carrier of meaning.
            Text(
                if (checked) "On" else "Off",
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier.padding(end = 10.dp),
            )
            Box(
                Modifier
                    .size(width = 40.dp, height = 24.dp)
                    .clip(CircleShape)
                    .background(if (checked) colors.primaryText else colors.background),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (checked) colors.background else colors.secondaryText)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
    }
}

data class SettingsState(
    val theme: ThemeChoice = ThemeChoice.Daylight,
    val shelfGrid: Boolean = true,
    val gapless: Boolean = true,
    val rememberLongTrackPosition: Boolean = true,
    val watchedFolderCount: Int = 0,
    val historyEventCount: Int = 0,
    val connected: Boolean = false,
    val lastBackupAt: Long = 0,
    val syncing: Boolean = false,
    val wifiOnlyDownloads: Boolean = true,
    /** Seconds since the epoch, or zero for never. */
    val lastSyncAt: Long = 0,
) {
    /**
     * When the collection was last checked, in words.
     *
     * Rounded rather than exact, because "23 minutes ago" invites arithmetic
     * and the only question being asked is whether it is worth checking again.
     */
    val lastSyncLabel: String
        get() {
            if (syncing) return "Asking Bandcamp for your collection"
            if (lastSyncAt <= 0) return "Not checked yet"
            val ago = (System.currentTimeMillis() / 1000) - lastSyncAt
            return "Last checked " + when {
                ago < 90 -> "a moment ago"
                ago < 3600 -> "${ago / 60} minutes ago"
                ago < 7200 -> "an hour ago"
                ago < 86_400 -> "${ago / 3600} hours ago"
                ago < 172_800 -> "yesterday"
                else -> "${ago / 86_400} days ago"
            }
        }

    val foldersSubtitle: String
        get() = when (watchedFolderCount) {
            0 -> "None yet. This is how owned files reach your shelf."
            1 -> "One folder Meedwell watches"
            else -> "$watchedFolderCount folders Meedwell watches"
        }

    /**
     * When the last export was written.
     *
     * Quiet and permanent, and it never nags. A row that turns orange after a
     * fortnight to remind somebody to back up is the app deciding how anxious
     * they should be about their own data.
     */
    val backupSubtitle: String
        get() = if (lastBackupAt <= 0) {
            "Never exported"
        } else {
            val ago = (System.currentTimeMillis() / 1000) - lastBackupAt
            "Last exported " + when {
                ago < 86_400 -> "today"
                ago < 172_800 -> "yesterday"
                else -> "${ago / 86_400} days ago"
            }
        }

    val historySubtitle: String
        get() = if (historyEventCount == 0) {
            "Nothing recorded yet"
        } else {
            "$historyEventCount ${if (historyEventCount == 1) "play" else "plays"} recorded on this phone"
        }
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.Daylight -> "Daylight"
    ThemeChoice.Lamplight -> "Lamplight"
    ThemeChoice.System -> "System"
}

private fun ThemeChoice.next(): ThemeChoice = when (this) {
    ThemeChoice.Daylight -> ThemeChoice.Lamplight
    ThemeChoice.Lamplight -> ThemeChoice.System
    ThemeChoice.System -> ThemeChoice.Daylight
}
