// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Text-field colours for the active scheme.
 *
 * Compose's own defaults assume a light container and a dark-on-light field,
 * which in the dark scheme rendered the input text invisible-blue against a
 * near-black box. Centralising it here means every field in the app picks up
 * both schemes at once, and a future token change lands in one place.
 */
@Composable
fun fastpayTextFieldColors(
    isError: Boolean = false,
    focusedContainer: Color = Color.Transparent,
    unfocusedContainer: Color = Color.Transparent
): TextFieldColors {
    val colors = FastpayTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        disabledTextColor = colors.textDisabled,
        errorTextColor = colors.statusError,
        focusedBorderColor = if (isError) colors.statusError else colors.accent,
        unfocusedBorderColor = if (isError) colors.statusError else colors.outline,
        errorBorderColor = colors.statusError,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textSecondary,
        errorLabelColor = colors.statusError,
        cursorColor = colors.accent,
        focusedContainerColor = focusedContainer,
        unfocusedContainerColor = unfocusedContainer,
        disabledContainerColor = unfocusedContainer,
        errorContainerColor = unfocusedContainer
    )
}
