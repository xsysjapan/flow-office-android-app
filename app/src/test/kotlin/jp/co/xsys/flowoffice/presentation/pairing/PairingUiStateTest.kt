package jp.co.xsys.flowoffice.presentation.pairing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingUiStateTest {
    @Test
    fun `valid inputs enable pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
            claimToken = "temporary-token",
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `submitting state disables pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
            claimToken = "temporary-token",
            submission = PairingSubmissionState.Submitting,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `missing claim token disables pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
        )

        assertFalse(state.canSubmit)
    }
}
