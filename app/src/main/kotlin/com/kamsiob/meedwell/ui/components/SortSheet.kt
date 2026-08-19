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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.data.ShelfScope
import com.kamsiob.meedwell.data.ShelfSort
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * How the shelf is ordered, and what it is showing.
 *
 * The sort label sat at the top of the shelf from the first build and did
 * nothing when tapped, which is the kind of dead control that reads as the app
 * being broken rather than unfinished.
 *
 * **The list of orders is shorter than the reference asked for, deliberately.**
 * Verification found that `getAlbumList2` returns nothing at all for
 * `frequent`, `recent` and `highest`, so the specified "most played" cannot come
 * from the API. It is computed here from the on-device play log instead, which
 * is better anyway: it reflects what this listener actually played rather than
 * whatever Bandcamp does or does not count. See issue #48.
 *
 * The scope filters live here rather than beside Albums, Artists and Genres,
 * so they do not compete with the view switcher.
 */
@Composable
fun SortSheet(
    sort: ShelfSort,
    scope: ShelfScope,
    onPick: (ShelfSort, ShelfScope) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Close" },
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
                .padding(horizontal = 26.dp),
        ) {
            SheetHandle(onDismiss = onDismiss, modifier = Modifier.padding(top = 12.dp))

            Text(
                "ORDER",
                style = type.section,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 20.dp, bottom = 2.dp),
            )
            ShelfSort.entries.forEach { option ->
                PickRow(
                    label = option.label(),
                    note = option.note(),
                    selected = option == sort,
                    onClick = { onPick(option, scope); onDismiss() },
                )
            }

            Text(
                "SHOWING",
                style = type.section,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 22.dp, bottom = 2.dp),
            )
            ShelfScope.entries.forEach { option ->
                PickRow(
                    label = option.label(),
                    note = option.note(),
                    selected = option == scope,
                    onClick = { onPick(sort, option); onDismiss() },
                )
            }

            Box(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun PickRow(label: String, note: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = type.rowTitle,
                color = if (selected) colors.primaryText else colors.secondaryText,
            )
            if (note != null) {
                Text(note, style = type.meta, color = colors.tertiaryText, modifier = Modifier.padding(top = 2.dp))
            }
        }
        // A dot rather than a tick, and only when chosen. Selection is carried
        // by ink as well, so the dot is not the only signal.
        if (selected) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.primaryText)
            )
        }
    }
}

private fun ShelfSort.label(): String = when (this) {
    ShelfSort.Artist -> "Artist A to Z"
    ShelfSort.Title -> "Title A to Z"
    ShelfSort.Recent -> "Recently added"
    ShelfSort.MostPlayed -> "Most played"
}

private fun ShelfSort.note(): String? = when (this) {
    ShelfSort.Recent -> "By when it landed on your shelf"
    // Says where the number comes from, because "most played" on a streaming
    // service usually means something counted elsewhere.
    ShelfSort.MostPlayed -> "Counted on this phone, from your own listening"
    else -> null
}

private fun ShelfScope.label(): String = when (this) {
    ShelfScope.Everything -> "Everything"
    ShelfScope.OnThisPhone -> "Only what is here as files"
    ShelfScope.LocalOnly -> "Only local music"
}

private fun ShelfScope.note(): String? = when (this) {
    ShelfScope.OnThisPhone -> "Records you have the files for"
    ShelfScope.LocalOnly -> "Music that never came from Bandcamp"
    else -> null
}
