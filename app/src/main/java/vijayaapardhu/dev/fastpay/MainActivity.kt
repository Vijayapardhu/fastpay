// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay
//
// Home screen: the entry point to both payment rails (Scan QR and Pay
// Contact), recent payments, and settings. UI only — the transfer
// orchestration and permission gating live in MainActivityHelper.

package vijayaapardhu.dev.fastpay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vijayaapardhu.dev.fastpay.constants.AppConstants
import vijayaapardhu.dev.fastpay.constants.PermissionConstants
import vijayaapardhu.dev.fastpay.data.PaymentDetails
import vijayaapardhu.dev.fastpay.data.TestResultsManager
import vijayaapardhu.dev.fastpay.data.ThemeMode
import vijayaapardhu.dev.fastpay.helpers.MainActivityHelper
import vijayaapardhu.dev.fastpay.managers.PermissionManager
import vijayaapardhu.dev.fastpay.payment.Upi123CallStringBuilder
import vijayaapardhu.dev.fastpay.payment.messageFor
import vijayaapardhu.dev.fastpay.ui.activities.SettingsActivity
import vijayaapardhu.dev.fastpay.ui.activities.TransactionHistoryActivity
import vijayaapardhu.dev.fastpay.ui.components.HomeHeader
import vijayaapardhu.dev.fastpay.ui.components.PaymentActionCards
import vijayaapardhu.dev.fastpay.ui.components.TransactionDetailDialog
import vijayaapardhu.dev.fastpay.ui.components.TransactionItem
import vijayaapardhu.dev.fastpay.ui.components.payContactActionSpec
import vijayaapardhu.dev.fastpay.ui.components.scanQrActionSpec
import vijayaapardhu.dev.fastpay.ui.dialogs.ContactPickerDialog
import vijayaapardhu.dev.fastpay.ui.theme.FastpayRadius
import vijayaapardhu.dev.fastpay.ui.theme.FastpayTheme
import vijayaapardhu.dev.fastpay.ui.theme.fastpayTextFieldColors
import vijayaapardhu.dev.fastpay.ui.theme.rememberAppThemeMode
import vijayaapardhu.dev.fastpay.ui.theme.resolveThemeMode
import vijayaapardhu.dev.fastpay.ui.theme.windowThemeFor
import vijayaapardhu.dev.fastpay.utils.findComponentActivity
import vijayaapardhu.dev.fastpay.viewmodel.MainUiEvent
import vijayaapardhu.dev.fastpay.viewmodel.MainViewModel
import vijayaapardhu.dev.fastpay.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "Fastpay"
    }

    // Helper for all business logic
    private lateinit var helper: MainActivityHelper

    // Shared with MainScreen (same instance via Compose viewModel()); carries
    // one-shot Activity -> Compose events, replacing the old static callbacks.
    private val mainViewModel: MainViewModel by viewModels()

    // Launches QRScannerActivity and, on return, un-sticks the QR button's
    // "Opening..." state via a QrScannerClosed event.
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        mainViewModel.onQrScannerClosed()
    }

    // Launches the system "draw over other apps" settings screen; on return,
    // re-checks the permission and reports the outcome via toast (there is
    // no reliable resultCode for this settings screen, so re-checking is
    // the only correct way to know what happened).
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = PermissionManager.canDrawOverlays(this)
        val message = if (granted) {
            "Overlay permission granted. You can now proceed with the transfer."
        } else {
            "Overlay permission is required for payment protection. Please enable it in Settings."
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Phone-call permission group, requested before dialing or QR scanning.
    // There is no auto-retry: the user re-taps the action once granted.
    private val phonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (!granted) {
            Toast.makeText(this, R.string.error_permissions_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chosen from the saved scheme, not hardcoded: the window background has
        // to already match the scheme Compose is about to draw, or the app
        // opens on black and fades to the light surface.
        setTheme(windowThemeFor(this))

        // Draw edge-to-edge so Compose's statusBarsPadding()/navigationBarsPadding()
        // are the single source of inset padding. The theme previously also set
        // android:fitsSystemWindows=true, which made the decor pad the content as
        // well — a double inset that, depending on inset-dispatch timing, showed
        // intermittent black bars at the top and bottom.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // System bars coloured to the scheme from the first frame. FastpayTheme
        // re-applies these in a SideEffect once the composition exists, which
        // also covers a scheme change while the app is backgrounded.
        applyWindowScheme()
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.decorView.post { applyWindowScheme() }

        // Initialize helper with UI callbacks (matches the 6-method UICallback)
        helper = MainActivityHelper(
            this,
            object : MainActivityHelper.UICallback {
                override fun showToast(message: String) {
                    runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
                }

                override fun updatePaymentState(paymentState: vijayaapardhu.dev.fastpay.states.PaymentState) {
                    Log.d(TAG, "Payment state updated: ${paymentState::class.simpleName}")
                }

                override fun navigateToSetup() {
                    startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                    finish()
                }

                override fun navigateToTestConfiguration() {
                    startActivity(Intent(this@MainActivity, TestConfigurationActivity::class.java))
                    finish()
                }

                override fun finishActivity() {
                    finish()
                }

                override fun showOverlayPermissionExplanation() {
                    mainViewModel.onOverlayPermissionNeeded()
                }

                override fun launchQRScanner(intent: Intent) {
                    qrScannerLauncher.launch(intent)
                }

                override fun requestPhonePermissions() {
                    phonePermissionLauncher.launch(PermissionConstants.PHONE_PERMISSIONS)
                }
            }
        )

        helper.initialize()

        // The launch gate must know which screen to show before rendering, so
        // these two SharedPreferences flags are read synchronously. Reading
        // them faults the prefs file in once (a small, one-time disk read);
        // annotate it as permitted so the debug StrictMode tripwire stays
        // sharp for genuinely unexpected main-thread disk I/O instead of
        // crying wolf on this known-safe read every launch.
        val setupCompleted: Boolean
        val testCompleted: Boolean
        val oldPolicy = android.os.StrictMode.allowThreadDiskReads()
        try {
            setupCompleted = helper.isSetupCompleted()
            testCompleted = helper.isTestCompleted()
        } finally {
            android.os.StrictMode.setThreadPolicy(oldPolicy)
        }

        if (!setupCompleted) {
            helper.navigateToSetup()
            return
        }
        if (!testCompleted) {
            helper.navigateToTestConfiguration()
            return
        }

        setContent {
            FastpayTheme(themeMode = rememberAppThemeMode()) {
                MainScreen(
                    onInitiateTransfer = { phoneNumber, amount ->
                        helper.initiateTransfer(phoneNumber, amount)
                    },
                    onQRScanClick = {
                        helper.startQRScanning()
                    },
                    onRequestOverlayPermission = {
                        PermissionManager(this).overlayPermissionSettingsIntent()?.let {
                            overlayPermissionLauncher.launch(it)
                        }
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        helper.onPause()
    }

    override fun onResume() {
        super.onResume()
        helper.onResume()
        applyWindowScheme()
    }

    /**
     * Colours the system bars and flips their icon polarity to match the
     * resolved scheme.
     *
     * Re-applied on every resume because the user can change the theme in
     * Settings and come straight back, and because a SYSTEM-mode app picks up
     * the OS's dark-mode change while it is backgrounded.
     */
    private fun applyWindowScheme() {
        val isDark = resolveThemeMode(this) == ThemeMode.DARK
        val bars = ContextCompat.getColor(
            this,
            if (isDark) R.color.window_background_dark else R.color.window_background_light
        )
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = bars
            window.navigationBarColor = bars
        }
        // WindowInsetsControllerCompat routes through WindowInsetsController on
        // API 30+ and the legacy systemUiVisibility flags on API 29, replacing
        // the deprecated direct flag manipulation.
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    override fun onStop() {
        super.onStop()
        helper.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        helper.onDestroy()
    }
}

// Payment actions - QR scan + Pay Contact.
//
// Both are locked until their own connectivity test has passed: Scan QR dials
// *99#, Pay Contact dials the UPI 123 IVR. The cards themselves live in
// HomeComponents.kt; this wrapper only exists to keep the screen's call site
// named after the feature rather than the widget.
@Composable
fun PaymentActionButtons(
    onQRScanClick: () -> Unit,
    onPayContactClick: () -> Unit,
    isUpi123Ready: Boolean,
    isUssdReady: Boolean,
    isScanning: Boolean
) {
    PaymentActionCards(
        scanQr = scanQrActionSpec(
            isUssdReady = isUssdReady,
            isScanning = isScanning,
            onClick = onQRScanClick
        ),
        payContact = payContactActionSpec(
            isUpi123Ready = isUpi123Ready,
            onClick = onPayContactClick
        ),
        isScanning = isScanning,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onInitiateTransfer: (String, String) -> Unit,
    onQRScanClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var savedBank by remember {
        mutableStateOf(sharedPreferences.getString(AppConstants.KEY_SELECTED_BANK, "hdfc") ?: "hdfc")
    }

    // Pay Contact dials the UPI 123 IVR, so it stays inactive until the
    // UPI 123 configuration test has passed (re-checked on every resume so
    // completing the test activates it immediately).
    val testResultsManager = remember { TestResultsManager(context) }
    var isUpi123Ready by remember {
        mutableStateOf(testResultsManager.getTestResults()?.upi123Enabled == true)
    }

    // Scan QR dials *99#, so it stays locked until the *99# test has passed —
    // the same gate Pay Contact has always had for the UPI 123 IVR. Without
    // it the scanner opened, read a QR, and only then dialled a rail this SIM
    // may not support.
    var isUssdReady by remember {
        mutableStateOf(testResultsManager.getTestResults()?.ussdEnabled == true)
    }

    LaunchedEffect(lifecycle) {
        snapshotFlow { lifecycle.currentState }.collect { state ->
            if (state == Lifecycle.State.RESUMED) {
                savedBank = sharedPreferences.getString(AppConstants.KEY_SELECTED_BANK, "hdfc") ?: "hdfc"
                isUpi123Ready = testResultsManager.getTestResults()?.upi123Enabled == true
                isUssdReady = testResultsManager.getTestResults()?.ussdEnabled == true
            }
        }
    }

    val transactionViewModel: TransactionViewModel = viewModel()
    val recentPayments by transactionViewModel.recentTransactions.collectAsState()
    val selectedTransaction by transactionViewModel.selectedTransaction.collectAsState()
    val isLoading by transactionViewModel.isLoading.collectAsState()
    val error by transactionViewModel.error.collectAsState()

    var showPayContact by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    var showSmsPermissionDialog by remember { mutableStateOf(false) }
    var pendingSmsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    val hostActivity = remember(context) { context.findComponentActivity() }
    val permissionManager = remember(hostActivity) {
        hostActivity?.let { PermissionManager(it) }
    }

    // Runs the queued action (start scan / open pay dialog / initiate transfer)
    // once RECEIVE_SMS is granted; the launcher stays Compose-scoped so no
    // Activity-level callback bridge is needed.
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingSmsAction?.invoke()
        } else {
            Toast.makeText(
                context,
                "SMS permission is required to detect payment confirmations",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingSmsAction = null
    }

    // POST_NOTIFICATIONS backs the payment-outcome notification — the
    // fallback the result screen relies on when its direct background launch
    // is blocked (see PaymentResultNotifier). It is best-effort, not a
    // prerequisite, so we ask ONCE at the first payment and never block on the
    // result: the payment proceeds whether granted or not. Settings offers a
    // later toggle for anyone who declines.
    val postNotificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* best-effort: outcome recorded by the OS; nothing to do here */ }
    val maybeAskNotifications = remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val alreadyAsked = sharedPreferences.getBoolean(AppConstants.KEY_NOTIFICATIONS_ASKED, false)
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!alreadyAsked && !granted) {
                    sharedPreferences.edit()
                        .putBoolean(AppConstants.KEY_NOTIFICATIONS_ASKED, true)
                        .apply()
                    postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // One-shot events from MainActivity (launchers + business-logic helper),
    // replacing the former static @Volatile callbacks on its companion.
    val mainViewModel: MainViewModel = viewModel()
    LaunchedEffect(Unit) {
        mainViewModel.events.collect { event ->
            when (event) {
                MainUiEvent.QrScannerClosed -> isScanning = false
                MainUiEvent.OverlayPermissionNeeded -> showOverlayPermissionDialog = true
            }
        }
    }

    // Reset scanning + refresh list whenever the app resumes
    LaunchedEffect(lifecycle) {
        snapshotFlow { lifecycle.currentState }.collect { state ->
            if (state == Lifecycle.State.RESUMED) {
                isScanning = false
                transactionViewModel.refresh()
            }
        }
    }

    // Safety: never leave the QR button stuck in "Opening..."
    LaunchedEffect(isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(AppConstants.USSD_SESSION_TIMEOUT)
            if (isScanning) isScanning = false
        }
    }

    val selectedBankName = when (savedBank) {
        "sbi" -> "State Bank of India"
        "hdfc" -> "HDFC Bank"
        "icici" -> "ICICI Bank"
        "axis" -> "Axis Bank"
        "kotak" -> "Kotak Mahindra Bank"
        "pnb" -> "Punjab National Bank"
        "bob" -> "Bank of Baroda"
        "yes" -> "Yes Bank"
        "idbi" -> "IDBI Bank"
        "canara" -> "Canara Bank"
        else -> "HDFC Bank"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FastpayTheme.colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FastpayTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp)
                    .align(Alignment.Center)
                    .background(FastpayTheme.colors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header: wordmark, connected bank, and which rails are live.
                HomeHeader(
                    bankName = selectedBankName,
                    isUssdReady = isUssdReady,
                    isUpi123Ready = isUpi123Ready,
                    onSettingsClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PaymentActionButtons(
                    onQRScanClick = {
                        maybeAskNotifications()
                        val hasSms = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECEIVE_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                        when {
                            // *99# not verified yet — the button is in its
                            // "Set up *99#" state; take the user to the test
                            // screen rather than opening a scanner whose
                            // payment rail has not been shown to work.
                            !isUssdReady -> {
                                context.startActivity(
                                    Intent(context, TestConfigurationActivity::class.java)
                                )
                            }
                            !hasSms -> {
                                pendingSmsAction = {
                                    isScanning = true
                                    onQRScanClick()
                                }
                                showSmsPermissionDialog = true
                            }
                            else -> {
                                isScanning = true
                                onQRScanClick()
                            }
                        }
                    },
                    onPayContactClick = {
                        maybeAskNotifications()
                        val hasSms = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECEIVE_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                        when {
                            // UPI 123 IVR not verified yet — the button is in its
                            // "Set up UPI 123 IVR" state; take the user to the
                            // *99# / UPI 123 test screen instead of the pay dialog.
                            !isUpi123Ready -> {
                                context.startActivity(
                                    Intent(context, TestConfigurationActivity::class.java)
                                )
                            }
                            // Overlay permission is required before the payment
                            // call can show its UI, so ask now — not after the
                            // user has filled in the transfer details.
                            !PermissionManager.canDrawOverlays(context) -> {
                                showOverlayPermissionDialog = true
                            }
                            !hasSms -> {
                                pendingSmsAction = { showPayContact = true }
                                showSmsPermissionDialog = true
                            }
                            else -> showPayContact = true
                        }
                    },
                    isUpi123Ready = isUpi123Ready,
                    isUssdReady = isUssdReady,
                    isScanning = isScanning
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Recent Transactions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(FastpayRadius.card),
                    colors = CardDefaults.cardColors(containerColor = FastpayTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.home_recent_payments),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FastpayTheme.colors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.home_recent_payments_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FastpayTheme.colors.textTertiary
                                )
                            }

                            TextButton(
                                onClick = {
                                    context.startActivity(Intent(context, TransactionHistoryActivity::class.java))
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.home_view_all),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FastpayTheme.colors.accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        RecentPaymentsBody(
                            isLoading = isLoading,
                            hasError = error != null,
                            payments = recentPayments,
                            onRetry = { transactionViewModel.refresh() },
                            onSelect = { transactionViewModel.selectTransaction(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Detail dialog for a tapped Recent Payments row — the same
            // surface the history screen shows.
            selectedTransaction?.let { transaction ->
                TransactionDetailDialog(
                    transaction = transaction,
                    onDismiss = { transactionViewModel.clearSelectedTransaction() },
                    onDelete = {
                        transactionViewModel.deleteTransaction(transaction)
                        transactionViewModel.clearSelectedTransaction()
                    }
                )
            }

            // Dialogs
            if (showPayContact) {
                PayContactDialog(
                    onDismiss = { showPayContact = false },
                    onConfirm = { phone, amt ->
                        if (permissionManager?.checkSMSPermissions() != true) {
                            pendingSmsAction = {
                                showPayContact = false
                                onInitiateTransfer(phone, amt)
                            }
                            showSmsPermissionDialog = true
                        } else {
                            showPayContact = false
                            onInitiateTransfer(phone, amt)
                        }
                    }
                )
            }

            if (showOverlayPermissionDialog) {
                PermissionExplanationDialog(
                    title = stringResource(R.string.permission_overlay_title),
                    message = stringResource(R.string.permission_overlay_message),
                    confirmButtonText = stringResource(R.string.permission_grant),
                    onConfirm = {
                        showOverlayPermissionDialog = false
                        onRequestOverlayPermission()
                    },
                    onDismiss = { showOverlayPermissionDialog = false }
                )
            }

            if (showSmsPermissionDialog) {
                PermissionExplanationDialog(
                    title = stringResource(R.string.permission_sms_title),
                    message = stringResource(R.string.permission_sms_message),
                    confirmButtonText = stringResource(R.string.permission_grant),
                    onConfirm = {
                        showSmsPermissionDialog = false
                        // Only RECEIVE_SMS is declared in the manifest and
                        // needed (the app never reads the inbox). The launcher's
                        // callback runs pendingSmsAction once granted.
                        smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    },
                    onDismiss = {
                        showSmsPermissionDialog = false
                        pendingSmsAction = null
                    }
                )
            }
        }
    }
}

/**
 * The recent-payments list and its three non-list states.
 *
 * Split out of MainScreen because the four branches were long enough to push
 * that function past detekt's length and complexity limits, and because the
 * error and empty states are now near-identical in shape — same well, same
 * rhythm — so they belong next to each other.
 */
@Composable
private fun RecentPaymentsBody(
    isLoading: Boolean,
    hasError: Boolean,
    payments: List<PaymentDetails>,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit
) {
    when {
        isLoading -> CenteredState(
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = FastpayTheme.colors.accent,
                    strokeWidth = 3.dp
                )
            },
            title = stringResource(R.string.home_loading_transactions)
        )

        hasError -> CenteredState(
            icon = { CenteredStateIcon(Icons.Default.ErrorOutline, FastpayTheme.colors.statusError) },
            title = stringResource(R.string.home_transactions_error_title),
            subtitle = stringResource(R.string.error_load_transactions)
        ) {
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                    color = FastpayTheme.colors.accent
                )
            }
        }

        payments.isEmpty() -> CenteredState(
            icon = { CenteredStateIcon(Icons.Default.History, FastpayTheme.colors.textTertiary) },
            title = stringResource(R.string.home_no_transactions_title),
            subtitle = stringResource(R.string.home_no_transactions_sub)
        )

        else -> LazyColumn(
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(payments) { index, payment ->
                TransactionItem(
                    payment = payment,
                    index = index,
                    onClick = { onSelect(payment.id) }
                )
            }
        }
    }
}

/** The tinted glyph inside a [CenteredState] well. */
@Composable
private fun CenteredStateIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(28.dp)
    )
}

/**
 * A centred well with an icon, a title and an optional subtitle — the shared
 * shape for the loading, error and empty states.
 */
@Composable
private fun CenteredState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    val colors = FastpayTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(8.dp))
            action()
        }
    }
}

@Composable
fun PayContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findComponentActivity() }
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    // UPI 123Pay's IVR will not accept 5000 or more (AppConstants
    // .UPI123PAY_MAX_AMOUNT = 4999), so the cap is enforced before dialling.
    val isOverCap = (amount.toLongOrNull() ?: 0L) > AppConstants.UPI123PAY_MAX_AMOUNT.toLong()
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showContactPermissionDialog by remember { mutableStateOf(false) }
    val permissionManager = remember(hostActivity) {
        hostActivity?.let { PermissionManager(it) }
    }
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showContactPicker = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FastpayTheme.colors.surfaceOverlay,
        title = {
            Text(
                text = stringResource(R.string.pay_contact_title),
                style = MaterialTheme.typography.titleLarge,
                color = FastpayTheme.colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                selectedContactName?.let { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(FastpayRadius.control))
                            .background(FastpayTheme.colors.accentSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = FastpayTheme.colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pay_contact_sending_to, name),
                            style = MaterialTheme.typography.bodySmall,
                            color = FastpayTheme.colors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                phoneNumber = it
                                selectedContactName = null
                            }
                        },
                        label = {
                            Text(
                                stringResource(R.string.home_field_mobile_label),
                                color = FastpayTheme.colors.textSecondary
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.home_field_mobile_hint),
                                color = FastpayTheme.colors.textTertiary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(FastpayRadius.control),
                        modifier = Modifier.weight(1f),
                        colors = fastpayTextFieldColors()
                    )

                    IconButton(
                        onClick = {
                            val pm = permissionManager
                            if (pm == null) {
                                Toast.makeText(
                                    context.applicationContext,
                                    context.getString(R.string.error_contacts_unavailable),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@IconButton
                            }
                            if (pm.hasContactPermission()) {
                                showContactPicker = true
                            } else {
                                showContactPermissionDialog = true
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(FastpayRadius.control))
                            .background(FastpayTheme.colors.accentSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermContactCalendar,
                            contentDescription = stringResource(R.string.cd_select_contact),
                            tint = FastpayTheme.colors.accent
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            amount = it
                        }
                    },
                    label = {
                        Text(
                            stringResource(R.string.home_field_amount_label),
                            color = FastpayTheme.colors.textSecondary
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.home_field_amount_hint),
                            color = FastpayTheme.colors.textTertiary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isOverCap,
                    shape = RoundedCornerShape(FastpayRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fastpayTextFieldColors(isError = isOverCap)
                )

                // Told here, in the dialog, while the number can still be
                // corrected. The IVR itself only rejects an over-cap amount
                // mid-call, after the user has already dialled.
                if (isOverCap) {
                    Text(
                        text = Upi123CallStringBuilder.Reason.AMOUNT_ABOVE_CAP.messageFor(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = FastpayTheme.colors.statusError
                    )
                }
            }
        },
        confirmButton = {
            val canTransfer = phoneNumber.length == 10 && amount.isNotEmpty() &&
                amount != "0" && !isOverCap
            TextButton(
                onClick = { onConfirm(phoneNumber, amount) },
                enabled = canTransfer
            ) {
                Text(
                    stringResource(R.string.action_transfer),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (canTransfer) {
                        FastpayTheme.colors.accent
                    } else {
                        FastpayTheme.colors.textDisabled
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = FastpayTheme.colors.textSecondary)
            }
        }
    )

    if (showContactPicker) {
        ContactPickerDialog(
            onDismiss = { showContactPicker = false },
            onContactSelected = { contact ->
                phoneNumber = contact.phoneNumber
                selectedContactName = contact.name
                showContactPicker = false
            }
        )
    }

    if (showContactPermissionDialog) {
        PermissionExplanationDialog(
            title = stringResource(R.string.permission_contacts_title),
            message = stringResource(R.string.permission_contacts_message),
            confirmButtonText = stringResource(R.string.permission_grant),
            onConfirm = {
                showContactPermissionDialog = false
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onDismiss = { showContactPermissionDialog = false }
        )
    }
}

@Composable
fun PermissionExplanationDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FastpayTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceOverlay,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_not_now), color = colors.textSecondary)
            }
        }
    )
}
