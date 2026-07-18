package jp.co.xsys.flowoffice.presentation.punch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.domain.punch.PunchType
import jp.co.xsys.flowoffice.presentation.theme.FlowOfficeReaderTheme

private const val PUNCH_CONTENT_MAX_WIDTH_DP = 720

@Composable
fun PunchScreen(
    state: PunchUiState,
    onSelectType: (PunchType) -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    showDeviceAdminAction: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = PUNCH_CONTENT_MAX_WIDTH_DP.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.punch_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(
                        if (state.canPunch) {
                            R.string.punch_description
                        } else {
                            R.string.punch_unavailable_description
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DeviceStatus(state = state)
                if (state.canPunch) {
                    HorizontalDivider()
                    PunchTypeGrid(
                        selectedType = state.selectedType,
                        onSelectType = onSelectType,
                    )
                    PunchOperationStatus(operation = state.operation)
                }
                if (showDeviceAdminAction) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onOpenDeviceAdmin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.admin_open))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatus(state: PunchUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val deviceId = state.deviceId
        if (deviceId != null) {
            Text(
                text = stringResource(
                    R.string.punch_device_info,
                    deviceId,
                    state.appInstanceIdShort,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.punch_pending_count, state.pendingCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PunchTypeGrid(
    selectedType: PunchType,
    onSelectType: (PunchType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PunchTypeButton(
                type = PunchType.CLOCK_IN,
                selectedType = selectedType,
                labelResId = R.string.punch_clock_in,
                onSelectType = onSelectType,
                modifier = Modifier.weight(1f),
            )
            PunchTypeButton(
                type = PunchType.CLOCK_OUT,
                selectedType = selectedType,
                labelResId = R.string.punch_clock_out,
                onSelectType = onSelectType,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PunchTypeButton(
                type = PunchType.BREAK_START,
                selectedType = selectedType,
                labelResId = R.string.punch_break_start,
                onSelectType = onSelectType,
                modifier = Modifier.weight(1f),
            )
            PunchTypeButton(
                type = PunchType.BREAK_END,
                selectedType = selectedType,
                labelResId = R.string.punch_break_end,
                onSelectType = onSelectType,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PunchTypeButton(
    type: PunchType,
    selectedType: PunchType,
    labelResId: Int,
    onSelectType: (PunchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = type == selectedType
    val buttonModifier = modifier
        .height(88.dp)
        .semantics { this.selected = selected }

    if (selected) {
        Button(
            onClick = { onSelectType(type) },
            modifier = buttonModifier,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(labelResId), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.punch_selected), style = MaterialTheme.typography.labelMedium)
            }
        }
    } else {
        OutlinedButton(
            onClick = { onSelectType(type) },
            modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(stringResource(labelResId), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PunchOperationStatus(operation: PunchOperationState) {
    val color = when (operation) {
        PunchOperationState.Success -> MaterialTheme.colorScheme.primary
        is PunchOperationState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val heading = when (operation) {
        PunchOperationState.WaitingForNfc -> R.string.punch_waiting_nfc
        PunchOperationState.Sending -> R.string.punch_sending
        PunchOperationState.Success -> R.string.punch_success_heading
        is PunchOperationState.Error -> R.string.punch_error_heading
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
        if (operation is PunchOperationState.Success) {
            Text(
                text = stringResource(R.string.punch_success_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (operation is PunchOperationState.Error) {
            Text(
                text = stringResource(operation.messageResId),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
    }
}

@Preview(name = "Punch - Phone", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PunchScreenPhonePreview() {
    FlowOfficeReaderTheme {
        PunchScreen(
            state = PunchUiState(
                deviceId = 123,
                appInstanceId = "12345678-1234-1234-1234-123456789012",
                canPunch = true,
            ),
            onSelectType = {},
            onOpenDeviceAdmin = {},
            showDeviceAdminAction = true,
        )
    }
}
