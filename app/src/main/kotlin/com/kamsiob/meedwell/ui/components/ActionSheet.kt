package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.Elevation
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.meedwellShadow

/**
 * Screen 18 in the visual reference: the action sheet.
 *
 * **One component, every surface.** Long-press anywhere a track or album lives
 * and the same verbs appear in the same order. `ISSUES-SEED.md` is explicit
 * that this must be "one shared component rather than per-screen variants",
 * because a verb that moves position between screens is worse than one that is
 * missing: the user learns a place and then it lies to them.
 *
 * The verb list changed after API verification, and the changes are the honest
 * ones rather than the convenient ones:
 *
 *  - **Download is gone.** There is no download endpoint on Bandcamp's API. A
 *    row that cannot work is worse than no row.
 *  - **Love states its limit.** `star` works and reaches the account; `unstar`
 *    is broken server side. The row says so at the moment it matters rather
 *    than failing silently when tapped.
 *  - **Add to a list is gone.** Making lists is not built, and Bandcamp's API
 *    offers no way to create or change a playlist even once it is. The Lists
 *    screen says where that stands; a row here could only apologize.
 */
@Composable
fun ActionSheet(
    target: ActionTarget,
    onAction: (SheetAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(
        Modifier
            .fillMaxSize()
            // A scrim that dismisses. Tapping outside a sheet to close it is
            // the thing everybody tries first.
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Close" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .meedwellShadow(Elevation.sheet, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(colors.background)
                // Consumes taps so they do not fall through to the scrim.
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .width(36.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.secondaryText.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // The header names what the verbs will act on, so a long-press on
            // the wrong row is obvious before anything is done.
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverThumb(url = target.coverUrl, title = target.title, size = 44.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        target.title,
                        style = type.rowTitle,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        target.subtitle,
                        style = type.metadata,
                        color = colors.tertiaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))

            val actions = target.actions()
            actions.forEachIndexed { index, action ->
                ActionRow(
                    action = action,
                    // A hairline separates rows. Under the last one it is
                    // separating the list from nothing, and it read as the
                    // sheet being cut off rather than finished.
                    divider = index < actions.lastIndex,
                    onClick = { onAction(action); onDismiss() },
                )
            }

            Box(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ActionRow(action: SheetAction, divider: Boolean, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp)
                .semantics { contentDescription = action.label + (action.note?.let { ". $it" } ?: "") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(action)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(action.label, style = type.rowTitle, color = colors.primaryText)
                action.note?.let {
                    Text(it, style = type.metadata, color = colors.tertiaryText, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        if (divider) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        }
    }
}

@Composable
private fun ActionIcon(action: SheetAction) {
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        MeedwellIcon(
            icon = action.icon,
            contentDescription = null,
            tint = MeedwellTheme.colors.secondaryText,
        )
    }
}

/** What the sheet was opened on. */
data class ActionTarget(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val kind: Kind,
    val isStarred: Boolean = false,
    val artistId: String? = null,
) {
    enum class Kind { Album, Track }

    /**
     * The verbs, in a fixed order, every time.
     *
     * Order is part of the contract. Somebody who has used the sheet twice
     * reaches for the third row without reading it.
     */
    fun actions(): List<SheetAction> = buildList {
        add(SheetAction.PlayNext)
        add(SheetAction.AddToQueue)
        // The heart, with its real limit attached rather than hidden.
        add(if (isStarred) SheetAction.AlreadyLoved else SheetAction.Love)
        add(SheetAction.ViewArtwork)
        if (artistId != null) add(SheetAction.GoToArtist)
        add(SheetAction.Share)
    }
}

sealed class SheetAction(
    val label: String,
    val note: String?,
    val icon: MeedwellIcons,
) {
    data object PlayNext : SheetAction("Play next", null, MeedwellIcons.PlayNext)
    data object AddToQueue : SheetAction("Add to queue", null, MeedwellIcons.Queue)

    /** `star` works and reaches the account. */
    data object Love : SheetAction("Love", "Goes to your Bandcamp account", MeedwellIcons.Heart)

    /**
     * `unstar` is broken on Bandcamp's side and returns an error whatever is
     * sent. Rather than offering a control that silently fails, the row states
     * the limit and points at the one place it can be done.
     */
    data object AlreadyLoved : SheetAction(
        "Loved",
        "Bandcamp cannot take a heart off yet. Use their website.",
        MeedwellIcons.HeartFilled,
    )

    data object ViewArtwork : SheetAction("View artwork", null, MeedwellIcons.Artwork)
    data object GoToArtist : SheetAction("Go to artist", null, MeedwellIcons.Artist)
    data object Share : SheetAction("Share", "A plain Bandcamp link, nothing fetched", MeedwellIcons.Share)
}
