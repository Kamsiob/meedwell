package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * Naming something, on one line.
 *
 * Used to make a list and to rename one. A sheet rather than a platform alert
 * dialog for the same reason the time picker is: an AlertDialog is a Material
 * surface with its own colours and elevation, and one would be the loudest thing
 * on any screen it appeared over.
 *
 * The field takes focus and raises the keyboard on open, and the keyboard's own
 * done key commits, because a naming sheet that needs a second tap to reach a
 * button is a naming sheet somebody abandons.
 */
@Composable
fun NameSheet(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

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
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
        ) {
            Box(Modifier.height(20.dp))
            Text(title, style = type.h2, color = colors.primaryText)

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .defaultMinSize(minHeight = 48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = type.rowTitle.copy(color = colors.primaryText),
                cursorBrush = SolidColor(colors.moss),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit(text, onConfirm, onDismiss) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = title },
            )
            }
            // The one underline in the app, and it is a text field, which is the
            // one thing that genuinely needs a line to write on.
            Hairline()

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .semantics { contentDescription = "Cancel" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = type.button, color = colors.secondaryText)
                }
                Box(
                    Modifier
                        .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                        .clickable(role = Role.Button) { commit(text, onConfirm, onDismiss) }
                        .semantics { contentDescription = confirmLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(confirmLabel, style = type.button, color = colors.mossInk)
                }
            }
        }
    }
}

/** A blank name is not a name, so an empty field simply closes. */
private fun commit(text: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val clean = text.trim()
    if (clean.isNotEmpty()) onConfirm(clean)
    onDismiss()
}
