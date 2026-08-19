package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.MeedwellMark
import com.kamsiob.meedwell.ui.components.SupportButton
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.StaffRule
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The transparency screens and Settings.
 *
 * Every claim on these pages is checked against the built software, because a
 * privacy page that overstates is worse than none. Where verification changed
 * what the app can do, the copy changed with it in the same pass rather than
 * being left to be discovered.
 */

/**
 * The More tab, grid screen 18.
 *
 * **Three groups, three staves, and every row carries its current value on the
 * right** rather than only a chevron. That is the whole idea of the screen: the
 * state of the app is readable without opening anything.
 *
 * The transparency screens sit beside Settings as peers rather than buried
 * under it, because they are the argument this app makes rather than a
 * sub-preference.
 */
@Composable
fun MoreScreen(
    state: MoreState,
    onOpen: (MoreDestination) -> Unit,
    onConnectBandcamp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Text("More", style = type.h1, color = colors.primaryText, modifier = Modifier.padding(top = 11.dp))

            SectionHead("Listening", Modifier.padding(top = 18.dp, bottom = 1.dp))
            SettingRow("Tone", null, trailing = "${state.toneName} ›") { onOpen(MoreDestination.Tone) }
            SettingRow(
                "Sleep timer",
                null,
                trailing = "${state.sleepTimer} ›",
                trailingIcon = MeedwellIcons.Fermata,
            ) { onOpen(MoreDestination.SleepTimer) }
            SettingRow("History", null, trailing = "${state.playsLabel} ›") { onOpen(MoreDestination.History) }
            SettingRow("Forgotten shelf", null, trailing = "${state.forgottenLabel} ›") {
                onOpen(MoreDestination.Forgotten)
            }
            // The screen was fully built, back-targeted, rendered, and had no
            // door anywhere in the app. The heart asked for the most personal
            // act the app offers and then had nowhere to keep it.
            SettingRow("Loved", null) { onOpen(MoreDestination.Loved) }

            SectionHead("Your shelf", Modifier.padding(top = 18.dp, bottom = 1.dp))
            SettingRow(
                "Bandcamp connection",
                null,
                trailing = "${state.syncLabel} ›",
            ) { if (state.connected) onOpen(MoreDestination.Settings) else onConnectBandcamp() }
            SettingRow("Folders on this phone", null, trailing = "${state.folderCount} ›") {
                onOpen(MoreDestination.YourFiles)
            }

            SectionHead("Meedwell", Modifier.padding(top = 18.dp, bottom = 1.dp))
            SettingRow("Settings", null) { onOpen(MoreDestination.Settings) }
            SettingRow("Privacy", null) { onOpen(MoreDestination.Privacy) }
            SettingRow("Being considered", null) { onOpen(MoreDestination.WhatsAhead) }
            SettingRow("Not planned", null) { onOpen(MoreDestination.NotPlanned) }
            SettingRow("Credits and licenses", null) { onOpen(MoreDestination.Credits) }
            SettingRow("About Meedwell", null) { onOpen(MoreDestination.About) }

            Box(Modifier.height(40.dp))
        }
    }
}

/**
 * What More shows on the right of each row.
 *
 * Every value here is real. A row that cannot say anything true shows a plain
 * chevron rather than a placeholder, because the point of the right-hand column
 * is that it can be trusted at a glance.
 */
data class MoreState(
    val connected: Boolean = false,
    val toneName: String = "As Recorded",
    val sleepTimer: String = "Off",
    val playCount: Int = 0,
    val forgottenCount: Int = 0,
    val folderCount: Int = 0,
    val lastSyncAt: Long = 0,
) {
    val playsLabel: String
        get() = when (playCount) {
            0 -> "nothing yet"
            1 -> "1 play"
            else -> "$playCount plays"
        }

    val forgottenLabel: String
        get() = when (forgottenCount) {
            0 -> "none waiting"
            1 -> "1 waiting"
            else -> "$forgottenCount waiting"
        }

    val syncLabel: String
        get() = when {
            !connected -> "not connected"
            lastSyncAt <= 0 -> "never synced"
            else -> {
                val ago = (System.currentTimeMillis() / 1000) - lastSyncAt
                when {
                    ago < 3600 -> "synced just now"
                    ago < 86_400 -> "synced ${ago / 3600} hr ago"
                    else -> "synced ${ago / 86_400} days ago"
                }
            }
        }
}

enum class MoreDestination {
    Tone, SleepTimer, History, Forgotten, Loved, YourFiles,
    Settings, Privacy, WhatsAhead, NotPlanned, Credits, About,
}

/**
 * Screen 33: Privacy, as five plain questions and answers.
 *
 * Every answer here is a statement about the built software that can be checked
 * by reading the source, and each one is true of the code as it stands.
 */
@Composable
fun PrivacyScreen(onOpenSource: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenScaffold(title = "Privacy", voice = "The whole sheet. This is all of it.", onBack = onBack, modifier = modifier) {
        QuestionAnswer(
            "What Meedwell collects",
            "Nothing. No analytics, no telemetry, no identifiers, nothing that leaves this phone.",
        )
        QuestionAnswer(
            "Your listening history",
            "One database file, here, powering the forgotten shelf and the waveforms. It is plain SQLite " +
                "rather than an encrypted store, on purpose, so you can take it with you. Erase it any " +
                "time in Settings.",
        )
        QuestionAnswer(
            "Your Bandcamp credentials",
            "In Android's encrypted storage, on this phone, and nowhere else. Never in the database, " +
                "never in an export, never in a log. Meedwell also opts out of autofill entirely, so " +
                "nothing offers to copy them to a cloud password manager.",
        )
        QuestionAnswer(
            "Network traffic",
            // Two servers now, not one. Surroundings fetches its recordings
            // from GitHub, and a privacy page that still said "exactly one"
            // would be false the first time somebody downloaded a pack. Naming
            // both, and what each one is for, is the whole point of the page.
            "Two servers, both only when you ask. Bandcamp's, to stream and sync what you own. " +
                "GitHub's, to fetch Surroundings recordings, which is where the ambience library is " +
                "published. Neither is told anything about you beyond what the request needs. That " +
                "traffic follows their own privacy policies. Meedwell does not have one, because " +
                "there is nothing to govern.",
        )
        // The shelf draws a sun and a moon at the real time of day, which is
        // exactly the kind of thing that usually means an app has asked where
        // you are. This one has not and cannot, and the place to say so is the
        // page somebody opens to check.
        QuestionAnswer(
            "Where you are",
            "Meedwell never asks for your location and has no way to find it. The day line, and the " +
                "sun and moon on it, run on your phone's own clock plus two times you set yourself " +
                "in Settings. Real sunrise and sunset would need a latitude, and that is not a fair " +
                "trade for a line on a screen.",
        )
        QuestionAnswer(
            "Notifications",
            "Downloading in the background shows a notification because Android requires one, and " +
                "because work you cannot see or stop is worse than work that shows itself. It names " +
                "the recording and its progress, and it is drawn on this phone. Turning notifications " +
                "down costs you that and the playback controls, and nothing else.",
        )
        QuestionAnswer(
            "Sharing and outside links",
            "Sharing hands a plain Bandcamp link to Android's own share sheet. Links open in your " +
                "browser. Meedwell fetches nothing along the way.",
        )
        QuestionAnswer(
            "Don't take our word for it",
            "Every line of source code is public.",
        )
        LinkRow("Read the source on GitHub ↗", onOpenSource)
    }
}

/**
 * Screen 39: What's ahead.
 *
 * Rewritten after verification. Several entries were written when the API's
 * shape was unknown, and the honest versions are different: downloading from
 * Bandcamp is not "coming later", it is not offered by their API at all.
 */
/**
 * Grid screen 22: Not planned.
 *
 * **Decisions, not gaps.** Every entry names its actual reason, so a refusal
 * reads as a position rather than as a shortfall. This screen is the reason
 * "Being considered" can be short: the things that are never coming are said here
 * once, plainly, instead of being quietly absent forever.
 *
 * The mixing entry gives the technical argument in plain words, which is the
 * only honest way to decline something every competitor offers.
 */
@Composable
fun NotPlannedScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    ScreenScaffold(
        title = "Not planned",
        voice = "Decisions, not gaps.",
        onBack = onBack,
        modifier = modifier,
    ) {
        SectionHead("In the app", Modifier.padding(top = 18.dp, bottom = 1.dp))

        QuestionAnswer(
            "Accounts, or a Meedwell login",
            "There is no server to log in to, and there will not be one.",
        )
        QuestionAnswer(
            "Cloud sync",
            "It would mean hosting your listening. Export and restore does the same job without anyone " +
                "else holding your history.",
        )
        QuestionAnswer(
            "Badges, streaks and achievements",
            "Listening is not a task to complete.",
        )
        QuestionAnswer(
            "Recommendations from strangers",
            "Your shelf is the whole catalog here.",
        )

        Text(
            "IN SURROUNDINGS",
            style = type.section,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 24.dp),
        )

        QuestionAnswer(
            "Mixing several sounds at once",
            "These are real recordings of whole places. Layering two of them stacks two noise floors and " +
                "two rooms, and sounds worse than either one alone.",
        )
        QuestionAnswer(
            "Generated or synthetic sound",
            "Tried, and it could not reach the realism of a real recording.",
        )
        QuestionAnswer(
            "Streaming instead of downloading",
            "Everything works offline, which needs no service to keep running.",
        )
    }
}

@Composable
fun WhatsAheadScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    ScreenScaffold(title = "Being considered", voice = null, onBack = onBack, modifier = modifier) {
        SectionHead("Being considered", Modifier.padding(top = 18.dp, bottom = 1.dp))

        QuestionAnswer(
            "Searching all of Bandcamp, inside Meedwell",
            "Their API only shares your own collection today. The moment they open up store search, it " +
                "lands here. Until then, one tap hands your search to Bandcamp's site.",
        )
        QuestionAnswer(
            "Getting files through the app",
            "Bandcamp's API streams your collection but will not release the files. So Meedwell does not " +
                "pretend to: you download them from Bandcamp as you always have, and Meedwell recognizes " +
                "them. If they ever open that up, this becomes one button.",
        )
        QuestionAnswer(
            "Taking a heart back off",
            "Adding one works and reaches your account. Removing one is broken on Bandcamp's side right " +
                "now, and returns an error whatever we send. When they fix it, the control here starts " +
                "working with no update needed.",
        )
        QuestionAnswer(
            "Lists that travel with your account",
            "Bandcamp's API can show a playlist and offers no way to make or change one, so lists live " +
                "on this phone and say so.",
        )
        QuestionAnswer("Equalizer", "Real work to do well. It won't ship half-done.")
        QuestionAnswer("Android Auto, and other Subsonic servers", "The plumbing is already built for both.")
        QuestionAnswer(
            "Tablet and large screen layouts",
            "Version one is built for a phone held in one hand. Everything survives rotation, and nothing " +
                "is laid out specially for a bigger screen yet.",
        )
        QuestionAnswer(
            "Languages beyond English",
            "Version one ships in English only. Every word in the app is already separated out ready to " +
                "translate, so this is an addition rather than a rewrite.",
        )
        QuestionAnswer(
            "F-Droid",
            "F-Droid needs reproducible builds, which this project deliberately does not pursue. That is " +
                "the real reason, rather than a queue this is waiting in.",
        )

        SectionHead("Not planned", Modifier.padding(top = 18.dp, bottom = 1.dp))
        QuestionAnswer("Accounts, ads, telemetry, subscriptions", "Not gaps. Decisions.")
        QuestionAnswer(
            "A store inside the app",
            "Buying happens on Bandcamp, where artists get paid and nothing stands between you and them.",
        )
        QuestionAnswer(
            "Streaks, badges, and notifications that pull you back",
            "The app has one notification and it is the player. There will not be a second.",
        )

        Text(
            "One person builds this. Everything gets read.",
            style = type.voice,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 30.dp),
        )
    }
}

/**
 * Grid screen 21: About, wearing the flat mark.
 *
 * The grid calls this "the screen most likely to be photographed, so held to
 * the highest finish", and it was the one furthest from the drawing. The mark
 * and the wordmark are **stacked**, not side by side; the serif line is the
 * emotional centre and comes before any prose; the prose is two short
 * paragraphs rather than one dense block; and the outward links and the
 * inward destination are separate groups divided by a rule.
 *
 * The Feedback row was wired to an empty lambda, so the one row on this screen
 * that invites somebody to get in touch did nothing at all when tapped.
 */
@Composable
fun AboutScreen(
    versionName: String,
    onOpenSource: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenSite: () -> Unit,
    onFeedback: () -> Unit,
    onOpenLicenses: () -> Unit,
    onSupport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    ScreenScaffold(title = null, voice = null, onBack = onBack, modifier = modifier) {
        // Bare here too. The framed form belongs on a launcher, nowhere else.
        Box(Modifier.padding(top = 22.dp)) {
            MeedwellMark(size = 44.dp, bare = true, contentDescription = null)
        }
        Text(
            "Meedwell",
            // The wordmark is a label above a plate line, not the headline.
            style = type.h2,
            color = colors.primaryText,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            // Tabular figures, and the license under its proper name.
            "Version $versionName · AGPL-3.0 · by Kamsiob",
            style = type.plate,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            "Music you own, on a shelf, in a room.",
            // The emotional center of the screen most likely to be
            // photographed. It was set smaller than the wordmark above it,
            // which handed the climax to a product name.
            style = type.serifOpening.copy(fontSize = 30.sp, lineHeight = 37.sp),
            color = colors.primaryText,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            "Meedwell reads a Bandcamp collection and the files already on your phone, and puts them " +
                "on one shelf. It keeps no account of you, holds no server, and asks for nothing " +
                "beyond the permissions it visibly needs.",
            style = type.body,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 13.dp),
        )
        Text(
            "It was built around instrumental listening, and it plays whatever you own. Both of those " +
                "are true at once, and neither is a marketing position.",
            style = type.body,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 11.dp),
        )

        StaffRule(Modifier.padding(top = 22.dp, bottom = 6.dp))

        // Outward. Every one of these leaves the app, and the arrow says so.
        SettingRow("Source code", null, trailing = "GitHub ↗", onClick = onOpenSource)
        SettingRow("Videos", null, trailing = "YouTube ↗", onClick = onOpenVideos)
        SettingRow("Everything else", null, trailing = "kamsiob.com ↗", onClick = onOpenSite)
        SettingRow("Feedback", null, trailing = "hello@kamsiob.com ↗", onClick = onFeedback)

        // Inward, and kept in its own group by a rule, because a chevron and an
        // arrow mean different things and should not sit in one list.
        Hairline(Modifier.padding(top = 2.dp))
        SettingRow("Credits and licenses", null, onClick = onOpenLicenses)

        Text(
            "Not affiliated with, endorsed by, or connected to Bandcamp.",
            style = type.meta,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = 8.dp),
        )

        // The value block states the terms before any invitation is made.
        Text(
            "Built and carried by one person. If software made this way matters to you, there is a " +
                "place to stand behind it. Either way, it is yours.",
            style = type.meta,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        )
        SupportButton(
            label = "Support this work",
            onClick = onSupport,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 30.dp),
        )
    }
}

// ---------- Shared pieces ----------

@Composable
private fun ScreenScaffold(
    title: String?,
    voice: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        // About passes no title, because it wears the wordmark instead and a
        // heading saying "About" above a mark saying "Meedwell" would be the
        // same thing twice. The chevron still needs to be there, so the header
        // renders with an empty title beside it.
        DetailHeader(title.orEmpty(), onBack)
        if (voice != null) {
            Text(voice, style = type.voice, color = colors.secondaryText, modifier = Modifier.padding(top = 11.dp))
        }
        content()
        Box(Modifier.height(30.dp))
    }
}

@Composable
private fun QuestionAnswer(question: String, answer: String) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(question, style = type.rowTitle, color = colors.primaryText)
        Text(answer, style = type.meta, color = colors.secondaryText, modifier = Modifier.padding(top = 4.dp))
        // Padding before height. The other way round, the padding eats the
        // strip and the rule under every question renders as nothing.
        Hairline(Modifier.padding(top = 14.dp))
    }
}

/** The chevron sentinel, so a row can ask for the icon rather than a glyph. */
const val CHEVRON = "\u203A"

@Composable
fun SettingRow(
    title: String,
    subtitle: String?,
    trailing: String? = CHEVRON,
    /**
     * A mark drawn before the trailing value.
     *
     * The fermata is the sleep timer's sign, and `U+1D110` is not in Instrument
     * Sans: it rendered as a bare arc with its dot missing. A mark this app
     * relies on cannot depend on a glyph the typeface may not have, so it is
     * drawn.
     */
    trailingIcon: MeedwellIcons? = null,
    /**
     * Whether this row ends something.
     *
     * Sets the title in alarm ink, which `DESIGN.md` section 2 reserves for
     * exactly this and nothing else. A grep found `colors.alarm` used once in
     * the whole interface, while the row that erases your listening history sat
     * in the same ink as the one above it that exports it.
     */
    destructive: Boolean = false,
    /**
     * Null for a row that only states something.
     *
     * Without this every row was tappable, so a purely informational line still
     * lit up under a finger and still announced itself to a screen reader as a
     * button. A row that does nothing should not look or sound like one that
     * does.
     */
    onClick: (() -> Unit)? = null,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .then(
                    if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                    else Modifier
                )
                .padding(vertical = ROW_BREATH),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                // Alarm ink, and only here. `DESIGN.md` section 2 reserves it
                // for destructive rows, and erasing your listening history was
                // drawn exactly like "Export and restore" one row above it.
                Text(
                    title,
                    style = type.rowTitle,
                    color = if (destructive) colors.alarm else colors.primaryText,
                )
                if (subtitle != null) {
                    Text(subtitle, style = type.meta, color = colors.tertiaryText, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (trailing == CHEVRON) {
                MeedwellIcon(
                    icon = MeedwellIcons.ChevronRight,
                    size = 14.dp,
                    tint = colors.tertiaryText,
                    modifier = Modifier.padding(start = 14.dp),
                )
            } else if (trailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp)) {
                    trailingIcon?.let {
                        MeedwellIcon(
                            icon = it,
                            size = 13.dp,
                            tint = colors.tertiaryText,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Text(trailing.removeSuffix(" ›"), style = type.meta, color = colors.tertiaryText)
                    if (trailing.endsWith(" ›")) {
                        MeedwellIcon(
                            icon = MeedwellIcons.ChevronRight,
                            size = 14.dp,
                            tint = colors.tertiaryText,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
        // **The rule under the row.**
        //
        // Settings, More and About were the only lists in the app without one,
        // on the argument that 13dp of padding was proximity enough. It was not:
        // twenty rows became one gray field with nothing marking where one
        // ended, while the five voicing rows one tap away were ruled. The grid
        // draws `border-bottom` on `.row` without exception.
        Hairline()
    }
}

/**
 * The space that separates one row from the next, in place of a rule.
 *
 * **Settings had a hairline under every row**, which on a page of twenty rows is
 * twenty lines competing with the section staves that are supposed to be the
 * structure. The staves stayed and the rules went.
 *
 * What separates rows now is proximity. A title and its subtitle sit 3dp apart;
 * consecutive rows sit `ROW_BREATH * 2` apart. At roughly eight to one the eye
 * groups them without being told to, which is how a well-set page has always
 * done it and why a printed table of contents needs no leader rules.
 *
 * It also buys back the thing rules were costing: on a screen this dense the
 * lines read as a grid to be scanned, and the space reads as a page to be read.
 */
val ROW_BREATH = 13.dp

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(label, style = MeedwellTheme.typography.meta, color = MeedwellTheme.colors.primaryText)
    }
}
