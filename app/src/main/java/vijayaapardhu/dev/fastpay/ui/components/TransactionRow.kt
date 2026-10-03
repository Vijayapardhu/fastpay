// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.data.PaymentDetails
import vijayaapardhu.dev.fastpay.data.PaymentStatus
import vijayaapardhu.dev.fastpay.ui.theme.FastpayMotion
import vijayaapardhu.dev.fastpay.ui.theme.FastpayRadius
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme
import vijayaapardhu.dev.fastpay.utils.CurrencyFormat
import vijayaapardhu.dev.fastpay.utils.formatDate

/**
 * How many recent-payment rows animate in with a stagger. Beyond this the
 * remaining rows appear immediately — a long history would otherwise take a
 * visible moment to finish revealing itself, which reads as lag rather than
 * polish.
 */
private const val MAX_STAGGERED_ROWS = 6

@Composable
fun TransactionItem(payment: PaymentDetails, onClick: () -> Unit) {
    TransactionItem(payment = payment, index = 0, onClick = onClick)
}

/**
 * One row in the recent-payments list.
 *
 * [index] drives the entrance stagger, capped inside the item: a list of forty
 * transactions should not take forty staggered steps to appear, so only the
 * first handful animate and the rest are simply present.
 */
@Composable
fun TransactionItem(payment: PaymentDetails, index: Int, onClick: () -> Unit) {
    val colors = FastpayTheme.colors
    val shape = RoundedCornerShape(FastpayRadius.row)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(payment.id) {
        enter.animateTo(
            targetValue = 1f,
            animationSpec = FastpayMotion.staggered(index.coerceAtMost(MAX_STAGGERED_ROWS))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationY = 16f * (1f - enter.value)
            }
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = payment.recipientName ?: payment.phoneNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = formatDate(payment.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TransactionItemAmount(amount = payment.amount, status = payment.status)
        }
    }
}

@Composable
private fun TransactionItemAmount(amount: Double, status: PaymentStatus) {
    // FAILED and CANCELLED read as "this payment did not go through" — a
    // cross, not the same outgoing arrow a successful payment gets. Every
    // other outcome (COMPLETED, PENDING, NEEDS_REVIEW, UNVERIFIED) keeps the
    // arrow: this row has no separate status chip (unlike transaction
    // history), so the icon is the only signal here.
    val failed = status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED
    val tint = if (failed) FastpayTheme.colors.statusError else FastpayTheme.colors.accent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.amount_rupees, CurrencyFormat.inr(amount)),
            style = MaterialTheme.typography.titleSmall,
            color = FastpayTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = if (failed) Icons.Default.Close else Icons.Default.ArrowOutward,
            contentDescription = stringResource(
                if (failed) R.string.cd_payment_failed else R.string.cd_payment_outgoing
            ),
            modifier = Modifier.size(16.dp),
            tint = tint
        )
    }
}
