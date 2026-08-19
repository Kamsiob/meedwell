package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * The mobile data question, asked once, at the first download.
 *
 * **The default is Wi-Fi only and it stays that way.** The whole library is
 * something over five hundred megabytes and the largest single recording is
 * twenty five, which is not a surprise anybody should meet on their bill. But a
 * careful default nobody was told about is a setting they have to go and find
 * after a download has already refused, and being sent to look for a switch is
 * not care, it is homework.
 *
 * So it is put plainly, once, at the exact moment it matters, with the cost of
 * each answer stated rather than implied. Either answer counts as answered: "on
 * Wi-Fi only" is a real choice and must not mean being asked again tomorrow.
 *
 * The line about Settings is there because the most common regret about a
 * one-time question is not knowing it can be revisited.
 */
@Composable
fun CellularSheet(
    onAnswer: (allowCellular: Boolean) -> Unit,
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
            Text("Download over mobile data?", style = type.h2, color = colors.primaryText)
            Text(
                "Recordings are large: the whole library is about 530 MB. Meedwell waits for " +
                    "Wi-Fi unless you say otherwise.",
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 10.dp),
            )

            PillButton(
                label = "Wi-Fi only",
                onClick = { onAnswer(false) },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
            GhostButton(
                label = "Wi-Fi and mobile data",
                onClick = { onAnswer(true) },
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            )

            Text(
                "You can change this any time in Settings.",
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 22.dp)
                    .defaultMinSize(minHeight = 20.dp),
            )
        }
    }
}
