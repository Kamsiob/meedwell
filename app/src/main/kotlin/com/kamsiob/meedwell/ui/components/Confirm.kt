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
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * A confirmation for something that cannot be undone.
 *
 * Two actions in Settings used to fire on a single tap, and both wore a chevron
 * identical to the rows beside them that merely navigate: erasing the listening
 * history, and disconnecting the account. The history is the only genuinely
 * irreplaceable thing the app holds, since everything else re-syncs, and there
 * is no export yet.
 *
 * The rule this encodes: **name the consequence, not the action.** "Are you
 * sure?" tells somebody nothing. What is about to be lost, and what is safe,
 * tells them everything.
 */
@Composable
fun ConfirmSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Cancel" },
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
            Text(title, style = type.h2, color = colors.primaryText)
            Text(
                body,
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    label = confirmLabel,
                    onClick = { onConfirm(); onDismiss() },
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.width(14.dp))
                // The safe choice is the plain one. Making "keep it" the
                // quieter of the two would be pushing somebody toward the door.
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Keep it", style = type.button, color = colors.secondaryText)
                }
            }
        }
    }
}

/** What a pending confirmation is about. */
sealed interface PendingConfirm {
    data object EraseHistory : PendingConfirm
    data object Disconnect : PendingConfirm

    /**
     * Confirmed before the file picker rather than after it.
     *
     * Choosing a file should not be the moment somebody learns that opening it
     * will replace their listening history.
     */
    data object Restore : PendingConfirm

    /**
     * Deleting a list.
     *
     * Worth confirming even though nothing musical is lost: an order somebody
     * built by hand is not recoverable from anywhere, unlike the tracks in it.
     */
    data class DeleteList(val id: String) : PendingConfirm
}
