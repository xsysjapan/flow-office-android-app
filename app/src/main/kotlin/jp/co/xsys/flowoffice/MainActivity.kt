package jp.co.xsys.flowoffice

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.presentation.admin.DeviceAdminScreen
import jp.co.xsys.flowoffice.presentation.admin.DeviceAdminViewModel
import jp.co.xsys.flowoffice.presentation.pairing.PairingScreen
import jp.co.xsys.flowoffice.presentation.pairing.PairingSubmissionState
import jp.co.xsys.flowoffice.presentation.pairing.PairingViewModel
import jp.co.xsys.flowoffice.presentation.punch.PunchScreen
import jp.co.xsys.flowoffice.presentation.punch.PunchViewModel
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activationStore = DeviceActivationStore(this)
        activationStore.getOrCreateAppInstanceId()

        setContent {
            FlowOfficeReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var showPunch by remember {
                        mutableStateOf(activationStore.readActivation() != null)
                    }
                    val pairingViewModel: PairingViewModel = viewModel()
                    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
                    val punchViewModel: PunchViewModel = viewModel()
                    val punchState by punchViewModel.uiState.collectAsStateWithLifecycle()
                    val adminViewModel: DeviceAdminViewModel = viewModel()
                    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
                    val scannerOptions = remember {
                        GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .enableAutoZoom()
                            .build()
                    }
                    val qrScanner = remember {
                        GmsBarcodeScanning.getClient(this@MainActivity, scannerOptions)
                    }

                    LaunchedEffect(pairingState.submission) {
                        if (pairingState.submission is PairingSubmissionState.Success) {
                            punchViewModel.refreshDeviceSummary()
                            adminViewModel.refreshAvailability()
                            showPunch = true
                        }
                    }

                    DisposableEffect(showPunch, adminState.visible) {
                        val nfcAdapter = NfcAdapter.getDefaultAdapter(this@MainActivity)
                        if (showPunch) {
                            nfcAdapter?.enableReaderMode(
                                this@MainActivity,
                                { tag ->
                                    if (adminState.visible) {
                                        adminViewModel.onNfcTagDiscovered(tag.id)
                                    } else {
                                        punchViewModel.onNfcTagDiscovered(tag.id)
                                    }
                                },
                                NFC_READER_FLAGS,
                                null,
                            )
                        }

                        onDispose {
                            nfcAdapter?.disableReaderMode(this@MainActivity)
                        }
                    }

                    DisposableEffect(showPunch, adminState.visible) {
                        if (showPunch && !adminState.visible) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                        onDispose { }
                    }

                    if (showPunch && adminState.visible) {
                        DeviceAdminScreen(
                            state = adminState,
                            onClose = adminViewModel::close,
                            onSelectBootstrapAdmin = adminViewModel::selectBootstrapAdmin,
                            onSearchQueryChange = adminViewModel::onSearchQueryChange,
                            onSearch = adminViewModel::searchUsers,
                            onSelectUser = adminViewModel::selectUser,
                            onBeginCardRegistration = adminViewModel::beginCardRegistration,
                            onBackToUsers = adminViewModel::backToUsers,
                        )
                    } else if (showPunch) {
                        PunchScreen(
                            state = punchState,
                            onSelectType = punchViewModel::selectType,
                            onOpenDeviceAdmin = adminViewModel::open,
                            showDeviceAdminAction = adminState.canOpen,
                        )
                    } else {
                        PairingScreen(
                            state = pairingState,
                            onApiBaseUrlChange = pairingViewModel::onApiBaseUrlChange,
                            onClaimTokenChange = pairingViewModel::onClaimTokenChange,
                            onScanQr = {
                                qrScanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        pairingViewModel.onQrCodeScanned(barcode.rawValue)
                                    }
                                    .addOnFailureListener {
                                        pairingViewModel.onQrScannerFailed()
                                    }
                            },
                            onPair = pairingViewModel::activate,
                            allowApiUrlEditing = true,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val NFC_READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }
}
