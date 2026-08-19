package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.data.SyncFailure
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * Screen 31 in the visual reference: Connection trouble.
 *
 * The reference shows `error 40 · wrong username or password`. **No such code
 * is ever returned.** Verification on 15 August 2026 found that Bandcamp
 * answers a rejected login with HTTP 500 and an empty body, which is almost
 * certainly the unexplained 401 in the field reports: a bare server error that
 * every client renders differently.
 *
 * So the screen keeps its shape, its reassurance and its three ways forward, and
 * the "what happened" block says what actually happened.
 *
 * It also reassures before it explains, in that order, because the first thing
 * somebody wants to know when a music app says something went wrong is whether
 * their music is still there.
 */
@Composable
fun SyncTroubleSheet(
    failure: SyncFailure,
    lastSyncAt: Long,
    onRetry: () -> Unit,
    onFreshCredentials: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Dismiss" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .sheetShadow()
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 22.dp),
        ) {
            Text("Bandcamp didn't answer", style = type.h2, color = colors.primaryText)

            // Reassurance first. Somebody whose music app says something went
            // wrong wants to know their music is fine before they want a cause.
            Text(
                "Your music on this phone is untouched and playing fine.",
                style = type.voice,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                failure.explanation(),
                style = type.meta,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )

            Text(
                "WHAT HAPPENED",
                style = type.section,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 18.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.recess)
                    .padding(12.dp),
            ) {
                Text(failure.detail(), style = type.numeric, color = colors.tertiaryText)
                Text(
                    "last successful sync · " + lastSyncLabel(lastSyncAt),
                    style = type.numeric,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            PillButton(
                label = "Try again",
                onClick = { onRetry(); onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TroubleLink("Get fresh credentials ↗", Modifier.weight(1f)) {
                    onFreshCredentials(); onDismiss()
                }
                TroubleLink("Stay offline for now", Modifier.weight(1f), onDismiss)
            }
        }
    }
}

@Composable
private fun TroubleLink(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MeedwellTheme.typography.meta, color = MeedwellTheme.colors.secondaryText)
    }
}

/**
 * What went wrong, in the app's own words rather than in the server's.
 *
 * Each case names a real cause and a real way forward. None of them says
 * "something went wrong", which tells nobody anything.
 */
private fun SyncFailure.explanation(): String = when (this) {
    SyncFailure.CredentialsRejected ->
        "Bandcamp turned down the username and password. That usually means the password was " +
            "regenerated on their side, which quietly invalidates the old one. Nothing on your " +
            "shelf is lost."
    is SyncFailure.Unreachable ->
        "The request did not get an answer at all. That is usually the connection on this phone " +
            "rather than anything on Bandcamp's side."
    is SyncFailure.ServerSaid ->
        "Bandcamp answered, but with a problem rather than your collection. Their Subsonic support " +
            "is a young beta and this sometimes passes on its own."
    is SyncFailure.Unreadable ->
        "Bandcamp answered with something Meedwell could not read. Their Subsonic support is a " +
            "young beta, so this may be them rather than you."
}

/** The technical line, kept short and honest. */
private fun SyncFailure.detail(): String = when (this) {
    // Deliberately not "error 40". There is no code; there is an empty 500.
    SyncFailure.CredentialsRejected -> "server error 500 · no reason given"
    is SyncFailure.Unreachable -> "no answer · $reason"
    is SyncFailure.ServerSaid -> if (code > 0) "error $code · $message" else message
    is SyncFailure.Unreadable -> "unreadable answer · $reason"
}

private fun lastSyncLabel(at: Long): String {
    if (at <= 0) return "never"
    val ago = (System.currentTimeMillis() / 1000) - at
    return when {
        ago < 60 -> "just now"
        ago < 3600 -> "${ago / 60} min ago"
        ago < 86_400 -> "${ago / 3600} hr ago"
        else -> "${ago / 86_400} days ago"
    }
}
