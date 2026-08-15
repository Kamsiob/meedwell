package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.R
import com.kamsiob.meedwell.ui.components.MeedwellMark
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 03 in the visual reference: Welcome.
 *
 * Two doors, and the second one is a real product rather than a fallback. A
 * user who taps "Just play my local files" must never meet sync language the
 * app cannot honor, which is why it sits here as an equal choice instead of
 * hiding behind the first.
 */
@Composable
fun WelcomeScreen(
    onConnect: () -> Unit,
    onLocalOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MeedwellMark(
                size = 90.dp,
                contentDescription = stringResource(R.string.mark_description),
            )

            Box(Modifier.height(26.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = type.h1,
                color = colors.primaryText,
            )
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = type.voice,
                color = colors.primaryText.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = stringResource(R.string.welcome_body),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 22.dp),
            )

            Box(Modifier.height(38.dp))

            PillButton(
                label = stringResource(R.string.welcome_connect),
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButtonRow(
                label = stringResource(R.string.welcome_local_only),
                onClick = onLocalOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            )

            Text(
                text = stringResource(R.string.welcome_footer),
                style = type.section,
                color = colors.primaryText.copy(alpha = 0.52f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 42.dp),
            )
        }
    }
}
