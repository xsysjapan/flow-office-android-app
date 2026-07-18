package jp.co.xsys.flowoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jp.co.xsys.flowoffice.presentation.pairing.PairingScreen
import jp.co.xsys.flowoffice.presentation.pairing.PairingUiState
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlowOfficeReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var state by rememberSaveable(stateSaver = PairingUiState.Saver) {
                        mutableStateOf(PairingUiState())
                    }

                    PairingScreen(
                        state = state,
                        onApiBaseUrlChange = { state = state.copy(apiBaseUrl = it) },
                        onDeviceIdChange = { state = state.copy(deviceId = it) },
                        onPairingCodeChange = {
                            state = state.copy(pairingCode = it.uppercase().take(8))
                        },
                        onPair = {},
                        allowApiUrlEditing = true,
                    )
                }
            }
        }
    }
}
