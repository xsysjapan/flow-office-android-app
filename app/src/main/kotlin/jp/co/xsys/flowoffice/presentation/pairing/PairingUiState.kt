package jp.co.xsys.flowoffice.presentation.pairing

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
@Immutable
data class PairingUiState(
    val claimUrl: String = "",
    val claimToken: String = "",
    val submission: PairingSubmissionState = PairingSubmissionState.Idle,
) {
    val canSubmit: Boolean
        get() = claimUrl.isNotBlank() &&
            claimToken.isNotBlank() &&
            submission !is PairingSubmissionState.Submitting
}

sealed interface PairingSubmissionState {
    data object Idle : PairingSubmissionState
    data object Submitting : PairingSubmissionState
    data object Success : PairingSubmissionState
    data class Error(@param:StringRes val messageResId: Int) : PairingSubmissionState
}
