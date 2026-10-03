// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import vijayaapardhu.dev.fastpay.data.ThemeMode
import vijayaapardhu.dev.fastpay.utils.findComponentActivity

/**
 * Entry point to the token system.
 *
 * Screens read `FastpayTheme.colors.surface` / `.textPrimary` / `.accent`
 * rather than a scheme constant, so the same screen file serves both colour
 * schemes without any `if (isSystemInDarkTheme())` branching.
 */
object FastpayTheme {
    /** The semantic ramp for the scheme currently in scope. */
    val colors: FastpayColors
        @Composable get() = LocalFastpayColors.current
}

/**
 * Resolves the ramp for the requested [themeMode].
 *
 * [ThemeMode.SYSTEM] defers to the OS. Note that with SYSTEM the app can
 * change scheme while it is running (on the OS's dark-mode toggle), so
 * everything downstream of here must be recomposition-safe rather than
 * captured once in a `remember`.
 */
@Composable
private fun colorsFor(themeMode: ThemeMode): FastpayColors = when (themeMode) {
    ThemeMode.LIGHT -> LightFastpayColors
    ThemeMode.DARK -> DarkFastpayColors
    ThemeMode.SYSTEM ->
        if (isSystemInDarkTheme()) DarkFastpayColors else LightFastpayColors
}

/**
 * Material 3 scheme, derived from the semantic ramp.
 *
 * Built rather than hand-written so the two can never disagree: Material
 * components (AlertDialog, OutlinedTextField, Switch, …) read `colorScheme`,
 * and any role we forgot to expose as a token would otherwise quietly fall
 * back to M3's purple defaults.
 *
 * [isDark] is passed explicitly rather than inferred from [colors]: the ramp is
 * rebuilt every frame while a scheme change interpolates, so identity
 * comparison against DarkFastpayColors would never match and the app would
 * always take the light branch. The *values* come from the animated ramp, so
 * Material components cross-fade along with everything else; only the
 * light-vs-dark branch, which drives M3's derived on-colours, uses the
 * resolved scheme.
 */
private fun materialSchemeFor(colors: FastpayColors, isDark: Boolean) =
    if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.surfaceElevated,
            onPrimaryContainer = colors.textPrimary,
            inversePrimary = colors.accentDark,
            secondary = colors.accentLight,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surfaceElevated,
            surfaceContainerHigh = colors.surfaceHighlight,
            outline = colors.outline,
            outlineVariant = colors.outlineStrong,
            error = colors.statusError,
            onError = colors.onAccent,
            scrim = colors.background
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSurface,
            onPrimaryContainer = colors.accentDark,
            inversePrimary = colors.accent,
            secondary = colors.accentDark,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.surfaceElevated,
            surfaceContainerHigh = colors.surfaceHighlight,
            outline = colors.outline,
            outlineVariant = colors.outlineStrong,
            error = colors.statusError,
            onError = colors.onAccent,
            scrim = colors.background
        )
    }

/**
 * Applies the Flowpay theme: the semantic ramp, a matching Material 3 scheme,
 * the type scale, and system bars that invert to match.
 *
 * Dynamic (Material You) color is intentionally NOT used. On Android 12+ it
 * silently replaced the brand palette with wallpaper-derived colors — which
 * matters more than usual here, because the header gradient and the action
 * buttons are the app's identity and the accent has to hold its contrast
 * against both schemes (see the note on [LightRamp]'s accent).
 */
@Composable
fun FastpayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val resolved = colorsFor(themeMode)
    // Cross-fade the ramp so a theme switch glides rather than snapping. The
    // `isDark` flag below is read from the *resolved* scheme, not the animated
    // colours, so system-bar polarity flips at the right moment instead of
    // waiting on 420ms of colour interpolation.
    val colors = rememberSchemeAnimatedColors(resolved)
    val view = LocalView.current
    val isDark = resolved === DarkFastpayColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findComponentActivity()?.window ?: return@SideEffect
            val bars = colors.background.toArgb()
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = bars
                window.navigationBarColor = bars
            }
            // The icon colour has to invert with the scheme, or the clock and
            // battery render black-on-black in the light theme. This is the
            // one thing that cannot be left to a resource qualifier, because
            // the scheme is a user setting rather than a device setting.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        // Both locals are provided together so the legacy accent view can never
        // disagree with the ramp it is derived from.
        LocalFastpayColors provides colors,
        LocalFastpayAccentTheme provides colors.accentTheme
    ) {
        MaterialTheme(
            colorScheme = materialSchemeFor(colors, isDark),
            typography = Typography,
            shapes = FastpayShapes,
            content = content
        )
    }
}
