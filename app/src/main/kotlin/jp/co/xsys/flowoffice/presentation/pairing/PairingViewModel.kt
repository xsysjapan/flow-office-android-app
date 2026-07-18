package jp.co.xsys.flowoffice.presentation.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.xsys.flowoffice.data.remote.PairingApiClient
import jp.co.xsys.flowoffice.data.remote.PairingApiException
import jp.co.xsys.flowoffice.data.repository.PairingRepository
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PairingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PairingRepository(
        apiClient = PairingApiClient(),
        activationStore = DeviceActivationStore(application),
    )

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onApiBaseUrlChange(value: String) {
        _uiState.update {
            it.copy(apiBaseUrl = value, submission = PairingSubmissionState.Idle)
        }
    }

    fun onDeviceIdChange(value: String) {
        _uiState.update {
            it.copy(deviceId = value.filter(Char::isDigit), submission = PairingSubmissionState.Idle)
        }
    }

    fun onPairingCodeChange(value: String) {
        _uiState.update {
            it.copy(
                pairingCode = value.uppercase().take(PairingUiState.PAIRING_CODE_LENGTH),
                submission = PairingSubmissionState.Idle,
            )
        }
    }

    fun activate() {
        val state = _uiState.value
        if (!state.canSubmit) {
            _uiState.update {
                it.copy(submission = PairingSubmissionState.Error("APIサーバーURL、端末ID、8文字のコードを入力してください。"))
            }
            return
        }

        val deviceId = state.deviceId.toLongOrNull()
        if (deviceId == null) {
            _uiState.update {
                it.copy(submission = PairingSubmissionState.Error("端末IDは数字で入力してください。"))
            }
            return
        }

        _uiState.update { it.copy(submission = PairingSubmissionState.Submitting) }

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    repository.activateDevice(
                        apiBaseUrl = state.apiBaseUrl,
                        deviceId = deviceId,
                        pairingCode = state.pairingCode,
                    )
                }
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(submission = PairingSubmissionState.Success)
                } else {
                    it.copy(submission = PairingSubmissionState.Error(result.exceptionOrNull().toSafeMessage()))
                }
            }
        }
    }

    private fun Throwable?.toSafeMessage(): String = when (this) {
        is PairingApiException -> safeMessage
        else -> "アクティベーションに失敗しました。時間を置いて再試行してください。"
    }
}
