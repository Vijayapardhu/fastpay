// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The accent-only view of the ramp.
 *
 * Historically this was the *primary* theming mechanism — a data class
 * provided per-Activity through `LocalFastpayAccentTheme`, hardcoded to
 * `BlueAccentTheme` at every call site. That made the accent theme-oblivious:
 * a light scheme would have needed a second instance provided at every one of
 * those sites, and nothing stopped one screen from quietly using blue while
 * its neighbour used something else.
 *
 * The accent now lives inside [FastpayColors] and resolves with the rest of
 * the ramp, so this type is a *view* over it rather than a parallel source of
 * truth. It stays because ~65 call sites read these fields, and keeping them
 * working meant the accent became scheme-aware across the whole app for the
 * cost of deleting the per-Activity `provides` overrides.
 */
data class FastpayAccentTheme(
    val primary: Color,
    val primaryDark: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val accent: Color,
    val accentLight: Color
)

/**
 * Defaults to the dark accent, and is overridden for the whole tree by
 * [FastpayTheme].
 *
 * The default cannot be `LocalFastpayColors.current.accentTheme`: the lambda
 * `compositionLocalOf` takes is not a composable scope, so it cannot read
 * another CompositionLocal. Hence the explicit `provides` in FastpayTheme,
 * which is also what makes this local track the scheme.
 */
val LocalFastpayAccentTheme = staticCompositionLocalOf {
    DarkFastpayColors.accentTheme
}

/** The accent view of the scheme currently in scope. */
val currentAccentTheme: FastpayAccentTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalFastpayAccentTheme.current
