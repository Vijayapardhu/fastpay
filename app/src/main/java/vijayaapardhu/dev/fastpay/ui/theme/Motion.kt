// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Motion.
//
// Centralised so timing is a decision made once rather than re-invented per
// screen — the reason the old UI felt inconsistent was less the absence of
// animation than the presence of *some* of it, at four different durations.
//
// Two constraints shaped these values, and both are product requirements
// rather than taste:
//
//  * The app's audience is on entry-level Android hardware, where a long
//    animation is not "less polished" but visibly janky. Nothing here exceeds
//    ~450ms, and the entrance motions are all transform/opacity only, so they
//    stay off the layout path.
//  * Several screens appear over a *dialer* during a payment. Motion that
//    delays comprehension is worse than no motion when someone is trying to
//    read an amount and a phone number mid-call.
// ─────────────────────────────────────────────────────────────────────────

object FastpayMotion {
    /**
     * Standard easing — decelerating, the Material "emphasized" feel.
     *
     * The slightly-under-linear curve is what makes something feel like it
     * "arrives" rather than "stops".
     */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For colour/opacity cross-fades, where easing is imperceptible anyway. */
    val gentle: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Fast, for press feedback. Long enough to be seen, short enough to feel instant. */
    const val PRESS_MILLIS = 120

    /** List/panel entrance. */
    const val ENTER_MILLIS = 320

    /** Exits are quicker than entrances: leaving should not hold the user. */
    const val EXIT_MILLIS = 200

    /** Scheme change (light <-> dark). Slow enough to read as one object. */
    const val SCHEME_MILLIS = 420

    /** Stagger between siblings, so a group reveals in order. */
    const val STAGGER_MILLIS = 45

    fun <T> press(): FiniteAnimationSpec<T> = tween(PRESS_MILLIS, easing = standard)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(ENTER_MILLIS, easing = standard)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(EXIT_MILLIS, easing = gentle)

    fun <T> scheme(): FiniteAnimationSpec<T> = tween(SCHEME_MILLIS, easing = standard)

    /**
     * A spring for anything the user directly manipulates (drag, press).
     *
     * [Spring.DampingRatioMediumBouncy] is deliberately *not* used here: a
     * payment app should not wobble.
     */
    fun <T> spring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Staggered enter for the nth child of a group. */
    fun <T> staggered(index: Int): FiniteAnimationSpec<T> = tween(
        durationMillis = ENTER_MILLIS + (STAGGER_MILLIS * index),
        easing = standard
    )

    /** A cross-fade spec for a colour animating to a new scheme's value. */
    fun color(): FiniteAnimationSpec<Color> = tween(SCHEME_MILLIS, easing = standard)
}

/**
 * Cross-fades the whole ramp when the colour scheme changes.
 *
 * Done once, here, rather than with `animateColorAsState` at each of the ~250
 * call sites — the alternative would have been a per-screen animation that
 * each author had to remember to add, and the screens that forgot would snap
 * while their neighbours glided, which is far worse than no animation at all.
 *
 * Every field is animated, including the status hues, so a theme switch reads
 * as one object transforming rather than a set of unrelated repaints.
 */
@Composable
fun rememberSchemeAnimatedColors(target: FastpayColors): FastpayColors {
    val spec = FastpayMotion.color()

    @Composable
    fun fade(color: Color) = animateColorAsState(targetValue = color, animationSpec = spec).value

    return FastpayColors(
        background = fade(target.background),
        surface = fade(target.surface),
        surfaceElevated = fade(target.surfaceElevated),
        surfaceOverlay = fade(target.surfaceOverlay),
        surfaceSunken = fade(target.surfaceSunken),
        surfaceHighlight = fade(target.surfaceHighlight),
        outline = fade(target.outline),
        outlineStrong = fade(target.outlineStrong),
        textPrimary = fade(target.textPrimary),
        textSecondary = fade(target.textSecondary),
        textTertiary = fade(target.textTertiary),
        textDisabled = fade(target.textDisabled),
        onAccent = fade(target.onAccent),
        accent = fade(target.accent),
        accentLight = fade(target.accentLight),
        accentDark = fade(target.accentDark),
        accentGradientStart = fade(target.accentGradientStart),
        accentGradientEnd = fade(target.accentGradientEnd),
        accentSurface = fade(target.accentSurface),
        statusSuccess = fade(target.statusSuccess),
        statusError = fade(target.statusError),
        statusWarning = fade(target.statusWarning),
        statusNeutral = fade(target.statusNeutral)
    )
}
