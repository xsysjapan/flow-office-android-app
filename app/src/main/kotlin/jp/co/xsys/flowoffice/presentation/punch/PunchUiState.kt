package jp.co.xsys.flowoffice.presentation.punch

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import jp.co.xsys.flowoffice.domain.punch.PunchType

@Immutable
data class PunchUiState(
    val selectedType: PunchType = PunchType.CLOCK_IN,
    val operation: PunchOperationState = PunchOperationState.WaitingForNfc,
    val pendingCount: Int = 0,
    val deviceId: Long? = null,
    val appInstanceId: String = "",
    val canPunch: Boolean = false,
    val connection: ServerConnectionState = ServerConnectionState.Checking,
) {
    val appInstanceIdShort: String
        get() = appInstanceId.take(APP_INSTANCE_ID_VISIBLE_LENGTH)

    private companion object {
        const val APP_INSTANCE_ID_VISIBLE_LENGTH = 8
    }
}

sealed interface PunchOperationState {
    data object WaitingForNfc : PunchOperationState
    data object Sending : PunchOperationState
    data class Success(val result: PunchResult) : PunchOperationState
    data class Error(@param:StringRes val messageResId: Int) : PunchOperationState
}

enum class ServerConnectionState {
    Checking,
    Connected,
    Disconnected,
    AuthenticationError,
}

@Immutable
data class PunchResult(
    val employeeName: String? = null,
    val punchedAt: String? = null,
    val punchType: PunchType,
    val workMinutes: Int? = null,
    val missingPunchCount: Int = 0,
    val currentDayIncomplete: Boolean = false,
)
