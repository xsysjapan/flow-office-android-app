package jp.co.xsys.flowoffice.presentation.pairing

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

@Immutable
data class PairingUiState(
    val apiBaseUrl: String = "",
    val deviceId: String = "",
    val pairingCode: String = "",
    val submission: PairingSubmissionState = PairingSubmissionState.Idle,
) {
    val canSubmit: Boolean
        get() = apiBaseUrl.isNotBlank() &&
            deviceId.toLongOrNull() != null &&
            pairingCode.length == PAIRING_CODE_LENGTH &&
            submission !is PairingSubmissionState.Submitting

    companion object {
        const val PAIRING_CODE_LENGTH = 8

        val Saver: Saver<PairingUiState, Any> = listSaver(
            save = { listOf(it.apiBaseUrl, it.deviceId, it.pairingCode) },
            restore = {
                PairingUiState(
                    apiBaseUrl = it[0],
                    deviceId = it[1],
                    pairingCode = it[2],
                )
            },
        )
    }
}

sealed interface PairingSubmissionState {
    data object Idle : PairingSubmissionState
    data object Submitting : PairingSubmissionState
    data object Success : PairingSubmissionState
    data class Error(@param:StringRes val messageResId: Int) : PairingSubmissionState
}
