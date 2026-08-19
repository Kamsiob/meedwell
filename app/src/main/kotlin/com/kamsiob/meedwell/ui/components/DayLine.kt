package com.kamsiob.meedwell.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The day line, under the Shelf title.
 *
 * A hairline horizon with a **copper sun at the actual time of day**, resting on a
 * row of grooves. It is the one other place copper is allowed, because it is literally a
 * sun.
 *
 * ```
 * .dayline{position:relative;height:26px;}
 * .dayline .ln{...top:19px;height:1.5px;background:var(--hair-2);}
 * .dayline .tk{...top:19px;width:1px;height:5px;}
 * .dayline .sun{...width:12px;height:12px;border-radius:50%;background:var(--copper);}
 * ```
 *
 * **The line is the whole day: midnight at the left, 23:59 at the right.**
 *
 * It was originally drawn dawn to dusk, to match the grid, which puts the sun at
 * 26 percent for 9:41 when measured against a 6am to 9pm span. The trouble only
 * showed up on a real phone in the evening: the mark reached the right-hand end
 * at dusk and then stayed pinned against it for the rest of the night, so at nine
 * o'clock the line already looked like something that had run out with three
 * hours of the day still to go. A day that ends before the day does is worse than
 * a slightly different sun position at 9:41.
 *
 * Dawn and dusk still decide whether the mark is a **sun or a moon**, which is
 * the job they were always best at, and they stay the listener's own to set. The
 * mark is inset by its own radius at each end so that midnight draws a whole
 * glyph rather than half of one hanging off the edge.
 *
 * ## Touch the mark
 *
 * The sun, or the moon after dusk, opens [DayMomentPanel]: the hour, where in
 * the day it falls, and the span the line is drawn across. The mark is a 12dp
 * dot, so the target around it is 44dp and follows it along the line. The line
 * itself stays untappable, because it is a drawing and only the mark on it has
 * anything to say.
 *
 * ## It can also roll
 *
 * Behind [DAY_LINE_GAME], currently **off**. With it on, tilting the phone rolls
 * the sun along the horizon under real gravity, the ticks become **grooves**,
 * and a sun arriving slowly enough drops into one and stays. All of it is kept
 * working; see that flag for why it is switched off and what turning it back on
 * costs, which is nothing but the constant.
 *
 * Whether it rolls or not:
 *
 *  - **It obeys reduce-motion.** With that setting on there is no sensor and no
 *    loop at all.
 *  - **The sensor is registered only while this is on screen**, and only while
 *    the app is resumed.
 *  - **State is only written when the sun actually moves.** At rest the loop
 *    does arithmetic and nothing recomposes.
 */
@Composable
fun DayLine(
    modifier: Modifier = Modifier,
    /** Minutes since midnight. Defaults to now, on the phone's own clock. */
    minuteOfDay: Int = currentMinuteOfDay(),
    /** The listener's own dawn and dusk. */
    span: DaySpan = DaySpan.Default,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(26.dp)) {
    val colors = MeedwellTheme.colors

    // **The sun keeps time.** The minute used to be read once, when the shelf
    // was composed, so the mark froze wherever it stood until the screen was
    // rebuilt. It now wakes at each minute boundary. No animation: it simply
    // is where the clock says, which is the whole claim the line makes.
    var liveMinute by remember { androidx.compose.runtime.mutableIntStateOf(minuteOfDay) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L - (System.currentTimeMillis() % 60_000L))
            liveMinute = currentMinuteOfDay()
        }
    }
    val trueFraction = dayFraction(liveMinute)
    val context = LocalContext.current
    val playful = DAY_LINE_GAME && !MeedwellTheme.reducedMotion

    // Measured first, because both the physics and the drawing need to agree on
    // exactly where the grooves are.
    val grooves = remember(maxWidth) { groovesFor(maxWidth) }

    // A groove is a notch of a fixed physical size, so it is expressed as a
    // fraction of whatever line it happens to be cut into. Holding the fraction
    // constant instead would make every groove wider on a tablet and, with seven
    // of them, leave almost no open line between one mouth and the next.
    val mouth = remember(maxWidth) { (GROOVE_MOUTH / maxWidth).coerceIn(0.02f, 0.09f) }

    // Where the sun actually is, which is the true time until somebody tilts.
    //
    // **Not keyed on the minute.** It was, which meant every tick of the clock
    // threw the sun back to the true time and there was no keeping hold of it
    // for longer than sixty seconds. It resets when the shelf is left and
    // entered again, which is the composable being created afresh, and that is
    // reset enough for a line that has to stay honest about the time of day.
    var sunAt by remember { mutableFloatStateOf(trueFraction) }
    var showMoment by remember { mutableStateOf(false) }
    val night = isNight(liveMinute, span)

    // **The sun warms the paper around it.** A faint copper halo that swells
    // and eases over about ten seconds, far too slow to watch and just enough
    // that the mark reads as a light rather than a dot. It is the one place
    // in the app allowed to glow, because it is the one thing in the app that
    // is literally the sun. Held still under reduced motion, and not drawn at
    // night: moonlight does not warm anything.
    val reducedMotion = MeedwellTheme.reducedMotion
    val breath = if (reducedMotion || night) {
        0.14f
    } else {
        val breathing = rememberInfiniteTransition(label = "sun breath")
        breathing.animateFloat(
            initialValue = 0.09f,
            targetValue = 0.20f,
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "halo",
        ).value
    }
    val tilt = remember { FloatArray(1) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    if (playful) {
        // Registered on resume and dropped on pause, rather than held for the
        // whole time the shelf is composed. Leaving the app and coming back left
        // the listener attached to a stopped activity, so the sun stopped
        // answering the phone until the screen was rebuilt.
        DisposableEffect(lifecycle) {
            val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            // Gravity first: it is the accelerometer already low-passed by the
            // platform, so the sun does not jitter with every footstep. Raw
            // acceleration is the fallback on hardware that has no such sensor.
            val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    tilt[0] = event.values[0]
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            fun attach() {
                if (sensor != null && sensors != null) {
                    sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
            fun detach() = sensors?.unregisterListener(listener)

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> attach()
                    Lifecycle.Event.ON_PAUSE -> detach()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                detach()
            }
        }

        LaunchedEffect(Unit) {
            var velocity = 0f
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                    last = now

                    // **Negated.** A gravity sensor reports the up direction, not
                    // the down one: lying flat it reads +9.81 on z, out of the
                    // screen. So tilting the right edge down puts a negative
                    // value on x, and the sun has to roll the other way from the
                    // reading or it climbs the hill instead of falling down it.
                    // Unnegated, tilting left sent it right.
                    val tiltAccel = -(tilt[0] / SensorManager.GRAVITY_EARTH) * ROLL

                    // Each groove is a dip. Inside its mouth the floor slopes
                    // back toward the middle, so a slow sun is drawn in and a
                    // fast one carries straight through under its own momentum.
                    // That is the whole game, and it falls out of the physics
                    // rather than being special-cased.
                    var grooveAccel = 0f
                    var inGroove = false
                    grooves.forEach { groove ->
                        val offset = sunAt - groove
                        if (abs(offset) < mouth) {
                            grooveAccel -= offset * GROOVE_PULL
                            inGroove = true
                        }
                    }

                    velocity += (tiltAccel + grooveAccel) * dt
                    velocity *= if (inGroove) GROOVE_DRAG else DRAG

                    var next = sunAt + velocity * dt

                    // The ends of the day are walls, and a little lossy.
                    if (next < 0f) { next = 0f; velocity = -velocity * BOUNCE }
                    if (next > 1f) { next = 1f; velocity = -velocity * BOUNCE }

                    // Settled: in a groove, barely moving, **and the phone is
                    // roughly level**.
                    //
                    // That last condition is the one that was missing, and
                    // without it the sun could never leave a groove again. The
                    // snap zeroed the velocity on every frame, while a tilt adds
                    // only about a hundredth per frame, so the speed could never
                    // climb past the threshold that would have stopped the
                    // zeroing. It parked itself permanently on first contact.
                    // Now a sustained tilt keeps the snap off and the sun climbs
                    // out under its own steam.
                    if (abs(tiltAccel) < HOLD_TILT) {
                        grooves.forEach { groove ->
                            if (abs(next - groove) < SNAP && abs(velocity) < SNAP_SPEED) {
                                next = groove
                                velocity = 0f
                            }
                        }
                    }

                    // Only wake the renderer when the sun has genuinely moved.
                    if (abs(next - sunAt) > 0.0004f) sunAt = next
                }
            }
        }
    }

    val fraction = if (playful) sunAt else trueFraction

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(26.dp)
            .semantics { contentDescription = describe(minuteOfDay, span) }
    ) {
        val lineY = 19.dp.toPx()
        val depth = 5.dp.toPx()
        val stroke = 1.5.dp.toPx()
        val mouthPx = size.width * mouth

        // Grooves only exist to be landed in, so with the game off the line goes
        // back to exactly what the grid draws: one rule and three ticks.
        var settle = 0f
        if (playful) {
            // The horizon, drawn as segments between the grooves so each groove
            // is a genuine notch in the line rather than a tick hung under it.
            val edges = grooves.map { it * size.width }
            var cursor = 0f
            val horizon = Path()
            edges.forEach { at ->
                val left = (at - mouthPx).coerceAtLeast(0f)
                val right = (at + mouthPx).coerceAtMost(size.width)
                if (left > cursor) {
                    horizon.moveTo(cursor, lineY)
                    horizon.lineTo(left, lineY)
                }
                // The dip itself.
                horizon.moveTo(left, lineY)
                horizon.quadraticTo(at, lineY + depth * 1.6f, right, lineY)
                cursor = right
            }
            if (cursor < size.width) {
                horizon.moveTo(cursor, lineY)
                horizon.lineTo(size.width, lineY)
            }
            drawPath(
                path = horizon,
                color = colors.hairline2,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // How far into a groove the sun has dropped, so it visibly sits in
            // one rather than hovering over the notch.
            val nearest = grooves.minByOrNull { abs(fraction - it) } ?: 0f
            val closeness = (1f - (abs(fraction - nearest) / mouth)).coerceIn(0f, 1f)
            settle = depth * 1.1f * closeness * closeness
        } else {
            // `.dayline .ln` and three `.tk`, straight off the grid.
            drawLine(
                color = colors.hairline2,
                start = Offset(0f, lineY),
                end = Offset(size.width, lineY),
                strokeWidth = stroke,
            )
            listOf(0f, 0.5f, 1f).forEach { at ->
                val x = (size.width * at).coerceIn(0.5f, size.width - 0.5f)
                drawLine(
                    color = colors.hairline2,
                    start = Offset(x, lineY),
                    end = Offset(x, lineY + depth),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        val radius = 6.dp.toPx()
        // The sun is copper by day. On Lamplight the grid draws it in
        // `--lamp-2` rather than copper, because a copper dot on deep pine
        // reads as a warning light rather than as the sun.
        val markInk = if (colors.isDark) colors.secondaryText else colors.copper
        // Inset by its own radius at each end.
        //
        // The mark's center used to be placed at the raw fraction, which was
        // harmless while the line ran dawn to dusk and the ends were never
        // reached. On a line that runs midnight to midnight they are reached
        // every day: at 00:00 half the sun hung off the left edge and just
        // before midnight half the moon hung off the right.
        val travel = (size.width - radius * 2f).coerceAtLeast(0f)
        val at = Offset(radius + travel * fraction, lineY - radius / 2f + settle)

        if (night) {
            // **The real moon.** The carve used to be a fixed constant, so the
            // shelf showed the same crescent every night of the month. The
            // offset now comes from the actual lunation, computed from the date
            // alone: no location, no network, nothing asked of anybody, and it
            // changes about once a day. It is the detail somebody notices in
            // month three rather than minute three, which is what rewarding
            // sitting with a thing means.
            val phase = moonPhase()
            val illumination = (1.0 - kotlin.math.cos(phase * 2.0 * Math.PI)) / 2.0
            if (illumination < 0.04) {
                // Nearly new: a carve would erase the whole disc, and a mark
                // that vanishes off the line is worse than a simplified one.
                drawCircle(
                    color = markInk,
                    radius = radius,
                    center = at,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
                )
            } else {
                drawIntoCanvas {
                    val layer = Paint()
                    it.saveLayer(
                        androidx.compose.ui.geometry.Rect(
                            at.x - radius * 2, at.y - radius * 2,
                            at.x + radius * 2, at.y + radius * 2,
                        ),
                        layer,
                    )
                    drawCircle(color = markInk, radius = radius, center = at)
                    // The carve crosses the disc as the month runs: clear of it
                    // at full, nearly covering it at new, sides swapping with
                    // the waxing and the waning.
                    val carveX = radius * 1.9f * kotlin.math.cos(phase * 2.0 * Math.PI).toFloat()
                    drawCircle(
                        color = Color.Black,
                        radius = radius * 0.88f,
                        center = Offset(at.x + carveX, at.y - radius * 0.10f),
                        blendMode = BlendMode.Clear,
                    )
                    it.restore()
                }
            }
        } else {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(markInk.copy(alpha = breath), Color.Transparent),
                    center = at,
                    radius = radius * 2.8f,
                ),
                radius = radius * 2.8f,
                center = at,
            )
            drawCircle(color = markInk, radius = radius, center = at)
        }
    }

    // The touch target for the mark.
    //
    // The sun is a 12dp dot on a 26dp line, which is far too small to aim at,
    // so it gets a proper 44dp reach that follows it along the line. Drawn as a
    // sibling of the canvas rather than as a gesture on it, so the target moves
    // with the mark instead of the whole line becoming tappable: the line is a
    // drawing, and only the mark on it has anything to say.
    val sunX = maxWidth * fraction.coerceIn(0f, 1f)
    Box(
        Modifier
            .offset(x = sunX - TARGET / 2)
            .size(TARGET)
            .align(Alignment.CenterStart)
            .clickable(
                role = Role.Button,
                onClick = { showMoment = !showMoment },
            )
            .semantics {
                contentDescription =
                    if (night) "The moon, at ${describe(minuteOfDay, span).lowercase()}. Tap for the hour."
                    else "The sun, at ${describe(minuteOfDay, span).lowercase()}. Tap for the hour."
            }
    )

    if (showMoment) {
        Popup(
            alignment = Alignment.TopStart,
            offset = with(LocalDensity.current) {
                // Centred under the mark, then held inside the line's own width
                // so the panel never hangs off an edge of the screen.
                val half = PANEL_WIDTH / 2
                val x = (sunX - half).coerceIn(0.dp, (maxWidth - PANEL_WIDTH).coerceAtLeast(0.dp))
                IntOffset(x.roundToPx(), (26.dp + 6.dp).roundToPx())
            },
            onDismissRequest = { showMoment = false },
            properties = PopupProperties(focusable = true),
        ) {
            DayMomentPanel(minuteOfDay = minuteOfDay, span = span)
        }
    }
    }
}

/** A finger's worth of reach around a 12dp mark. */
private val TARGET = 44.dp

/** Kept in step with `DayMomentPanel`'s own width. */
private val PANEL_WIDTH = 232.dp

/**
 * Whether the sun rolls.
 *
 * **Off.** The whole apparatus below is kept, working and commented, because the
 * owner may want it back: flip this to true and the grooves, the gravity and the
 * settling all return exactly as they were. Nothing else has to change.
 *
 * It is off because the top of the shelf is where somebody looks to get
 * somewhere, and a toy competing for that attention is a toy in the wrong place.
 * With it off the line goes back to what the grid draws, a plain horizon with
 * three ticks and the sun at the hour, and the eye is left with the switcher and
 * the search pill, which is what is actually wanted up there.
 */
private const val DAY_LINE_GAME = false

/**
 * Where the grooves sit, for a line of a given width.
 *
 * The grid draws three ticks, which is right for the phone it was drawn at and
 * thin on anything wider: three grooves across a tablet leaves long empty runs
 * with nothing to aim at. So the count follows the width, one groove roughly
 * every [GROOVE_SPACING], and is clamped to between three and seven.
 *
 * Three is the floor because that is the grid's own figure and the line still
 * has to read as dawn, midday and dusk. Seven is the ceiling because beyond that
 * the grooves crowd close enough that the sun can no longer cross one without
 * falling into the next, which takes the aim out of it.
 *
 * The ends are always grooves, so the first and last are 0 and 1 exactly and the
 * rest divide the line evenly between them.
 */
private fun groovesFor(width: Dp): List<Float> {
    val gaps = (width / GROOVE_SPACING).roundToInt().coerceIn(2, 6)
    return (0..gaps).map { it.toFloat() / gaps }
}

/** Roughly one groove per this much line. */
private val GROOVE_SPACING = 90.dp

/**
 * How wide a groove's mouth is, each side of centre.
 *
 * Physical rather than proportional. On the phone the grid was drawn at this
 * works out near the 0.055 of the line the game was originally tuned against.
 */
private val GROOVE_MOUTH = 20.dp

/** How hard the groove floor slopes back toward its middle. */
private const val GROOVE_PULL = 5.2f

/** Tilt authority. A comfortable lean should cross the line in about a second. */
private const val ROLL = 1.15f

/** Rolling friction, per frame. Enough that it does not skate forever. */
private const val DRAG = 0.985f

/**
 * A groove is rougher than open line, which is what lets it hold the sun.
 *
 * Tuned so a sun crossing at speed keeps about three quarters of it and rides
 * out the far side. At 0.90 a groove bled half the speed in the seven frames it
 * takes to cross, so almost nothing ever escaped and there was no missing left
 * in the game.
 */
private const val GROOVE_DRAG = 0.955f

/** Ends of the day are walls, and they take some of the energy. */
private const val BOUNCE = 0.35f

private const val SNAP = 0.006f
private const val SNAP_SPEED = 0.05f

/**
 * How level the phone has to be before a groove is allowed to hold the sun.
 *
 * Roughly a gentle lean. Past this the tilt wins and the sun climbs out, which
 * is what makes a groove something to escape as well as something to land.
 */
private const val HOLD_TILT = 0.28f

/**
 * The hours the day line is drawn between.
 *
 * **These are settings, not measurements.** Real sunrise and sunset need a
 * latitude, and Meedwell asks for no location at any point, so the honest thing
 * is to let the listener say where their own day starts and ends. The clock
 * itself still comes from the phone, which means changing timezone moves the sun
 * correctly without the app being told anything at all.
 *
 * A span with dusk at or before dawn cannot be drawn, so it falls back rather
 * than producing a line that runs backwards.
 */
data class DaySpan(
    val dawnMinute: Int = 6 * 60,
    val duskMinute: Int = 21 * 60,
) {
    val usable: DaySpan get() = if (duskMinute > dawnMinute) this else DaySpan()

    companion object {
        val Default = DaySpan()
    }
}

/**
 * Where the mark sits, as a fraction of the line: the whole day, midnight to
 * midnight.
 *
 * **The line is 24 hours, not the waking part of them.** It used to run dawn to
 * dusk, which meant the mark hit the right-hand end at dusk and then stayed
 * pinned there until morning: at nine in the evening the moon was already parked
 * against the edge with three hours of the day left to go, and the line read as
 * something that had finished rather than something still moving. Anybody
 * looking at it late in the evening saw a bar that had run out.
 *
 * So midnight is 0, noon is the middle tick, and 23:59 is 1. The value can never
 * leave the line now, and the mark crosses the whole width over a real day.
 *
 * Dawn and dusk did not stop mattering, they just stopped being the ends. They
 * are what decides whether the mark is drawn as a sun or as a moon, which is the
 * job they are actually good at, and they remain the listener's to set.
 */
fun dayFraction(minuteOfDay: Int): Float =
    (minuteOfDay.toFloat() / MINUTES_IN_DAY).coerceIn(0f, 1f)

/** Midnight to midnight, which is the full width of the line. */
private const val MINUTES_IN_DAY = 24f * 60f

/**
 * Where the moon is in its month, 0 at new through 0.5 at full and back.
 *
 * Anchored to the new moon of 6 January 2000, 18:14 UTC, over the mean synodic
 * month. Accurate to better than a day, which is all a 12dp mark can show.
 */
private fun moonPhase(): Double {
    val anchor = 947182440000L
    val synodicDays = 29.530588853
    val days = (System.currentTimeMillis() - anchor) / 86_400_000.0
    return ((days % synodicDays) + synodicDays) % synodicDays / synodicDays
}

private fun currentMinuteOfDay(): Int = Calendar.getInstance().let {
    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
}

/**
 * The line in words, for a screen reader.
 *
 * A horizon with a dot on it is meaningless read out literally, so this says
 * what it means instead. It reports the **time of day**, never where the sun has
 * been rolled to: the game is a thing to find by looking, and reading out "the
 * sun is at forty percent" to somebody who cannot see it would be noise.
 */
private fun describe(minuteOfDay: Int, span: DaySpan = DaySpan.Default): String =
    partOfDay(minuteOfDay, span)
