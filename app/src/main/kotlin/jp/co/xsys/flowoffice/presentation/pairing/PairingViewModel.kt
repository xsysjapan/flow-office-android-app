package jp.co.xsys.flowoffice.presentation.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.data.remote.PairingApiClient
import jp.co.xsys.flowoffice.data.remote.PairingApiException
import jp.co.xsys.flowoffice.data.repository.PairingRepository
import jp.co.xsys.flowoffice.data.repository.PairingClaimPayload
import jp.co.xsys.flowoffice.data.repository.PairingClaimPayloadParser
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError
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
            it.copy(claimUrl = value, submission = PairingSubmissionState.Idle)
        }
    }

    fun onClaimTokenChange(value: String) {
        _uiState.update {
            it.copy(
                claimToken = value,
                submission = PairingSubmissionState.Idle,
            )
        }
    }

    fun onQrCodeScanned(rawValue: String?) {
        val payload = rawValue?.let(PairingClaimPayloadParser::fromQrUrl)
        if (payload == null) {
            _uiState.update {
                it.copy(submission = PairingSubmissionState.Error(R.string.error_pairing_qr_invalid))
            }
            return
        }

        _uiState.update {
            it.copy(
                claimUrl = payload.claimUrl,
                claimToken = payload.claimToken,
                submission = PairingSubmissionState.Idle,
            )
        }
        activate(payload)
    }

    fun onQrScannerFailed() {
        _uiState.update {
            it.copy(submission = PairingSubmissionState.Error(R.string.error_pairing_scanner_unavailable))
        }
    }

    fun activate() {
        val state = _uiState.value
        val payload = PairingClaimPayloadParser.fromClaimUrl(
            claimUrl = state.claimUrl,
            claimToken = state.claimToken,
        )
        if (!state.canSubmit || payload == null) {
            _uiState.update {
                it.copy(submission = PairingSubmissionState.Error(R.string.error_pairing_input_required))
            }
            return
        }
        activate(payload)
    }

    private fun activate(payload: PairingClaimPayload) {
        _uiState.update { it.copy(submission = PairingSubmissionState.Submitting) }

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    repository.activateDevice(payload)
                }
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        claimToken = "",
                        submission = PairingSubmissionState.Success,
                    )
                } else {
                    it.copy(submission = PairingSubmissionState.Error(result.exceptionOrNull().toStringResId()))
                }
            }
        }
    }

    private fun Throwable?.toStringResId(): Int = when (this) {
        is PairingApiException -> error.toStringResId()
        else -> R.string.error_pairing_unknown
    }

    private fun AppError.toStringResId(): Int = when (this) {
        AppError.PairingInputRequired -> R.string.error_pairing_input_required
        AppError.PairingQrInvalid -> R.string.error_pairing_qr_invalid
        AppError.PairingScannerUnavailable -> R.string.error_pairing_scanner_unavailable
        AppError.PairingTimeout -> R.string.error_pairing_timeout
        AppError.PairingConnectionFailed -> R.string.error_pairing_connection_failed
        AppError.PairingResponseInvalid -> R.string.error_pairing_response_invalid
        AppError.PairingTokenMissing -> R.string.error_pairing_token_missing
        AppError.PairingBadRequest -> R.string.error_pairing_bad_request
        AppError.PairingUnauthorized -> R.string.error_pairing_unauthorized
        AppError.PairingForbidden -> R.string.error_pairing_forbidden
        AppError.PairingNotFound -> R.string.error_pairing_not_found
        AppError.PairingValidationFailed -> R.string.error_pairing_validation_failed
        AppError.PairingRateLimited -> R.string.error_pairing_rate_limited
        AppError.PairingServerError -> R.string.error_pairing_server_error
        else -> R.string.error_pairing_unknown
    }
}
