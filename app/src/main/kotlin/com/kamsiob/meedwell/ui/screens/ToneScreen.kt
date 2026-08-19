package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.library.Voicing
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Grid screen 17: Tone.
 *
 * **Named by musical umbrella, not by genre.** The list says more by what it
 * omits than any wording could: no rock, no electronic, no bass anything.
 *
 * The curve sits on a staff, one of its three sanctioned appearances in the
 * whole app. There are no sliders, and both honest limits are stated where the
 * claim is made rather than buried in a help screen.
 */
@Composable
fun ToneScreen(
    voicing: Voicing,
    available: Boolean,
    onPick: (Voicing) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        DetailHeader("Tone", onBack)
        Text(
            "Voiced for instruments and rooms.",
            style = type.voice,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 11.dp),
        )

        // The grid makes the in-use line a section head rather than a caption,
        // so the staff runs from it out to the margin and the curve below sits
        // under its own heading.
        Text(
            "${voicing.label}, in use".uppercase(),
            // A plate label, not a section head: the tone curve below draws the
            // screen's one real staff, and two staves of different gauge nine
            // dp apart read as a rendering error.
            style = type.plate,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )

        ToneCurve(voicing, Modifier.padding(top = 9.dp))

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("40Hz", "160", "800", "3k", "12k").forEach {
                Text(it, style = type.plate, color = colors.tertiaryText)
            }
        }

        // A rule above the first row as well as below each one, so the group
        // is closed at both ends the way the grid draws it.
        Box(Modifier.height(6.dp))
        Hairline()

        Voicing.entries.forEach { option ->
            VoicingRow(
                voicing = option,
                selected = option == voicing,
                onClick = { onPick(option) },
            )
        }

        // Both honest limits, at the moment the claim is made.
        Text(
            "Five voicings, all gentle, all cuts rather than boosts, so nothing clips. There is no " +
                "bass boost, no loudness, and no sliders. That is a decision, not an omission.",
            style = type.body,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 13.dp),
        )
        Text(
            "Anything but As Recorded turns off audio offload, which costs a little battery. Your " +
                "phone's own processing sits outside Meedwell and cannot be switched off from here.",
            style = type.body,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 9.dp),
        )
        if (!available) {
            Text(
                "This phone did not offer an equalizer, so Tone has no effect here. Everything plays " +
                    "as recorded.",
                style = type.body,
                color = colors.primaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Box(Modifier.height(40.dp))
    }
}

/**
 * The curve, drawn on a five-line staff.
 *
 * Geometry straight off the grid, which draws the staff at y = 12, 26, 40, 54
 * and 68, so the lines are 14 apart with 12 of clearance above the top one.
 *
 * **Zero sits on the middle line**, which is the heavier one, so a flat voicing
 * lies exactly along it and every cut reads as a dip below the centre. That is
 * the whole reason the staff is here rather than a plain box: it gives the curve
 * a zero to be measured against.
 *
 * The scale is set so the deepest cut any voicing is allowed, 3 dB, lands
 * exactly on the bottom line. Two consequences, both deliberate:
 *
 *  - The curve can never leave the staff, so it never needs clamping.
 *  - **The upper half stays empty on every voicing, forever.** That empty half
 *    is the drawing saying what the copy underneath says: there is headroom up
 *    there and nothing in this app ever uses it. The grid's own sketch runs the
 *    curve above the centre, which contradicts its caption; drawing a boost that
 *    `Voicing` cannot produce would be the one dishonest pixel on the screen.
 */
@Composable
private fun ToneCurve(voicing: Voicing, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    Canvas(
        modifier
            .fillMaxWidth()
            .height(CURVE_HEIGHT)
            .semantics { contentDescription = describe(voicing) }
    ) {
        val line = 1.dp.toPx()
        val top = 12.dp.toPx()
        val gap = 14.dp.toPx()
        repeat(5) { index ->
            val y = top + index * gap
            drawLine(
                color = if (index == 2) colors.hairline2 else colors.hairline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = line,
            )
        }

        // Zero on the middle line, and the 3 dB floor on the bottom one.
        val zeroY = top + 2 * gap
        val perDb = (2 * gap) / 3f

        val path = Path()
        val steps = 96
        for (step in 0..steps) {
            val t = step / steps.toFloat()
            // Log spaced across the same range the labels name, so the shape
            // matches where the labels say the frequencies are.
            val hz = (40.0 * Math.pow(12_000.0 / 40.0, t.toDouble())).toInt()
            val db = Voicing.gainAt(voicing.curve, hz)
            val x = size.width * t
            val y = zeroY - (db * perDb).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = colors.moss,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** 12 of clearance, four 14dp gaps, and a little air under the bottom line. */
private val CURVE_HEIGHT = 12.dp + 14.dp * 4 + 4.dp

@Composable
private fun VoicingRow(voicing: Voicing, selected: Boolean, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                // The grid's 11dp padding on two lines of text already clears
                // 48dp, so the minimum only ever bites on a one-line row.
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 11.dp)
                .semantics {
                    contentDescription = "${voicing.label}. ${voicing.note}"
                    this.selected = selected
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    voicing.label,
                    style = type.rowTitle,
                    color = if (selected) colors.mossInk else colors.primaryText,
                )
                Text(
                    voicing.note,
                    style = type.rowSub,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (selected) {
                MeedwellIcon(MeedwellIcons.Check, size = 15.dp, tint = colors.mossInk)
            }
        }
        Hairline()
    }
}

/** The curve in words, since a line on a staff reads out as nothing. */
private fun describe(voicing: Voicing): String {
    if (voicing.isFlat) return "A flat curve. Nothing is applied."
    val cuts = Voicing.FREQUENCIES.zip(voicing.curve)
        .filter { it.second < 0 }
        .joinToString(", ") { "${"%.1f".format(-it.second)} dB off at ${it.first} hertz" }
    return "The curve for ${voicing.label}: $cuts. Nothing is boosted."
}
