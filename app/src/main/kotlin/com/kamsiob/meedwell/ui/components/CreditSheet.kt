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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kamsiob.meedwell.core.surroundings.CreditBlock
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * Who made one recording, under what terms.
 *
 * The second of the three attribution surfaces, between the one-line credit on
 * every row and the full list on the credits screen. It exists because the row
 * has space for a name and a license and nothing else, and the credits screen
 * is a document rather than an answer to "what is this one".
 *
 * **Every line is generated from the manifest.** Nothing here is typed, so
 * nothing here can drift out of step with the published library or with what
 * the credits screen says about the same recording.
 *
 * The uploader's own extra conditions are reproduced word for word. Nine
 * recordings carry one, and paraphrasing somebody's credit request is not
 * crediting them.
 */
@Composable
fun CreditSheet(
    title: String,
    originalTitle: String,
    credit: CreditBlock?,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    /**
     * Whether this recording can be taken off the phone.
     *
     * **Removal lived only on the storage screen**, at the foot of a tab, a long
     * way from the recording somebody is actually looking at. This sheet opens
     * from every row on every list, so it is where the question is asked.
     * Bundled recordings do not offer it, because they cannot go.
     */
    canRemove: Boolean = false,
    onRemove: () -> Unit = {},
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
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
        ) {
            SheetHandle(onDismiss = onDismiss, modifier = Modifier.padding(top = 12.dp))

            // The catalog description leads, as it does on the row. The
            // uploader's own title is a field further down rather than the
            // heading: it is often a filename, and "Wind_grass_sheep_Ouessant"
            // set as a heading looks like the app failed to load something.
            Text(
                title,
                style = type.h2,
                color = colors.primaryText,
                modifier = Modifier.padding(top = 18.dp),
            )

            if (credit == null) {
                // Unreachable through the interface, because an incomplete
                // entry is filtered out before anything can be offered. Said
                // plainly rather than left blank, because a blank credit is the
                // one failure this surface must not have.
                Text(
                    "This recording is missing the credit it has to carry, so Meedwell does not offer it.",
                    style = type.body,
                    color = colors.primaryText,
                    modifier = Modifier.padding(top = 18.dp, bottom = 26.dp),
                )
                return@Column
            }

            Field("RECORDED BY", credit.recordist)
            credit.recordistUrl?.let { Link("Their page ↗") { onOpenUrl(it) } }

            Field("LICENSE", credit.licenseFullName.ifBlank { credit.licenseLabel })
            Link("Read the license ↗") { onOpenUrl(credit.licenseUrl) }

            Field("CHANGED BY MEEDWELL", credit.modificationNote)

            credit.extraConditions?.let {
                // Word for word, and marked as the uploader's own words rather
                // than the app's.
                Field("THE UPLOADER ALSO ASKED", it)
            }

            // Named exactly as its maker named it, which is what somebody
            // tracing the source back to Freesound needs.
            if (!originalTitle.equals(title, ignoreCase = true)) {
                Field("THE MAKER CALLED IT", originalTitle)
            }

            Field("WHERE IT CAME FROM", "Freesound")
            Link("The original recording ↗") { onOpenUrl(credit.sourceUrl) }

            if (canRemove) {
                Hairline(Modifier.padding(top = 8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Button) { onRemove(); onDismiss() }
                        .padding(top = 14.dp)
                        .semantics { contentDescription = "Remove this recording from the phone" },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Remove from this phone", style = type.chip, color = colors.alarm)
                }
                Text(
                    "It can be fetched again whenever you want it.",
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Box(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column(Modifier.padding(top = 18.dp)) {
        Text(label, style = type.section, color = colors.tertiaryText)
        Text(value, style = type.body, color = colors.primaryText, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Link(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MeedwellTheme.typography.meta, color = MeedwellTheme.colors.secondaryText)
    }
}
