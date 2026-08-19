package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.surroundings.CreditBlock
import com.kamsiob.meedwell.core.surroundings.LicenseGroup
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The credits screen: every recording, grouped by license.
 *
 * This is the surface the licenses actually compel. Twenty-one of the
 * Surroundings recordings are CC BY, where crediting the maker, naming the
 * license **with its version**, linking the source and stating that the file was
 * modified are conditions of use rather than courtesies. The other ninety are
 * CC0 and need none of it; they are credited anyway.
 *
 * Every word here is generated from `manifest.json` rather than typed into a
 * string resource. A credit typed by hand drifts the moment the library
 * changes, and a drifted credit on a CC BY file is a license breach rather than
 * a stale label.
 *
 * Licenses that impose conditions are listed first, so somebody checking whether
 * the app honors its obligations does not scroll past ninety public domain
 * entries to find the ones that matter.
 */
@Composable
fun CreditsScreen(
    summary: String,
    groups: List<LicenseGroup>,
    loadError: String? = null,
    softwareNotices: List<SoftwareNotice>,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        DetailHeader("Credits and licenses", onBack)
        LazyColumn(contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)) {

            item(key = "summary") {
                if (loadError != null) {
                    // A licensing surface that fails must say so. Silently
                    // showing no recordings would look like there are none.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.recess)
                            .padding(14.dp),
                    ) {
                        Text(
                            "The recording credits could not be loaded",
                            style = type.rowTitle,
                            color = colors.primaryText,
                        )
                        Text(
                            "This is a bug worth reporting, because these recordings are used under " +
                                "licenses that require their makers to be credited. The full credits are " +
                                "also published at github.com/Kamsiob/meedwell-surroundings.",
                            style = type.meta,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            loadError,
                            style = type.meta,
                            color = colors.tertiaryText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    Text(
                        summary,
                        style = type.voice,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            groups.forEach { group ->
                item(key = "h-" + group.licenseLabel) {
                    Column(Modifier.padding(top = 24.dp, bottom = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                group.licenseLabel.uppercase(),
                                style = type.section,
                                // Tertiary, like every section head. Primary ink
                                // made the license labels shout over the very
                                // credits they were grouping.
                                color = colors.tertiaryText,
                            )
                            Text(
                                "${group.entries.size}",
                                style = type.numeric,
                                color = colors.tertiaryText,
                            )
                        }
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(role = Role.Button) { onOpenUrl(group.licenseUrl) }
                                .semantics { contentDescription = "Read the ${group.licenseLabel} license" },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                "Read the license ↗",
                                style = type.meta,
                                color = colors.secondaryText,
                            )
                        }
                    }
                }

                items(group.entries, key = { group.licenseLabel + "-" + it.id }) { entry ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable(role = Role.Button) { onOpenUrl(entry.sourceUrl) }
                                .padding(vertical = 10.dp)
                                .semantics {
                                    contentDescription =
                                        "${entry.title} by ${entry.recordist}. Open the original recording."
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = type.meta,
                                    color = colors.primaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    entry.recordist,
                                    style = type.meta,
                                    color = colors.tertiaryText,
                                    maxLines = 1,
                                )
                                // Word for word. Paraphrasing somebody's credit
                                // request is not crediting them.
                                entry.extraConditions?.let {
                                    Text(
                                        it,
                                        style = type.meta,
                                        color = colors.secondaryText,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                            }
                            MeedwellIcon(
                                icon = MeedwellIcons.ChevronRight,
                                size = 14.dp,
                                tint = colors.tertiaryText,
                            )
                        }
                        Hairline()
                    }
                }
            }

            if (softwareNotices.isNotEmpty()) {
                item(key = "software-head") {
                    Column(Modifier.padding(top = 24.dp, bottom = 4.dp)) {
                        SectionHead("Software")
                    }
                }
                items(softwareNotices, key = { "s-" + it.name }) { notice ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable(role = Role.Button) { onOpenUrl(notice.url) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(notice.name, style = type.meta, color = colors.primaryText)
                                Text(notice.license, style = type.meta, color = colors.tertiaryText)
                            }
                            MeedwellIcon(
                                icon = MeedwellIcons.ChevronRight,
                                size = 14.dp,
                                tint = colors.tertiaryText,
                            )
                        }
                        Hairline()
                    }
                }
            }

            item(key = "foot") {
                // CC BY asks that a change to the work be indicated, so the
                // fact stays. Explaining that it is a legal requirement does
                // not, because nobody reading a credits page came for that.
                Text(
                    "All of these were trimmed, level matched and re-encoded.",
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 26.dp),
                )
            }
        }
    }
}

/**
 * The full credit for one recording, shown on its detail sheet.
 *
 * Everything the license compels, one tap behind the quiet line under the
 * player. That relationship is what lets the player line be as subtle as it is:
 * the obligation is met in full here, so the player does not have to shout.
 */
@Composable
fun RecordingCredit(
    block: CreditBlock,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.recess)
            .padding(14.dp),
    ) {
        SectionHead("Recorded by")
        Text(
            block.recordist,
            style = type.rowTitle,
            color = colors.primaryText,
            modifier = Modifier.padding(top = 3.dp),
        )

        block.extraConditions?.let {
            Text(
                it,
                style = type.meta,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            // The version is never dropped: CC BY 3.0 and CC BY 4.0 are
            // different licenses with different terms.
            block.licenseFullName,
            style = type.meta,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            block.modificationNote,
            style = type.meta,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(Modifier.padding(top = 6.dp)) {
            LinkChip("The original recording ↗") { onOpenUrl(block.sourceUrl) }
            LinkChip("The license ↗") { onOpenUrl(block.licenseUrl) }
        }
    }
}

@Composable
private fun LinkChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = MeedwellTheme.typography.meta, color = MeedwellTheme.colors.secondaryText)
    }
}

/**
 * The one quiet line under a playing recording.
 *
 * Tertiary ink, one row, tappable. Deliberately understated: it is a line under
 * an ambience player, not a banner. It is legitimate rather than a shortcut
 * because the full block it opens carries everything the license requires.
 */
@Composable
fun RecordingCreditLine(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$text. Open the full credit." },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = MeedwellTheme.typography.meta,
            color = MeedwellTheme.colors.tertiaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class SoftwareNotice(val name: String, val license: String, val url: String)
