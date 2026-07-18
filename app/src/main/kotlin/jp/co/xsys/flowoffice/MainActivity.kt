package jp.co.xsys.flowoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import jp.co.xsys.flowoffice.presentation.pairing.PairingScreen
import jp.co.xsys.flowoffice.presentation.pairing.PairingViewModel
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
                    val viewModel: PairingViewModel = viewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    PairingScreen(
                        state = state,
                        onApiBaseUrlChange = viewModel::onApiBaseUrlChange,
                        onDeviceIdChange = viewModel::onDeviceIdChange,
                        onPairingCodeChange = viewModel::onPairingCodeChange,
                        onPair = viewModel::activate,
                        allowApiUrlEditing = true,
                    )
                }
            }
        }
    }
}
