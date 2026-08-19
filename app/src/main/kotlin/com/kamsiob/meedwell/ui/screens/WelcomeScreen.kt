package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.R
import com.kamsiob.meedwell.core.library.Voicing
import com.kamsiob.meedwell.ui.components.GhostButton
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.MeedwellMark
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.SeedHeadPlate
import com.kamsiob.meedwell.ui.components.StaffRule
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Grid screen 01: the declaration.
 *
 * **This is the first thing anybody sees, and it says what kind of app this is
 * before it asks for anything.** The screen that used to be here was a generic
 * welcome with a tagline and two doors, which said nothing a hundred other
 * players do not say, and its body copy promised files "yours to keep as real
 * files on your phone". Verification found Bandcamp offers no download endpoint
 * at all, so the very first sentence in the app was a promise it could not keep.
 *
 * The grid's design is a declaration made once, plainly, with a second button
 * that **refuses nobody**: somebody who listens to something else is told what
 * this was built for and then waved straight through. That is the difference
 * between a position and a gate.
 *
 * The engraved plate sets the register before a single album loads. A staff sits
 * under the wordmark because that is a section head; nothing else here is ruled.
 */
@Composable
fun WelcomeScreen(
    onAgree: () -> Unit,
    onCarryOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport)
                .padding(horizontal = 22.dp),
        ) {
            // The mark and the wordmark are centred; everything below is set
            // to the left margin like a page of type.
            // **Bare.** The dark rounded square is the launcher icon's frame,
            // not the mark. Inside the app the mark sits on the paper itself,
            // which is what the grid draws and what keeps a tile from reading
            // as the one card in a design that has none.
            Box(Modifier.fillMaxWidth().padding(top = 38.dp), contentAlignment = Alignment.Center) {
                MeedwellMark(
                    size = 58.dp,
                    bare = true,
                    contentDescription = stringResource(R.string.mark_description),
                )
            }
            Text(
                stringResource(R.string.app_name),
                style = type.programme.copy(fontSize = 25.sp, lineHeight = 30.sp),
                color = colors.primaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            StaffRule(Modifier.padding(top = 22.dp))

            Text(
                stringResource(R.string.declare_opening),
                style = type.serifOpening,
                color = colors.primaryText,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                stringResource(R.string.declare_body_1),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                stringResource(R.string.declare_body_2),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 11.dp),
            )

            Box(Modifier.fillMaxWidth().padding(top = 22.dp), contentAlignment = Alignment.Center) {
                SeedHeadPlate()
            }

            // Pushes the buttons to the foot on a tall screen, and collapses to
            // nothing on a short one rather than forcing a scroll.
            Box(Modifier.weight(1f).heightIn(min = 22.dp))

            PillButton(
                label = stringResource(R.string.declare_agree),
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                label = stringResource(R.string.declare_carry_on),
                onClick = onCarryOn,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            )
            Box(Modifier.height(24.dp))
        }
    }
}

/**
 * Grid screen 02: two ways in, equally weighted.
 *
 * **Local-only is a first-class path, not a fallback**, so it gets the same
 * amount of page, its own section head, and a button the same size and shape as
 * the other one. Somebody who taps "Choose folders" must never meet sync
 * language the app cannot honor.
 *
 * The privacy line at the foot is placed as fact rather than reassurance, which
 * is the register every claim in this app uses.
 */
@Composable
fun WhereMusicScreen(
    onJustListen: () -> Unit,
    onConnect: () -> Unit,
    onChooseFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport)
                .padding(horizontal = 22.dp),
        ) {
            Text(
                stringResource(R.string.where_opening),
                style = type.serifOpening.copy(fontSize = 27.sp, lineHeight = 33.sp),
                color = colors.primaryText,
                modifier = Modifier.padding(top = 52.dp),
            )
            Text(
                stringResource(R.string.where_body),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )

            SectionHead(stringResource(R.string.where_bandcamp), Modifier.padding(top = 28.dp))
            Text(
                stringResource(R.string.where_bandcamp_body),
                style = type.body.copy(fontSize = 13.sp),
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
            PillButton(
                label = stringResource(R.string.where_connect),
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )

            SectionHead(stringResource(R.string.where_phone), Modifier.padding(top = 28.dp))
            Text(
                stringResource(R.string.where_phone_body),
                style = type.body.copy(fontSize = 13.sp),
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
            GhostButton(
                label = stringResource(R.string.where_choose_folders),
                onClick = onChooseFolders,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )

            Box(Modifier.weight(1f).heightIn(min = 26.dp))
            // **The third door.** Somebody with no Bandcamp and no files yet
            // was locked out of the whole app, including the one feature built
            // to work on a first evening with nothing. Quiet on purpose: it is
            // an aside, not a peer of the two real ways in.
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onJustListen)
                    .padding(top = 14.dp)
                    .semantics { contentDescription = "Just listen for now" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Just listen for now",
                    style = type.meta,
                    color = colors.tertiaryText,
                )
            }


            Text(
                stringResource(R.string.where_footer),
                style = type.meta,
                color = colors.tertiaryText,
            )
            Box(Modifier.height(26.dp))
        }
    }
}

/**
 * Grid screen 03: tone, disclosed at onboarding.
 *
 * **A default that alters playback has to be declared, not discovered.**
 * Meedwell ships with a voicing already applied, and an app that quietly
 * equalises your music and never mentions it has taken a decision on your behalf
 * and hidden it. So it is said here, on the way in, with the opt-out sitting
 * beside it rather than buried three screens deep.
 *
 * Only two of the five voicings appear. This is the moment for a decision, not a
 * menu; the other three are in More, and the footer says so.
 *
 * The honest limit about the phone's own processing is stated here as well as on
 * the Tone screen, because this is where the claim is first made.
 */
@Composable
fun ToneIntroScreen(
    voicing: Voicing,
    onPick: (Voicing) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport)
                .padding(horizontal = 22.dp),
        ) {
            Text(
                stringResource(R.string.tone_intro_opening),
                style = type.serifOpening.copy(fontSize = 27.sp, lineHeight = 33.sp),
                color = colors.primaryText,
                modifier = Modifier.padding(top = 52.dp),
            )
            Text(
                stringResource(R.string.tone_intro_body_1),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                stringResource(R.string.tone_intro_body_2),
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 11.dp),
            )

            SectionHead("Tone", Modifier.padding(top = 26.dp, bottom = 6.dp))

            IntroVoicingRow(
                label = Voicing.Orchestral.label,
                note = stringResource(R.string.tone_intro_orchestral_sub),
                selected = voicing == Voicing.Orchestral,
                onClick = { onPick(Voicing.Orchestral) },
            )
            IntroVoicingRow(
                label = Voicing.AsRecorded.label,
                note = Voicing.AsRecorded.note,
                selected = voicing == Voicing.AsRecorded,
                onClick = { onPick(Voicing.AsRecorded) },
            )

            Text(
                stringResource(R.string.tone_intro_footer),
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 16.dp),
            )

            Box(Modifier.weight(1f).heightIn(min = 26.dp))

            PillButton(
                label = stringResource(R.string.tone_intro_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun IntroVoicingRow(
    label: String,
    note: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 11.dp)
            .semantics {
                contentDescription = "$label. $note"
                this.selected = selected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = type.rowTitle,
                color = if (selected) colors.mossInk else colors.primaryText,
            )
            Text(
                note,
                style = type.rowSub,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selected) {
            MeedwellIcon(MeedwellIcons.Check, size = 15.dp, tint = colors.mossInk)
        }
    }
}
