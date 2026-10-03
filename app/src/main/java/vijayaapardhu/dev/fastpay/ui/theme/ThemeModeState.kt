// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import vijayaapardhu.dev.fastpay.FastpayApplication
import vijayaapardhu.dev.fastpay.data.SettingsRepository.SavedSettings
import vijayaapardhu.dev.fastpay.data.ThemeMode

/**
 * The user's saved colour-scheme choice, observed reactively.
 *
 * Every Activity passes this to [FastpayTheme]. Reading it through the
 * settings repository's StateFlow — rather than each Activity loading the
 * preference itself — is what makes the Settings toggle take effect
 * immediately instead of on the next cold start.
 *
 * Falls back to [ThemeMode.SYSTEM] if the repository is unreachable (Compose
 * preview, or a host that is not FastpayApplication), rather than throwing:
 * a missing theme preference must never be the reason a screen fails to
 * draw.
 */
@Composable
fun rememberAppThemeMode(): ThemeMode {
    val context = LocalContext.current
    val repository = remember(context) {
        FastpayApplication.from(context)?.settingsRepository
    }
    val settings by remember(repository) {
        repository?.settingsFlow ?: MutableStateFlow(SavedSettings())
    }.collectAsState()
    return settings.themeMode
}
