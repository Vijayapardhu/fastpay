// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import vijayaapardhu.dev.fastpay.R
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme

@Composable
fun Upi123ProgressDialog(
    isVisible: Boolean,
    showConfigurationOptions: Boolean = false,
    onConfigured: () -> Unit = {},
    onNotConfigured: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Upi123ProgressDialogContent(
                showConfigurationOptions = showConfigurationOptions,
                onConfigured = onConfigured,
                onNotConfigured = onNotConfigured,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun Upi123ProgressDialogContent(
    showConfigurationOptions: Boolean = false,
    onConfigured: () -> Unit = {},
    onNotConfigured: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FastpayTheme.colors.surfaceOverlay),
        border = BorderStroke(1.dp, FastpayTheme.colors.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = FastpayTheme.colors.statusSuccess.copy(alpha = alpha * 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = UpiIcon,
                        contentDescription = stringResource(R.string.upi123_dlg_setup_icon_desc),
                        tint = FastpayTheme.colors.statusSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (showConfigurationOptions) {
                        stringResource(R.string.upi123_dlg_setup_complete_title)
                    } else {
                        stringResource(R.string.upi123_dlg_setting_up_title)
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = FastpayTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (showConfigurationOptions) {
                        stringResource(R.string.upi123_dlg_setup_complete_message)
                    } else {
                        stringResource(R.string.upi123_dlg_ivr_triggered_message)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = FastpayTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (showConfigurationOptions) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onNotConfigured,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FastpayTheme.colors.outline
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.upi123_dlg_not_yet),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FastpayTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = onConfigured,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FastpayTheme.colors.statusSuccess
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.upi123_dlg_yes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FastpayTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = FastpayTheme.colors.statusSuccess,
                        trackColor = FastpayTheme.colors.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.upi123_dlg_configuring),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        color = FastpayTheme.colors.textTertiary.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )

                    // Shortcut for users who already have UPI 123 set up: after a
                    // short delay, offer a way to confirm without waiting for the
                    // whole call flow. This only records the test result — it never
                    // touches the ongoing IVR call.
                    var showAlreadySetUp by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(3000)
                        showAlreadySetUp = true
                    }
                    if (showAlreadySetUp) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onConfigured,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, FastpayTheme.colors.statusSuccess),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = FastpayTheme.colors.statusSuccess
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.upi123_dlg_already_set_up),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.upi123_dlg_close),
                    tint = FastpayTheme.colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// Custom UPI Icon.
//
// The fill is a design token rather than a baked-in constant, so the glyph
// tracks the active scheme (it is green on dark, a darker green on light,
// where the bright value would not hold contrast). That makes the getter a
// composable scope; it is still built once per composition, not per frame.
val UpiIcon: ImageVector
    @Composable
    get() {
        return ImageVector.Builder(
            name = "upi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = androidx.compose.ui.graphics.SolidColor(FastpayTheme.colors.statusSuccess),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
                strokeLineMiter = 1f,
                pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
            ) {
                moveTo(6f, 8f)
                verticalLineTo(16f)
                curveTo(6f, 18.2f, 7.8f, 20f, 10f, 20f)
                horizontalLineTo(14f)
                curveTo(16.2f, 20f, 18f, 18.2f, 18f, 16f)
                verticalLineTo(8f)
                moveTo(8f, 8f)
                verticalLineTo(16f)
                curveTo(8f, 17.1f, 8.9f, 18f, 10f, 18f)
                horizontalLineTo(14f)
                curveTo(15.1f, 18f, 16f, 17.1f, 16f, 16f)
                verticalLineTo(8f)
                moveTo(10f, 12f)
                horizontalLineTo(14f)
                moveTo(10f, 14f)
                horizontalLineTo(14f)
            }
        }.build()
    }
