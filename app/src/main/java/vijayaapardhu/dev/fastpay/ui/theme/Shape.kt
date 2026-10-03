// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────
// Shape scale.
//
// A five-step scale, so "a card" and "a chip" are recognisably the same
// design language. The point of having it here is that it fixes a real
// inconsistency the old home screen had: Scan QR was a CircleShape while its
// sibling Pay Contact button was a 20dp squircle, side by side. Two peer
// actions, two different shapes. They now both use [FastpayRadius.control].
//
// [FastpayShapes] is the Material 3 scale, for components that ask for
// `MaterialTheme.shapes.…`. [FastpayRadius] is the same scale as plain dp
// values, for the places that build a shape inline.
// ─────────────────────────────────────────────────────────────────────────

/** The Material 3 shape scale, wired into the theme. */
val FastpayShapes = Shapes(
    extraSmall = RoundedCornerShape(FastpayRadius.chip),
    small = RoundedCornerShape(FastpayRadius.control),
    medium = RoundedCornerShape(FastpayRadius.row),
    large = RoundedCornerShape(FastpayRadius.card),
    extraLarge = RoundedCornerShape(FastpayRadius.hero)
)

/**
 * Corner radii by role.
 *
 * Named by function rather than by size, so a screen says `FastpayRadius.card`
 * instead of `RoundedCornerShape(20.dp)` and the two stay in step. Every value
 * here is also a step on the Material scale above, except [pill], which is
 * fully rounded.
 */
object FastpayRadius {
    /** Chips, tags, status pills. */
    val chip: Dp = 8.dp

    /** Inputs, compact buttons, avatars. */
    val control: Dp = 12.dp

    /** List rows, inline panels. */
    val row: Dp = 16.dp

    /** Cards, dialogs, the two primary action buttons. */
    val card: Dp = 20.dp

    /** Full-bleed containers such as the header. */
    val hero: Dp = 28.dp

    /** Fully rounded, for circular chips and avatars. */
    val pill: Dp = 999.dp
}
