package jp.co.xsys.flowoffice.presentation.pairing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingUiStateTest {
    @Test
    fun `valid inputs enable pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
            deviceId = "123",
            pairingCode = "A1B2C3D4",
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `submitting state disables pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
            deviceId = "123",
            pairingCode = "A1B2C3D4",
            submission = PairingSubmissionState.Submitting,
        )

        assertFalse(state.canSubmit)
    }

    @Test
    fun `non numeric device id disables pairing`() {
        val state = PairingUiState(
            apiBaseUrl = "https://office.example.jp/api/",
            deviceId = "device-123",
            pairingCode = "A1B2C3D4",
        )

        assertFalse(state.canSubmit)
    }
}
