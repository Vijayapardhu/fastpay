// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes
import vijayaapardhu.dev.fastpay.FastpayApplication
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.data.ThemeMode

/**
 * The scheme that will be in scope for [context] right now.
 *
 * [FastpayTheme] resolves the same thing from the repository's StateFlow, but
 * only once Compose is running. An Activity has to pick its window theme
 * *before* `super.onCreate` finishes, so it needs the answer synchronously —
 * hence this reading the same preference directly.
 *
 * Reads only the already-cached settings, and is safe to call with any
 * Context; falls back to the system setting when the repository is not
 * reachable.
 */
fun resolveThemeMode(context: Context): ThemeMode {
    val saved = (context.applicationContext as? FastpayApplication)
        ?.settingsRepository
        ?.settingsFlow
        ?.value
        ?.themeMode
        ?: ThemeMode.SYSTEM
    return when (saved) {
        ThemeMode.LIGHT -> ThemeMode.LIGHT
        ThemeMode.DARK -> ThemeMode.DARK
        ThemeMode.SYSTEM ->
            if (context.isCurrentlyInDarkMode()) ThemeMode.DARK else ThemeMode.LIGHT
    }
}

/**
 * The non-Composable equivalent of `isSystemInDarkTheme()`.
 *
 * Needed because the launch theme is chosen in onCreate, where there is no
 * composition to ask.
 */
fun Context.isCurrentlyInDarkMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * The window theme matching the resolved scheme.
 *
 * Choosing this per-launch is what stops the app opening on a black window
 * and then fading up to the light surface. Both variants keep the
 * edge-to-edge and cutout configuration identical — only the background
 * colour and the system-bar icon polarity differ.
 */
@StyleRes
fun windowThemeFor(context: Context): Int =
    if (resolveThemeMode(context) == ThemeMode.DARK) {
        R.style.Theme_Fastpay
    } else {
        R.style.Theme_Fastpay_Light
    }
