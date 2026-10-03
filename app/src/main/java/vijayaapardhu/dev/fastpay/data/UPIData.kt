// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UPIData(
    var vpa: String,
    val payeeName: String,
    var amount: String,
    val transactionNote: String,
    val currency: String
) : Parcelable
