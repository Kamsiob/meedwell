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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.kamsiob.meedwell.ui.screens.ListSummary
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * Where to put this.
 *
 * Reached from the action sheet on any track or album, and from the player,
 * which is what makes "add the thing I am listening to" one tap from anywhere
 * rather than a trip to a screen.
 *
 * **Only lists this phone made.** A Bandcamp list cannot be added to, because
 * their API implements no way to change one, so offering it would be a row that
 * fails silently. They are left out rather than shown greyed, and the empty case
 * says what to do instead.
 */
@Composable
fun AddToListSheet(
    lists: List<ListSummary>,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
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
            Box(Modifier.height(20.dp))
            Text("Add to a list", style = type.h2, color = colors.primaryText)

            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable(role = Role.Button, onClick = onNew)
                    .padding(vertical = 13.dp)
                    .semantics { contentDescription = "Make a new list and add it there" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New list", style = type.rowTitle, color = colors.mossInk, modifier = Modifier.weight(1f))
                MeedwellIcon(MeedwellIcons.ChevronRight, size = 14.dp, tint = colors.tertiaryText)
            }
            Hairline()

            if (lists.isEmpty()) {
                Text(
                    "You have no lists yet. Make one and this goes straight into it.",
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                )
            }

            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(lists, key = { it.id }) { list ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clickable(role = Role.Button) { onPick(list.id) }
                            .padding(vertical = 12.dp)
                            .semantics { contentDescription = "Add to ${list.name}" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                list.name,
                                style = type.rowTitle,
                                color = colors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                list.subtitle,
                                style = type.rowSub,
                                color = colors.tertiaryText,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Hairline()
                }
            }
            Box(Modifier.height(22.dp))
        }
    }
}
