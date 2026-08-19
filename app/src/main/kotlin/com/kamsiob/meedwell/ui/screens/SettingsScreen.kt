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
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.SupportButton
import com.kamsiob.meedwell.ui.components.DetailHeader
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
    onEditDawn: () -> Unit,
    onEditDusk: () -> Unit,
    onSyncNow: () -> Unit,
    onToggleWifiOnly: () -> Unit,
    onToggleResumeQueue: () -> Unit,
    onEraseHistory: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    onSendFeedback: () -> Unit,
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
        DetailHeader("Settings", onBack)
        Section("Look and feel", first = true)
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
        SettingRow(
            "Dawn",
            "Where the day line starts",
            trailing = state.dawnLabel,
            onClick = onEditDawn,
        )
        SettingRow(
            "Dusk",
            "Where it ends. No location is ever used to work these out",
            trailing = state.duskLabel,
            onClick = onEditDusk,
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
        ToggleRow(
            title = "Resume the queue on opening",
            // Says what it does not do, because that is the worry.
            subtitle = "Puts it back paused. Nothing ever starts on its own.",
            checked = state.resumeQueueOnOpening,
            onToggle = onToggleResumeQueue,
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
        // Not a control, just a fact worth stating.
        //
        // The grid says "Music/Meedwell, fixed so any player can read them",
        // which describes an app this one is not: `SurroundingsStore` puts
        // recordings in private storage on purpose, so they stay out of every
        // other app's media scanner and leave with the app. Copying the grid's
        // line here would have been a plainly false sentence on the screen that
        // exists to be checked.
        SettingRow(
            "Where recordings go",
            "Private to Meedwell, so they stay out of your music library",
            trailing = "",
            onClick = null,
        )

        Section("Permissions")
        SettingRow(
            "Notifications",
            if (state.notificationsAllowed) {
                "Allowed. Used only for playback controls"
            } else {
                "Not allowed. Playback controls will not appear"
            },
            onClick = onOpenAppSettings,
        )
        // Deliberately the only row here. Local folders are read through the
        // system's own document picker, which grants access to the one folder
        // that was picked and needs no permission at all. Meedwell asked for
        // READ_MEDIA_AUDIO and then never used it, so the declaration is gone.
        Text(
            "Your folders need no permission. Meedwell only ever sees the ones you hand it, " +
                "through Android's own picker.\n\nNo location, no contacts, no microphone. Not ever.",
            style = type.meta,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 9.dp),
        )

        Section("Your data")
        SettingRow("Export and restore", state.backupSubtitle, onClick = onOpenExport)
        SettingRow(
            "Erase listening history",
            state.historySubtitle,
            onClick = onEraseHistory,
            destructive = true,
        )

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

        Section("Help and honesty")
        SettingRow("Credits and licenses", null, onClick = onOpenLicenses)
        SettingRow(
            "Send feedback",
            // Says exactly what rides along, because "send feedback" in most
            // apps quietly attaches a log. This one attaches the version and
            // nothing else, and the mail sits in the draft where it can be read
            // before it goes.
            "Opens your mail app. Carries version only",
            onClick = onSendFeedback,
        )
        SettingRow(
            "Version",
            null,
            trailing = state.versionLabel,
            onClick = null,
        )

        // Terms before the invitation, always.
        // Padding before height, not after. The other order fixes the box at
        // 0.5dp, then consumes it with 24dp of padding, so neither the gap nor
        // the rule ever rendered and the value block butted into the row above.
        Hairline(Modifier.padding(top = 24.dp))
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

/**
 * A section heading, which on the grid is a label **and a staff** running out to
 * the margin, exactly as on More.
 *
 * It was a bare uppercase label here, so Settings was the one screen in the app
 * where a section did not look like a section.
 */
@Composable
private fun Section(title: String, first: Boolean = false) {
    // A larger gap before a staff than after it is what makes the staff read
    // as a beginning rather than as a tick every eighteen dp.
    SectionHead(title, Modifier.padding(top = if (first) 18.dp else 26.dp, bottom = 1.dp))
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
                .padding(vertical = ROW_BREATH)
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
            // 38 by 22 with a 17 knob, straight off the grid's `.tog`.
            //
            // On is **moss**, the one working accent, rather than ink. Off is
            // the heavier hairline: it was the page ground, which made an off
            // switch invisible against the paper it sat on.
            Box(
                Modifier
                    .size(width = 38.dp, height = 22.dp)
                    .clip(CircleShape)
                    .background(if (checked) colors.moss else colors.hairline2),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .padding(horizontal = 2.5.dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(colors.switchKnob)
                )
            }
        }
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
    val resumeQueueOnOpening: Boolean = true,
    /**
     * The listener's own dawn and dusk, in minutes since midnight.
     *
     * Settings rather than measurements: real solar times need a latitude and
     * this app asks for no location. The clock comes from the phone, so a change
     * of timezone already moves the sun correctly on its own.
     */
    val dawnMinute: Int = 6 * 60,
    val duskMinute: Int = 21 * 60,
    /**
     * Live permission state, read at the moment the screen is shown.
     *
     * Held rather than assumed, because it can be revoked in the system settings
     * while the app is in the background. A Permissions section that says
     * "Allowed" for something since turned off is worse than having no section
     * at all.
     *
     * There is only one, because notifications are the only permission Meedwell
     * declares that a person can withhold.
     */
    val notificationsAllowed: Boolean = false,
    val versionName: String = "",
    val versionCode: Int = 0,
    /** Seconds since the epoch, or zero for never. */
    val lastSyncAt: Long = 0,
) {
    /** "1.0.0 · 118", the grid's `num` styling on the right of the row. */
    val versionLabel: String get() = "$versionName · $versionCode"

    /**
     * The two hours, on whichever clock the phone is set to.
     *
     * Formatted here rather than in the row so Settings and the day line panel
     * can never disagree about what 21:00 is called.
     */
    val dawnLabel: String get() = plainClock(dawnMinute)
    val duskLabel: String get() = plainClock(duskMinute)

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

/**
 * A 24 hour label, used only as the fallback shape for the settings rows.
 *
 * The sheet and the day line panel both format through `DateFormat`, which
 * follows the phone. This one cannot, because a data class has no context, so it
 * states the hour plainly and the sheet shows the real thing on tap.
 */
private fun plainClock(minuteOfDay: Int): String =
    "%d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

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
