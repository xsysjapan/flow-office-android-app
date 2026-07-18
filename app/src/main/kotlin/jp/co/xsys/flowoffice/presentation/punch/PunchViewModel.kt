package jp.co.xsys.flowoffice.presentation.punch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.data.remote.PunchApiClient
import jp.co.xsys.flowoffice.data.remote.PunchApiException
import jp.co.xsys.flowoffice.data.repository.PunchRepository
import jp.co.xsys.flowoffice.data.repository.PunchRepositoryException
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.identity.NfcUidNormalizer
import jp.co.xsys.flowoffice.domain.punch.PunchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PunchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PunchRepository(
        apiClient = PunchApiClient(),
        activationStore = DeviceActivationStore(application),
    )

    private val _uiState = MutableStateFlow(PunchUiState())
    val uiState: StateFlow<PunchUiState> = _uiState.asStateFlow()

    init {
        refreshDeviceSummary()
    }

    fun selectType(type: PunchType) {
        _uiState.update {
            it.copy(selectedType = type, operation = PunchOperationState.WaitingForNfc)
        }
    }

    fun onNfcTagDiscovered(tagId: ByteArray?) {
        val normalizedUid = tagId?.let(NfcUidNormalizer::normalize).orEmpty()
        if (normalizedUid.isBlank()) {
            _uiState.update {
                it.copy(operation = PunchOperationState.Error(R.string.error_punch_nfc_missing))
            }
            return
        }

        val selectedType = _uiState.value.selectedType
        _uiState.update { it.copy(operation = PunchOperationState.Sending) }

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    repository.sendPunch(
                        punchType = selectedType,
                        authenticationKeyValue = normalizedUid,
                    )
                }
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(operation = PunchOperationState.Success)
                } else {
                    it.copy(
                        pendingCount = it.pendingCount + 1,
                        operation = PunchOperationState.Error(result.exceptionOrNull().toStringResId()),
                    )
                }
            }
        }
    }

    fun refreshDeviceSummary() {
        val summary = repository.readDeviceSummary()
        _uiState.update {
            it.copy(
                deviceId = summary?.deviceId,
                appInstanceId = summary?.appInstanceId.orEmpty(),
            )
        }
    }

    private fun Throwable?.toStringResId(): Int = when (this) {
        is PunchApiException -> error.toStringResId()
        is PunchRepositoryException -> error.toStringResId()
        else -> R.string.error_punch_unknown
    }

    private fun AppError.toStringResId(): Int = when (this) {
        AppError.ActivationMissing -> R.string.error_activation_missing
        AppError.PunchTypeMissing -> R.string.error_punch_type_missing
        AppError.PunchNfcMissing -> R.string.error_punch_nfc_missing
        AppError.PunchTimeout -> R.string.error_punch_timeout
        AppError.PunchConnectionFailed -> R.string.error_punch_connection_failed
        AppError.PunchResponseInvalid -> R.string.error_punch_response_invalid
        AppError.PunchUnauthorized -> R.string.error_punch_unauthorized
        AppError.PunchForbidden -> R.string.error_punch_forbidden
        AppError.PunchValidationFailed -> R.string.error_punch_validation_failed
        AppError.PunchRateLimited -> R.string.error_punch_rate_limited
        AppError.PunchServerError -> R.string.error_punch_server_error
        else -> R.string.error_punch_unknown
    }
}
