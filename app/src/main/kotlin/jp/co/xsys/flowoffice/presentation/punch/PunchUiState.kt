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
    data object Success : PunchOperationState
    data class Error(@param:StringRes val messageResId: Int) : PunchOperationState
}
