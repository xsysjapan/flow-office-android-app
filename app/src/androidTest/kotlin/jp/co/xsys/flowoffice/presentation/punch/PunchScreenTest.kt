package jp.co.xsys.flowoffice.presentation.punch

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.domain.punch.PunchType
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class PunchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectedIdleStateShowsPrimaryGuidanceAndSelectedType() {
        setContent(PunchUiState(canPunch = true, connection = ServerConnectionState.Connected))

        composeRule.onNodeWithText(stringResource(R.string.server_connection_connected)).assertExists()
        composeRule.onNodeWithText(stringResource(R.string.punch_waiting_nfc)).assertExists()
        composeRule.onNodeWithText(stringResource(R.string.punch_clock_in)).assertIsSelected()
    }

    @Test
    fun successShowsEmployeeWorkTimeAndWarning() {
        setContent(
            PunchUiState(
                canPunch = true,
                connection = ServerConnectionState.Connected,
                selectedType = PunchType.CLOCK_OUT,
                operation = PunchOperationState.Success(
                    PunchResult(
                        employeeName = "山田 太郎",
                        punchedAt = "2026-07-19T18:12:00+09:00",
                        punchType = PunchType.CLOCK_OUT,
                        workMinutes = 492,
                        missingPunchCount = 1,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("山田 太郎 さん").assertExists()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onNodeWithText("8時間12分", substring = true).assertExists()
        composeRule.onNodeWithText("打刻忘れが1件", substring = true).assertExists()
    }

    @Test
    fun disconnectedStateIsDistinctFromSuccess() {
        setContent(
            PunchUiState(
                canPunch = true,
                connection = ServerConnectionState.Disconnected,
                operation = PunchOperationState.Error(R.string.error_punch_connection_failed),
            ),
        )

        composeRule.onNodeWithText(stringResource(R.string.server_connection_disconnected)).assertExists()
        composeRule.onNodeWithText(stringResource(R.string.punch_error_heading)).assertExists()
    }

    @Test
    fun landscapePlacesPunchTypesInWorkflowOrderOnOneRow() {
        composeRule.setContent {
            FlowOfficeReaderTheme {
                PunchScreen(
                    state = PunchUiState(canPunch = true, connection = ServerConnectionState.Connected),
                    onSelectType = {},
                    onOpenDeviceAdmin = {},
                    showDeviceAdminAction = false,
                    modifier = Modifier.requiredSize(width = 800.dp, height = 400.dp),
                )
            }
        }

        val types = listOf(
            PunchType.CLOCK_IN,
            PunchType.BREAK_START,
            PunchType.BREAK_END,
            PunchType.CLOCK_OUT,
        )
        val bounds = types.map { type ->
            composeRule.onNodeWithTag("punch-type-${type.apiValue}")
                .fetchSemanticsNode().boundsInRoot
        }

        assertTrue(bounds.zipWithNext().all { (left, right) -> left.left < right.left })
        assertEquals(1, bounds.map { it.top }.distinct().size)
    }

    private fun setContent(state: PunchUiState) {
        composeRule.setContent {
            FlowOfficeReaderTheme {
                PunchScreen(
                    state = state,
                    onSelectType = {},
                    onOpenDeviceAdmin = {},
                    showDeviceAdminAction = false,
                )
            }
        }
    }

    private fun stringResource(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
