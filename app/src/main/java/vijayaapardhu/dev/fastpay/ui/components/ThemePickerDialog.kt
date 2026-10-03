// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.data.ThemeMode
import vijayaapardhu.dev.fastpay.ui.theme.DarkFastpayColors
import vijayaapardhu.dev.fastpay.ui.theme.FastpayRadius
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme
import vijayaapardhu.dev.fastpay.ui.theme.LightFastpayColors

/**
 * Picks the colour scheme.
 *
 * Lives in `ui/components` rather than SettingsActivity because that file is
 * already at detekt's per-file function limit, and because the theme control
 * is reusable — a future in-app entry point should not have to reach into the
 * settings screen to reuse it.
 */
@Composable
fun ThemePickerDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FastpayTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceOverlay,
        title = {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    ThemeOptionRow(
                        mode = mode,
                        isSelected = mode == selected,
                        onClick = { onSelect(mode) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_theme_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }
        }
    )
}

/**
 * One selectable scheme.
 *
 * The indicator is a miniature of the scheme rather than a generic radio dot:
 * the entire point of this setting is which one it looks like, and a dot
 * cannot say that. SYSTEM previews the light surface because that is what most
 * users will see from it.
 */
@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = FastpayTheme.colors
    val shape = RoundedCornerShape(FastpayRadius.control)
    val swatchShape = RoundedCornerShape(5.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(if (isSelected) colors.accentSurface else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 16.dp)
                .clip(swatchShape)
                .background(colors.background)
                .border(
                    width = 1.dp,
                    color = if (isSelected) colors.accent else colors.outline,
                    shape = swatchShape
                )
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when (mode) {
                            ThemeMode.SYSTEM -> colors.surfaceElevated
                            ThemeMode.LIGHT -> LightFastpayColors.surface
                            ThemeMode.DARK -> DarkFastpayColors.background
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(mode.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) colors.accent else colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The Settings row's summary label for a mode. */
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
