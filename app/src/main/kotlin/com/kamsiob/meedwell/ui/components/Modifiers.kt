package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Tap to open, long press to open the action sheet.
 *
 * Wrapped in one place because the action sheet appears on **every** surface a
 * track or album lives on, with the same eight verbs in the same order. Having
 * one helper is what makes "one shared component rather than per-screen
 * variants" true in the code rather than only in the specification.
 *
 * The long press also gets a named accessibility action, because a gesture that
 * has no equivalent for a TalkBack user is a feature that user does not have.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableCompat(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String = "More actions",
): Modifier = this.combinedClickable(
    role = Role.Button,
    onClick = onClick,
    onLongClick = onLongClick,
    onLongClickLabel = onLongClickLabel,
)
