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
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.MeedwellMark
import com.kamsiob.meedwell.ui.components.SupportButton
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.IconEdge
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The transparency screens and Settings.
 *
 * Every claim on these pages is checked against the built software, because a
 * privacy page that overstates is worse than none. Where verification changed
 * what the app can do, the copy changed with it in the same pass rather than
 * being left to be discovered.
 */

/** The More tab: a plain index into everything that is not the shelf. */
@Composable
fun MoreScreen(
    connected: Boolean,
    onOpen: (MoreDestination) -> Unit,
    onConnectBandcamp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Ember)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Text("More", style = type.largeHeading, color = colors.primaryText, modifier = Modifier.padding(top = 18.dp))

            SettingRow("History", "Everything you have played, on this phone") { onOpen(MoreDestination.History) }
            SettingRow("Forgotten shelf", "Bought, loved, and quietly waiting") { onOpen(MoreDestination.Forgotten) }
            SettingRow("Surroundings", "Rain, fire and rooms, under the music") { onOpen(MoreDestination.Surroundings) }
            SettingRow("Your files", "Where owned music lives") { onOpen(MoreDestination.YourFiles) }
            SettingRow("Settings", null) { onOpen(MoreDestination.Settings) }
            SettingRow("Privacy", "The whole sheet") { onOpen(MoreDestination.Privacy) }
            SettingRow("What's ahead", null) { onOpen(MoreDestination.WhatsAhead) }
            SettingRow("About", null) { onOpen(MoreDestination.About) }
            SettingRow("Credits", "Everyone whose recording is in here") { onOpen(MoreDestination.Credits) }

            // Connect Bandcamp stays permanently reachable from More, which is
            // what keeps local-only a door rather than a dead end.
            if (!connected) {
                SettingRow(
                    title = "Connect Bandcamp",
                    subtitle = "Your purchases join this shelf without duplicating what is already here",
                    onClick = onConnectBandcamp,
                )
            }
            Box(Modifier.height(40.dp))
        }
    }
}

enum class MoreDestination { History, Forgotten, Surroundings, YourFiles, Settings, Privacy, WhatsAhead, About, Credits }

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
@Composable
fun WhatsAheadScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    ScreenScaffold(title = "What's ahead", voice = null, onBack = onBack, modifier = modifier) {
        Text("BEING CONSIDERED", style = type.capsEyebrow, color = colors.secondaryText, modifier = Modifier.padding(top = 8.dp))

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

        Text("NOT PLANNED", style = type.capsEyebrow, color = colors.secondaryText, modifier = Modifier.padding(top = 22.dp))
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
            style = type.voiceSmall,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 30.dp),
        )
    }
}

/** Screen 38: About, wearing the flat mark. */
@Composable
fun AboutScreen(
    versionName: String,
    onOpenSource: () -> Unit,
    onOpenSite: () -> Unit,
    onSupport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    ScreenScaffold(title = null, voice = null, onBack = onBack, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            MeedwellMark(size = 52.dp, contentDescription = null)
            Text(
                "Meedwell",
                style = type.largeHeading,
                color = colors.primaryText,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        Text(
            "v$versionName · AGPLv3 · by Kamsiob".uppercase(),
            style = type.capsEyebrow,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "Subscriptions squeeze the people who listen and starve the people who make. Bandcamp proves " +
                "it can work another way. Meedwell exists to make owning music feel better than renting " +
                "it ever did.",
            style = type.body,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 14.dp),
        )

        SettingRow("Source code", null, trailing = "GitHub ↗", onClick = onOpenSource)
        SettingRow("Everything else", null, trailing = "kamsiob.com ↗", onClick = onOpenSite)
        SettingRow("Feedback", null, trailing = "hello@kamsiob.com", onClick = {})

        Text(
            "Not affiliated with or endorsed by Bandcamp".uppercase(),
            style = type.capsEyebrow,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 14.dp),
        )

        // The value block states the terms before any invitation is made.
        Text(
            "Free no matter what. Nothing held back, nothing unlocked later. Built and carried by one " +
                "person. If software made this way matters to you, there's a place to stand behind it. " +
                "Either way, it's yours.",
            style = type.metadata,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        )
        SupportButton(
            label = "Support this work",
            onClick = onSupport,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 30.dp),
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
        IconButton(
            icon = MeedwellIcons.Back,
            contentDescription = "Back",
            onClick = onBack,
            size = 25.dp,
            tint = colors.primaryText,
            edge = IconEdge.Start,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (title != null) {
            Text(title, style = type.largeHeading, color = colors.primaryText)
        }
        if (voice != null) {
            Text(voice, style = type.voiceSmall, color = colors.secondaryText, modifier = Modifier.padding(top = 8.dp))
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
        Text(answer, style = type.metadata, color = colors.secondaryText, modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).padding(top = 14.dp).background(colors.hairline))
    }
}

/** The chevron sentinel, so a row can ask for the icon rather than a glyph. */
const val CHEVRON = "\u203A"

@Composable
fun SettingRow(
    title: String,
    subtitle: String?,
    trailing: String? = CHEVRON,
    onClick: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = type.rowTitle, color = colors.primaryText)
                if (subtitle != null) {
                    Text(subtitle, style = type.metadata, color = colors.tertiaryText, modifier = Modifier.padding(top = 3.dp))
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
                    Text(trailing.removeSuffix(" ›"), style = type.metadata, color = colors.tertiaryText)
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
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(label, style = MeedwellTheme.typography.provenance, color = MeedwellTheme.colors.primaryText)
    }
}
