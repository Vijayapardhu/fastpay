// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package vijayaapardhu.dev.fastpay.data

/**
 * Which colour scheme the UI uses.
 *
 * Persisted as the [name] of the constant (see [fromStorage]), so reordering
 * this enum can never silently repoint an existing user's saved choice at a
 * different scheme.
 */
enum class ThemeMode {
    /** Follow the OS setting, re-evaluated on every configuration change. */
    SYSTEM,

    /** Always the light scheme. */
    LIGHT,

    /** Always the dark scheme — the app's original, and still the default. */
    DARK;

    companion object {
        /** Unknown/absent stored values fall back to [SYSTEM]. */
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
