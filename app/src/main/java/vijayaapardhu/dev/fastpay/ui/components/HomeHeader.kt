// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.ui.theme.FastpayMotion
import vijayaapardhu.dev.fastpay.ui.theme.FastpayRadius
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme

// ─────────────────────────────────────────────────────────────────────────
// Home screen header: the gradient card at the top of the home screen.
//
// Split from HomeComponents.kt (the primary action cards) because that file is
// at detekt's per-file function limit, and because the header and the actions
// change for different reasons.
// ─────────────────────────────────────────────────────────────────────────

/**
 * The gradient header: wordmark, settings, the connected bank, and which
 * payment rails this SIM has actually been verified on.
 *
 * The rail status is new. It was previously only visible as a locked button,
 * so a user could not tell "not set up yet" from "this SIM cannot do it" —
 * which is the single most confusing thing about an app that depends on
 * carrier support it does not control.
 */
@Composable
fun HomeHeader(
    bankName: String,
    isUssdReady: Boolean,
    isUpi123Ready: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FastpayTheme.colors
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, animationSpec = FastpayMotion.enter()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                // Rises and settles, so the header reads as arriving rather
                // than simply appearing.
                translationY = -14f * (1f - enter.value)
            }
            .clip(RoundedCornerShape(FastpayRadius.hero))
            .background(
                Brush.linearGradient(
                    listOf(colors.accentGradientStart, colors.accentGradientEnd)
                )
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            HeaderTopRow(onSettingsClick = onSettingsClick)
            Spacer(modifier = Modifier.height(18.dp))
            HeaderBankRow(bankName = bankName)
            Spacer(modifier = Modifier.height(14.dp))
            HeaderRailStatus(isUssdReady = isUssdReady, isUpi123Ready = isUpi123Ready)
        }
    }
}

/** Wordmark plus the settings affordance. */
@Composable
private fun HeaderTopRow(onSettingsClick: () -> Unit) {
    val colors = FastpayTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderMark()
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onAccent
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onAccent.copy(alpha = 0.82f)
                )
            }
        }

        HeaderSettingsButton(onSettingsClick = onSettingsClick)
    }
}

/**
 * The app's mark in the header.
 *
 * The real logo, not a stand-in glyph — the header sits on the accent
 * gradient, which is keyed to the logo's own orange, so the mark is carried on
 * a light tile. Drawing the mark directly on the gradient would work only for
 * its dark lower stroke; the bright orange top would vanish into it.
 */
@Composable
private fun HeaderMark() {
    Image(
        painter = painterResource(id = R.drawable.ic_fastpay_logo_mark),
        contentDescription = stringResource(R.string.app_name),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(FastpayRadius.control))
            .background(FastpayTheme.colors.onAccent)
            .padding(7.dp)
    )
}

/** The settings affordance in the header's top-right. */
@Composable
private fun HeaderSettingsButton(onSettingsClick: () -> Unit) {
    val colors = FastpayTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(FastpayRadius.control))
            .background(colors.onAccent.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSettingsClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings_title),
            tint = colors.onAccent,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** The connected bank, on a translucent inset so it reads as a distinct layer. */
@Composable
private fun HeaderBankRow(bankName: String) {
    val colors = FastpayTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FastpayRadius.row))
            .background(colors.onAccent.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_connected_bank),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onAccent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = bankName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onAccent,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = colors.onAccent.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Which rails are live. Deliberately not a count: the user needs to know
 * *which* payment methods work, because that decides what they can do.
 */
@Composable
private fun HeaderRailStatus(isUssdReady: Boolean, isUpi123Ready: Boolean) {
    val colors = FastpayTheme.colors
    val labelRes = when {
        isUssdReady && isUpi123Ready -> R.string.home_rails_ready
        isUssdReady -> R.string.home_rails_ussd_only
        isUpi123Ready -> R.string.home_rails_ivr_only
        else -> R.string.home_rails_neither
    }
    val railUp = isUssdReady || isUpi123Ready
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(FastpayRadius.pill))
                .background(
                    if (railUp) {
                        colors.onAccent.copy(alpha = 0.95f)
                    } else {
                        colors.onAccent.copy(alpha = 0.45f)
                    }
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onAccent.copy(alpha = if (railUp) 0.95f else 0.75f)
        )
    }
}
