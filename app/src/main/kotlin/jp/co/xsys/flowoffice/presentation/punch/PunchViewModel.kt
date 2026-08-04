package jp.co.xsys.flowoffice.presentation.punch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.xsys.flowoffice.BuildConfig
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.data.remote.PunchApiClient
import jp.co.xsys.flowoffice.data.remote.PunchApiException
import jp.co.xsys.flowoffice.data.repository.PunchRepository
import jp.co.xsys.flowoffice.data.repository.PunchRepositoryException
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.identity.NfcUidNormalizer
import jp.co.xsys.flowoffice.domain.identity.NfcOperationLock
import jp.co.xsys.flowoffice.domain.punch.PunchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

class PunchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PunchRepository(
        apiClient = PunchApiClient(),
        activationStore = DeviceActivationStore(application),
    )

    private val _uiState = MutableStateFlow(PunchUiState())
    val uiState: StateFlow<PunchUiState> = _uiState.asStateFlow()
    private var heartbeatJob: Job? = null
    private val nfcOperationLock = NfcOperationLock()

    init {
        refreshDeviceSummary()
    }

    fun selectType(type: PunchType) {
        if (!_uiState.value.canPunch) return
        if (_uiState.value.operation is PunchOperationState.Sending ||
            _uiState.value.operation is PunchOperationState.Success
        ) return
        _uiState.update {
            it.copy(selectedType = type, operation = PunchOperationState.WaitingForNfc)
        }
    }

    fun onNfcTagDiscovered(tagId: ByteArray?) {
        if (!_uiState.value.canPunch) return
        if (!nfcOperationLock.tryLock()) return
        val normalizedUid = tagId?.let(NfcUidNormalizer::normalize).orEmpty()
        if (normalizedUid.isBlank()) {
            _uiState.update {
                it.copy(operation = PunchOperationState.Error(R.string.error_punch_nfc_missing))
            }
            nfcOperationLock.unlock()
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
                    val response = result.getOrThrow()
                    it.copy(
                        connection = ServerConnectionState.Connected,
                        operation = PunchOperationState.Success(
                            PunchResult(
                                employeeName = response.employeeName,
                                punchedAt = response.punchedAt,
                                punchType = selectedType,
                                workMinutes = response.workMinutes,
                                missingPunchCount = response.missingPunchCount,
                                currentDayIncomplete = response.currentDayIncomplete,
                                fallbackMessageResId = if (
                                    selectedType == PunchType.CLOCK_IN &&
                                    response.employeeName.isNullOrBlank() &&
                                    response.punchedAt.isNullOrBlank() &&
                                    response.workMinutes == null &&
                                    response.missingPunchCount == 0 &&
                                    !response.currentDayIncomplete
                                ) {
                                    greetingResId(LocalTime.now().hour)
                                } else {
                                    null
                                },
                            ),
                        ),
                    )
                } else {
                    val failure = result.exceptionOrNull()
                    it.copy(
                        pendingCount = it.pendingCount + 1,
                        connection = failure.toConnectionState(),
                        operation = PunchOperationState.Error(failure.toStringResId()),
                    )
                }
            }
            val success = _uiState.value.operation as? PunchOperationState.Success
            if (success != null) {
                delay(RESULT_VISIBLE_MILLIS)
                _uiState.update {
                    if (it.operation == success) it.copy(operation = PunchOperationState.WaitingForNfc) else it
                }
            }
            nfcOperationLock.unlock()
        }
    }

    fun refreshDeviceSummary() {
        val summary = repository.readDeviceSummary()
        _uiState.update {
            it.copy(
                deviceId = summary?.deviceId,
                appInstanceId = summary?.appInstanceId.orEmpty(),
                canPunch = summary?.canPunch == true,
            )
        }
        heartbeatJob?.cancel()
        heartbeatJob = if (summary != null) {
            viewModelScope.launch {
                while (isActive) {
                    refreshServerConnection()
                    delay(HEARTBEAT_INTERVAL_MILLIS)
                }
            }
        } else {
            null
        }
    }

    private suspend fun refreshServerConnection() {
        _uiState.update { it.copy(connection = ServerConnectionState.Checking) }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                repository.checkServerConnection(BuildConfig.VERSION_NAME)
            }
        }
        _uiState.update {
            it.copy(
                connection = if (result.isSuccess) {
                    ServerConnectionState.Connected
                } else {
                    result.exceptionOrNull().toConnectionState()
                },
            )
        }
    }

    private fun Throwable?.toConnectionState(): ServerConnectionState = when (this) {
        is PunchApiException -> when {
            statusCode == 401 -> ServerConnectionState.AuthenticationError
            statusCode != null -> ServerConnectionState.Connected
            else -> ServerConnectionState.Disconnected
        }
        else -> ServerConnectionState.Disconnected
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

    private companion object {
        const val RESULT_VISIBLE_MILLIS = 3_000L
        const val HEARTBEAT_INTERVAL_MILLIS = 10 * 60 * 1_000L
    }
}

internal fun greetingResId(hour: Int): Int = when (hour) {
    in 5..10 -> R.string.punch_greeting_morning
    in 11..17 -> R.string.punch_greeting_daytime
    else -> R.string.punch_greeting_evening
}
