// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.ui.theme.FastpayMotion
import vijayaapardhu.dev.fastpay.ui.theme.FastpayRadius
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme

// ─────────────────────────────────────────────────────────────────────────
// Home screen components: the gradient header and the two primary actions.
//
// The actions used to be two bare icon buttons — Scan QR a CircleShape, Pay
// Contact a 20dp squircle — separated by an "OR" divider. Two peer actions,
// two shapes and a divider implying a choice between them, for what is really
// just "pick how you want to pay".
//
// They are now two full-width cards of identical shape: larger touch targets,
// which matters on the entry-level hardware this app targets; a title and a
// subtitle, so the rail each one dials is stated rather than implied; and no
// divider, because nothing is being disambiguated.
// ─────────────────────────────────────────────────────────────────────────

/**
 * The two primary payment actions.
 *
 * Each is locked until its own connectivity test has passed: Scan QR dials
 * *99#, Pay Contact dials the UPI 123 IVR. A locked card stays tappable on
 * purpose — it routes to the test screen, which is what the user wants when
 * they see it.
 */
@Composable
fun PaymentActionCards(
    scanQr: PaymentActionSpec,
    payContact: PaymentActionSpec,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaymentActionCard(
            spec = scanQr,
            onClick = scanQr.onClick,
            enabled = !isScanning,
            index = 0
        )
        PaymentActionCard(
            spec = payContact,
            onClick = payContact.onClick,
            enabled = true,
            index = 1
        )
    }
}

/**
 * One action's content and behaviour, resolved by the caller.
 *
 * Bundling these keeps [PaymentActionCard] under detekt's parameter limit
 * without weakening it to a state-holder, and keeps the lock/unlock string
 * selection next to the card that shows it.
 */
data class PaymentActionSpec(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val contentDescription: String
)

/**
 * The Scan QR card's content, given whether the *99# rail has been verified
 * and whether the scanner is currently launching.
 */
@Composable
fun scanQrActionSpec(
    isUssdReady: Boolean,
    isScanning: Boolean,
    onClick: () -> Unit
): PaymentActionSpec = PaymentActionSpec(
    icon = if (isUssdReady) Icons.Default.QrCodeScanner else Icons.Default.Lock,
    title = when {
        !isUssdReady -> stringResource(R.string.home_setup_ussd)
        isScanning -> stringResource(R.string.home_scan_opening)
        else -> stringResource(R.string.home_scan_qr)
    },
    subtitle = stringResource(
        if (isUssdReady) R.string.home_scan_qr_sub else R.string.home_scan_qr_sub_locked
    ),
    onClick = onClick,
    contentDescription = stringResource(
        if (isUssdReady) R.string.home_scan_qr else R.string.home_setup_ussd
    )
)

/** The Pay Contact card's content, given whether the 123Pay IVR is verified. */
@Composable
fun payContactActionSpec(
    isUpi123Ready: Boolean,
    onClick: () -> Unit
): PaymentActionSpec = PaymentActionSpec(
    icon = if (isUpi123Ready) Icons.Default.Person else Icons.Default.Lock,
    title = stringResource(
        if (isUpi123Ready) R.string.home_pay_contact else R.string.home_setup_upi123
    ),
    subtitle = stringResource(
        if (isUpi123Ready) R.string.home_pay_contact_sub else R.string.home_pay_contact_sub_locked
    ),
    onClick = onClick,
    contentDescription = stringResource(
        if (isUpi123Ready) R.string.home_pay_contact else R.string.home_setup_upi123
    )
)

/**
 * One primary action: icon tile, title, subtitle, chevron.
 *
 * Locked cards keep the same geometry as unlocked ones and differ only in
 * colour. Resizing or restyling a disabled control is what made the old pair
 * read as two unrelated controls rather than one pattern.
 */
@Composable
private fun PaymentActionCard(
    spec: PaymentActionSpec,
    onClick: () -> Unit,
    enabled: Boolean,
    index: Int
) {
    val colors = FastpayTheme.colors
    val shape = RoundedCornerShape(FastpayRadius.card)

    // Press feedback: a small scale dip on touch-down. The dip is what makes
    // the tap feel acknowledged on hardware where the ripple is easy to miss,
    // which matters on the low-end devices this app targets. `pressed` comes
    // from the interaction source so it tracks the finger rather than waiting
    // for the click to resolve.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = FastpayMotion.press(),
        label = "actionCardPress"
    )

    // Entrance: fade + a short rise, staggered so the pair reads top-down.
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, animationSpec = FastpayMotion.staggered(index))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = enter.value
                translationY = 22f * (1f - enter.value)
            }
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics { contentDescription = spec.contentDescription }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionIconTile(icon = spec.icon, enabled = enabled)
        Spacer(modifier = Modifier.width(14.dp))
        ActionCardLabels(spec = spec, enabled = enabled)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The title/subtitle pair in an action card.
 *
 * Scoped to [RowScope] because it uses `Modifier.weight` to absorb the row's
 * remaining width — `weight` only resolves inside a Row/Column scope, so
 * extracting this helper without the receiver scope would have silently
 * changed the layout.
 */
@Composable
private fun RowScope.ActionCardLabels(spec: PaymentActionSpec, enabled: Boolean) {
    val colors = FastpayTheme.colors
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = spec.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.textPrimary else colors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = spec.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            maxLines = 1
        )
    }
}

/** The accent-filled icon square at the left of an action card. */
@Composable
private fun ActionIconTile(icon: ImageVector, enabled: Boolean) {
    val colors = FastpayTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(FastpayRadius.control))
            .background(
                if (enabled) {
                    Brush.linearGradient(
                        listOf(colors.accentGradientStart, colors.accentGradientEnd)
                    )
                } else {
                    Brush.linearGradient(listOf(colors.surfaceElevated, colors.surfaceElevated))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.onAccent else colors.textDisabled,
            modifier = Modifier.size(22.dp)
        )
    }
}
