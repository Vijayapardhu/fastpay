// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a transaction timestamp for display, e.g. `04 Mar, 14:07`.
 *
 * Lives here rather than on an Activity because three screens show the same
 * string — the home screen's recent list, the full history, and the payment
 * result screen — and they had each grown their own copy. The locale is pinned
 * to en-IN so the month abbreviation and the numeric layout do not follow the
 * user's device locale; a payment record is a financial artefact, and "Mar"
 * should not become "Mär" on a German handset when the bank SMS said one
 * thing and the statement says another.
 *
 * Not thread-safe to share a single [SimpleDateFormat]; a new one per call is
 * cheap next to the disk read that renders a transaction row.
 */
fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale("en", "IN")).format(Date(timestamp))
