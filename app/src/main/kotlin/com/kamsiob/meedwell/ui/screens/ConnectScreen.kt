package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.R
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 04 in the visual reference: Connect.
 *
 * Three fields, the address prefilled, paste chips on the two that get pasted,
 * and a reveal eye on the password. Beta-honest copy throughout.
 *
 * The address prefilled is `https://bandcamp.com/api/subsonic`, exactly what
 * Bandcamp shows the user. The client appends `/rest/` itself, which is
 * standard Subsonic behavior; showing the user a URL Bandcamp never gave them
 * would be confusing and would break if they pasted the real one.
 */
@Composable
fun ConnectScreen(
    state: ConnectState,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onOpenBandcampSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val clipboard = LocalClipboardManager.current
    var revealPassword by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Text(
                text = "Connect your collection",
                style = type.h2,
                color = colors.primaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Bandcamp gives you three lines. Paste them in.",
                style = type.voice,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 10.dp),
            )
            // The login caveat is stated **before** the button rather than
            // after it. Bandcamp's fan settings page only resolves for a signed
            // in browser; a signed out one lands somewhere that looks nothing
            // like these instructions, and somebody who has already tapped
            // through has no way to tell whether the app sent them wrong.
            Text(
                text = "Find them under Fan Settings, then Subsonic, on bandcamp.com. " +
                    "Sign in to Bandcamp in your browser first, or the page will not open to your settings.",
                style = type.meta,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )
            TextButtonRow(
                label = "Open it for me ↗",
                onClick = onOpenBandcampSettings,
                modifier = Modifier.align(Alignment.Start),
            )

            CredentialField(
                label = "Server address · filled in for you",
                value = state.server,
                onValueChange = onServerChange,
                showPaste = false,
                clipboardText = { clipboard.getText()?.text },
                modifier = Modifier.padding(top = 8.dp),
            )
            CredentialField(
                label = "Username",
                value = state.username,
                onValueChange = onUsernameChange,
                showPaste = true,
                clipboardText = { clipboard.getText()?.text },
            )
            CredentialField(
                label = "Password",
                value = state.password,
                onValueChange = onPasswordChange,
                showPaste = true,
                isPassword = true,
                revealed = revealPassword,
                onToggleReveal = { revealPassword = !revealPassword },
                clipboardText = { clipboard.getText()?.text },
            )

            state.error?.let { error ->
                ConnectError(error, modifier = Modifier.padding(top = 20.dp))
            }

            Box(Modifier.height(28.dp))

            Text(
                text = "Stored only on this phone, encrypted. Meedwell talks to one server, Bandcamp's, and no one else.",
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            )
            PillButton(
                // An action keeps the same name through its whole flow, so this
                // says Connect while idle and describes itself while working
                // rather than turning into a spinner with no label.
                label = if (state.checking) "Checking" else stringResource(R.string.where_connect),
                onClick = { if (!state.checking) onConnect() },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButtonRow(
                label = "Not now",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

/**
 * What went wrong, said plainly.
 *
 * This is where verification changed the interface. Bandcamp does not return
 * Subsonic error code 40 for a bad login; it returns HTTP 500 with an empty
 * body. So the screen cannot show a code, and inventing one would be worse than
 * saying what actually happened.
 */
@Composable
private fun ConnectError(error: ConnectError, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    val (headline, detail) = when (error) {
        ConnectError.Rejected ->
            "Bandcamp turned those down" to
                "The username or the password is not what Bandcamp expects. They are long and easy to " +
                "clip when copying, so it is worth pasting both again. If they still do not work, " +
                "generate a fresh pair on Bandcamp and paste those."
        ConnectError.Unreachable ->
            "Could not reach Bandcamp" to
                "The request did not get an answer. That is usually the connection on this phone rather " +
                "than anything you typed."
        ConnectError.NotSubsonic ->
            "That address did not answer like Bandcamp" to
                "Meedwell got a reply, but not one it recognizes. Check the server address matches the " +
                "one on the Subsonic page of your Fan Settings."
        is ConnectError.ServerSaid ->
            "Bandcamp answered with a problem" to error.message
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.recess)
            .padding(14.dp),
    ) {
        Text(headline, style = type.rowTitle, color = colors.primaryText)
        Text(
            detail,
            style = type.meta,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun CredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showPaste: Boolean,
    clipboardText: () -> String?,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    revealed: Boolean = false,
    onToggleReveal: (() -> Unit)? = null,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier = modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            text = label.uppercase(),
            style = type.section,
            color = colors.secondaryText,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            // Centered in a minimum-height box rather than stretched to a
            // fixed one: the fixed height both floated the cursor to the top
            // and clipped the line at large font scale.
            Box(
                Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = type.body.fontFamily,
                    fontSize = type.body.fontSize,
                    color = colors.primaryText,
                ),
                cursorBrush = SolidColor(colors.primaryText),
                visualTransformation = if (isPassword && !revealed) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                // The keyboard is told what these fields are, which matters
                // more than it looks. Without KeyboardType.Password the IME
                // treats the password as ordinary prose: it learns it, and it
                // prints it in the suggestion strip in plain sight. Caught on
                // the device rather than in review.
                //
                // The username gets Ascii with capitalisation and autocorrect
                // off, because Bandcamp's generated username is a long
                // uppercase token that autocorrect will happily mangle.
                keyboardOptions = if (isPassword) {
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                    )
                } else {
                    KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = label
                        if (isPassword) password()
                    },
            )
            }

            if (isPassword && onToggleReveal != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = onToggleReveal)
                        .semantics {
                            contentDescription = if (revealed) "Hide the password" else "Show the password"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (revealed) "Hide" else "Show",
                        style = type.meta,
                        color = colors.secondaryText,
                    )
                }
            }

            if (showPaste) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) {
                            clipboardText()?.let { onValueChange(it.trim()) }
                        }
                        .padding(horizontal = 14.dp)
                        .semantics { contentDescription = "Paste into $label" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Paste", style = type.meta, color = colors.primaryText)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}

/** Keeps a text field's own target at the 48dp floor even though it reads as a line. */
private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(48.dp))

data class ConnectState(
    val server: String,
    val username: String = "",
    val password: String = "",
    val checking: Boolean = false,
    val error: ConnectError? = null,
) {
    val canSubmit: Boolean get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

sealed interface ConnectError {
    /** HTTP 500 with an empty body, which is how Bandcamp rejects a login. */
    data object Rejected : ConnectError
    data object Unreachable : ConnectError
    data object NotSubsonic : ConnectError
    data class ServerSaid(val message: String) : ConnectError
}
