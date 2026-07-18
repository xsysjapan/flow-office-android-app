package jp.co.xsys.flowoffice.presentation.pairing

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PairingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun submitIsDisabledUntilAllInputsAreValid() {
        composeRule.setContent {
            FlowOfficeReaderTheme {
                PairingScreen(
                    state = PairingUiState(),
                    onApiBaseUrlChange = {},
                    onDeviceIdChange = {},
                    onPairingCodeChange = {},
                    onPair = {},
                    allowApiUrlEditing = true,
                )
            }
        }

        composeRule.onNodeWithTag("pairing-submit").assertIsNotEnabled()
    }

    @Test
    fun validInputsEnableSubmitAndDispatchClick() {
        var clicks = 0
        composeRule.setContent {
            FlowOfficeReaderTheme {
                PairingScreen(
                    state = PairingUiState(
                        apiBaseUrl = "https://office.example.jp/api/",
                        deviceId = "123",
                        pairingCode = "A1B2C3D4",
                    ),
                    onApiBaseUrlChange = {},
                    onDeviceIdChange = {},
                    onPairingCodeChange = {},
                    onPair = { clicks += 1 },
                    allowApiUrlEditing = true,
                )
            }
        }

        composeRule.onNodeWithTag("pairing-submit").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun errorStateShowsSafeRecoveryMessage() {
        composeRule.setContent {
            FlowOfficeReaderTheme {
                PairingScreen(
                    state = PairingUiState(
                        submission = PairingSubmissionState.Error(
                            "管理者にコードの再発行を依頼してください。",
                        ),
                    ),
                    onApiBaseUrlChange = {},
                    onDeviceIdChange = {},
                    onPairingCodeChange = {},
                    onPair = {},
                    allowApiUrlEditing = true,
                )
            }
        }

        composeRule.onNodeWithText("ペアリングできませんでした").assertExists()
        composeRule.onNodeWithText("管理者にコードの再発行を依頼してください。").assertExists()
    }
}
