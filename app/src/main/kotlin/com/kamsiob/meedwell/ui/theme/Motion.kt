package com.kamsiob.meedwell.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring

/**
 * The app's motion, in one place.
 *
 * `DESIGN.md` section 10 asks for two spring personalities and three durations
 * "used consistently". None of that existed: every duration in the app was a
 * literal written at the call site, which makes consistency unenforceable and is
 * why the player spread's turn ended up fighting itself.
 *
 * **The governing idea is that a page is set, not swapped.** Parts arrive in
 * order, one beat apart, the way type and rules are laid onto a page. A block
 * that slides across is a screen being replaced; parts that arrive in sequence
 * read as something being built, which is what somebody means when they say a
 * transition feels like entering rather than switching.
 *
 * Nothing here bounces except one mark, and that exception is named below.
 */
object Motion {

    /** A control answering a finger. Under this and it is not felt at all. */
    const val quick = 120

    /** One state becoming another. */
    const val turn = 260

    /** The ceiling for any arrival, whole. Nothing may take longer. */
    const val enter = 380

    /** Something leaving, which nobody watches. */
    const val leave = 130

    /** One beat of a page being set. */
    const val beat = 38

    /** The delay for the nth part of an arrival. */
    fun stagger(order: Int): Int = order * beat

    /**
     * Decelerate hard and stop without overshooting: a stroke being laid down.
     * The default for anything arriving.
     */
    val Settle = CubicBezierEasing(0.17f, 0.84f, 0.26f, 1f)

    /** Accelerate away. Exits only. */
    val Leave = FastOutLinearInEasing

    /** A line being ruled across the page. */
    val Rule = FastOutSlowInEasing

    /** Critically damped. Everything that springs, springs like this. */
    val standard = spring<Float>(dampingRatio = 1.0f, stiffness = 700f)

    /**
     * The one spring allowed to overshoot, reserved for the play button.
     *
     * `DESIGN.md` section 10 permits an expressive spring for "a small number of
     * signature moments". The play disc is the only filled shape in a design
     * with no filled shapes, so it is already the exception, and letting the one
     * exception have a little life is consistent rather than indulgent.
     */
    val expressive = spring<Float>(dampingRatio = 0.62f, stiffness = 900f)
}
