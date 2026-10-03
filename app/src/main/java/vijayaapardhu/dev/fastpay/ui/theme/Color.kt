// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Flowpay design tokens — the semantic ramp, in two schemes.
//
// Every Compose color comes from here; inline Color(0x…) literals anywhere
// else are a CI failure (see the palette-gate step in build.yml). This file
// is the only place raw hex belongs.
//
// The ramp consolidates the near-duplicate greys that had accreted across
// screens (0x1E1E1E vs 0x1A1A1A, 0x8A8A8A vs 0x888888, …) into one value per
// *visual role*, so "card surface" or "secondary text" changes in exactly one
// place.
//
// Roles are named by what they DO, never by which scheme they came from, so
// a screen reads identically in both schemes: `colors.surface` is "the card"
// whether that card is near-black or near-white.
//
// Scheme selection lives in Theme.kt (FastpayTheme reads the saved
// preference); screens never branch on dark-vs-light themselves — they ask
// for a role and get the right value. That is what lets a single screen file
// serve both schemes with no `if (isSystemInDarkTheme())` branching.
// ─────────────────────────────────────────────────────────────────────────

/**
 * The complete semantic color ramp for one scheme.
 *
 * Read it through [FastpayTheme.colors]; screens should not reference the
 * `Dark…`/`Light…` instances below directly.
 */
@Immutable
data class FastpayColors(
    // ── Surfaces ──────────────────────────────────────────────────────────
    /** Page background, behind cards and lists. */
    val background: Color,
    /** Cards and list rows. */
    val surface: Color,
    /** Chips, avatars, input fields — one step up from [surface]. */
    val surfaceElevated: Color,
    /** Dialogs and sheets. */
    val surfaceOverlay: Color,
    /** Inset wells: progress tracks, code fields, empty-state wells. */
    val surfaceSunken: Color,
    /** A translucent wash of [surfaceElevated], for hover/pressed fills. */
    val surfaceHighlight: Color,

    // ── Lines ─────────────────────────────────────────────────────────────
    /** Hairline dividers and resting input borders. */
    val outline: Color,
    /** Stronger borders: focus rings, outlined buttons. */
    val outlineStrong: Color,

    // ── Text ──────────────────────────────────────────────────────────────
    /** Headings, amounts, primary labels. */
    val textPrimary: Color,
    /** Body copy, captions, timestamps. */
    val textSecondary: Color,
    /** Placeholders, hints, disabled labels. */
    val textTertiary: Color,
    /** Disabled content. */
    val textDisabled: Color,
    /** Anything drawn on top of [accent] / the accent gradient. */
    val onAccent: Color,

    // ── Accent ────────────────────────────────────────────────────────────
    /** Interactive accent: links, selected states, primary buttons. */
    val accent: Color,
    /** Lighter accent for hover/emphasis on dark surfaces. */
    val accentLight: Color,
    /** Deeper accent for pressed states and gradient tails. */
    val accentDark: Color,
    /** Head of the header / primary-button gradient. */
    val accentGradientStart: Color,
    /** Tail of the header / primary-button gradient. */
    val accentGradientEnd: Color,
    /** Very low-alpha accent wash, for tinted chips and banners. */
    val accentSurface: Color,

    // ── Status ────────────────────────────────────────────────────────────
    /** Bank confirmed. */
    val statusSuccess: Color,
    /** Declined, and destructive actions. */
    val statusError: Color,
    /** Needs the user's attention. */
    val statusWarning: Color,
    /** Outcome unknown. Deliberately neither success-green nor failure-red. */
    val statusNeutral: Color
) {
    /**
     * The legacy accent view, derived rather than stored.
     *
     * Kept so the ~65 existing `LocalFastpayAccentTheme.current.…` call sites
     * keep working while becoming theme-aware for free — AccentTheme.kt reads
     * these, so removing the hardcoded `provides BlueAccentTheme` overrides in
     * each Activity is all it took to make every one of them follow the
     * scheme.
     */
    val accentTheme: FastpayAccentTheme
        get() = FastpayAccentTheme(
            primary = accent,
            primaryDark = accentDark,
            headerGradientStart = accentGradientStart,
            headerGradientEnd = accentGradientEnd,
            accent = accent,
            accentLight = accentLight
        )
}

// ─────────────────────────────────────────────────────────────────────────
// Dark scheme. Near-black page, near-black cards, hairline outlines. The
// original build was dark-only, so these values are recognisably the same
// app with the muddy near-duplicate greys collapsed and the accent moved
// from flat sky-blue to an indigo-violet with more range.
// ─────────────────────────────────────────────────────────────────────────

private object DarkRamp {
    val background = Color(0xFF08080C)
    val surface = Color(0xFF121218)
    val surfaceElevated = Color(0xFF1D1D26)
    val surfaceOverlay = Color(0xFF16161E)
    val surfaceSunken = Color(0xFF0D0D12)
    val surfaceHighlight = Color(0xFF23232E)

    val outline = Color(0xFF2A2A36)
    val outlineStrong = Color(0xFF3D3D4D)

    val textPrimary = Color(0xFFF4F4F7)
    val textSecondary = Color(0xFFA9A9B8)
    val textTertiary = Color(0xFF7A7A8A)
    val textDisabled = Color(0xFF54545F)
    val onAccent = Color(0xFFFFFFFF)

    // The accent is keyed to the logo: amber at the head of the F's upper
    // stroke, red-orange at its tail. Keeping the UI accent and the icon in
    // the same family is the whole point of the mark - an indigo UI behind an
    // orange logo read as two unrelated products.
    val accent = Color(0xFFF4511E)
    val accentLight = Color(0xFFFFA726)
    val accentDark = Color(0xFFC2410C)
    val accentGradientStart = Color(0xFFFFA726)
    val accentGradientEnd = Color(0xFFE64A19)
    val accentSurface = Color(0x1FF4511E)

    val statusSuccess = Color(0xFF3ED67F)
    val statusError = Color(0xFFF4553F)

    // Pushed to yellow rather than orange so a pending/needs-review badge
    // cannot be mistaken for the accent - the two were neighbours before the
    // rebrand.
    val statusWarning = Color(0xFFFBBF24)
    val statusNeutral = Color(0xFF9E9E9E)
}

/** The dark ramp. Flowpay's original look. */
val DarkFastpayColors = FastpayColors(
    background = DarkRamp.background,
    surface = DarkRamp.surface,
    surfaceElevated = DarkRamp.surfaceElevated,
    surfaceOverlay = DarkRamp.surfaceOverlay,
    surfaceSunken = DarkRamp.surfaceSunken,
    surfaceHighlight = DarkRamp.surfaceHighlight,
    outline = DarkRamp.outline,
    outlineStrong = DarkRamp.outlineStrong,
    textPrimary = DarkRamp.textPrimary,
    textSecondary = DarkRamp.textSecondary,
    textTertiary = DarkRamp.textTertiary,
    textDisabled = DarkRamp.textDisabled,
    onAccent = DarkRamp.onAccent,
    accent = DarkRamp.accent,
    accentLight = DarkRamp.accentLight,
    accentDark = DarkRamp.accentDark,
    accentGradientStart = DarkRamp.accentGradientStart,
    accentGradientEnd = DarkRamp.accentGradientEnd,
    accentSurface = DarkRamp.accentSurface,
    statusSuccess = DarkRamp.statusSuccess,
    statusError = DarkRamp.statusError,
    statusWarning = DarkRamp.statusWarning,
    statusNeutral = DarkRamp.statusNeutral
)

// ─────────────────────────────────────────────────────────────────────────
// Light scheme.
//
// This is the scheme the old codebase never really had: it had a "light"
// theme that was a black background with light accents, so it was removed in
// favour of dark-only. This one is built from the ground up — light page,
// white cards, and borders carrying the structure that dark surfaces get
// from luminance alone. Grays are slightly blue-shifted to sit with the
// indigo accent rather than fight it.
// ─────────────────────────────────────────────────────────────────────────

private object LightRamp {
    val background = Color(0xFFF4F5F9)
    val surface = Color(0xFFFFFFFF)
    val surfaceElevated = Color(0xFFEEF0F6)
    val surfaceOverlay = Color(0xFFFFFFFF)
    val surfaceSunken = Color(0xFFE7E9F1)
    val surfaceHighlight = Color(0xFFE3E6F0)

    val outline = Color(0xFFDDE0EA)
    val outlineStrong = Color(0xFFBFC4D4)

    val textPrimary = Color(0xFF14151C)
    val textSecondary = Color(0xFF545A6B)
    val textTertiary = Color(0xFF838A9C)
    val textDisabled = Color(0xFFA8AEBD)
    val onAccent = Color(0xFFFFFFFF)

    // Deepened relative to the dark ramp: on a white card a bright orange
    // fails contrast, and these doubles as link text.
    val accent = Color(0xFFC2410C)
    val accentLight = Color(0xFFF4511E)
    val accentDark = Color(0xFF9A3412)
    val accentGradientStart = Color(0xFFF97316)
    val accentGradientEnd = Color(0xFFDC2626)
    val accentSurface = Color(0x1AC2410C)

    // Status hues are darkened in light so they hold up as small text and
    // as 1dp dots; the dark values are too light to read on white.
    val statusSuccess = Color(0xFF128A4B)
    val statusError = Color(0xFFC62828)
    val statusWarning = Color(0xFFB45309)
    val statusNeutral = Color(0xFF6B7280)
}

/** The light ramp. */
val LightFastpayColors = FastpayColors(
    background = LightRamp.background,
    surface = LightRamp.surface,
    surfaceElevated = LightRamp.surfaceElevated,
    surfaceOverlay = LightRamp.surfaceOverlay,
    surfaceSunken = LightRamp.surfaceSunken,
    surfaceHighlight = LightRamp.surfaceHighlight,
    outline = LightRamp.outline,
    outlineStrong = LightRamp.outlineStrong,
    textPrimary = LightRamp.textPrimary,
    textSecondary = LightRamp.textSecondary,
    textTertiary = LightRamp.textTertiary,
    textDisabled = LightRamp.textDisabled,
    onAccent = LightRamp.onAccent,
    accent = LightRamp.accent,
    accentLight = LightRamp.accentLight,
    accentDark = LightRamp.accentDark,
    accentGradientStart = LightRamp.accentGradientStart,
    accentGradientEnd = LightRamp.accentGradientEnd,
    accentSurface = LightRamp.accentSurface,
    statusSuccess = LightRamp.statusSuccess,
    statusError = LightRamp.statusError,
    statusWarning = LightRamp.statusWarning,
    statusNeutral = LightRamp.statusNeutral
)

/**
 * The ramp in scope. Defaults to dark, which is what the app used before the
 * light scheme existed — so any surface rendered outside [FastpayTheme]
 * (Compose previews, a dialog hosted by a legacy View activity) still looks
 * like the original app rather than flashing the light one.
 */
val LocalFastpayColors = staticCompositionLocalOf { DarkFastpayColors }

// ─────────────────────────────────────────────────────────────────────────
// Status palette. One color per outcome, used identically in the history
// list, detail dialog and result screen so a status never changes meaning
// between screens.
// ─────────────────────────────────────────────────────────────────────────

/**
 * Maps a [vijayaapardhu.dev.fastpay.data.TransactionStatus] string to its display
 * color in the current scheme. Replaces the byte-identical getStatusColor()
 * functions that had been copy-pasted into multiple screens.
 */
@Composable
fun statusColor(status: String): Color {
    val colors = FastpayTheme.colors
    return when (status.uppercase()) {
        "SUCCESS", "SUCCESSFUL", "COMPLETED" -> colors.statusSuccess
        "FAILED", "DECLINED" -> colors.statusError
        "PENDING", "NEEDS_REVIEW" -> colors.statusWarning
        else -> colors.statusNeutral // UNVERIFIED, CANCELLED, unknown
    }
}
