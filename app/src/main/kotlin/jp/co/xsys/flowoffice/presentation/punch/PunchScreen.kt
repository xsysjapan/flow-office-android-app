package jp.co.xsys.flowoffice.presentation.punch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isLandscape = maxWidth > maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = PUNCH_CONTENT_MAX_WIDTH_DP.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Header(connection = state.connection)
                if (state.canPunch) {
                    NfcHero(operation = state.operation)
                    PunchTypeGrid(
                        selectedType = state.selectedType,
                        enabled = state.operation !is PunchOperationState.Sending,
                        singleRow = isLandscape,
                        onSelectType = onSelectType,
                    )
                    PunchOperationStatus(operation = state.operation)
                } else {
                    UnavailablePanel()
                }
                DeviceStatus(state = state)
                if (showDeviceAdminAction) {
                    OutlinedButton(
                        onClick = onOpenDeviceAdmin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null)
                        Text(
                            text = stringResource(R.string.admin_open),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(connection: ServerConnectionState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.punch_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.punch_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ConnectionBadge(connection = connection)
    }
}

@Composable
private fun ConnectionBadge(connection: ServerConnectionState) {
    val (icon, label, colors) = when (connection) {
        ServerConnectionState.Checking -> Triple(
            Icons.Filled.Sync,
            R.string.server_connection_checking,
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ServerConnectionState.Connected -> Triple(
            Icons.Filled.CloudDone,
            R.string.server_connection_connected,
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ServerConnectionState.Disconnected -> Triple(
            Icons.Filled.CloudOff,
            R.string.server_connection_disconnected,
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer,
        )
        ServerConnectionState.AuthenticationError -> Triple(
            Icons.Filled.VerifiedUser,
            R.string.server_connection_auth_error,
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    Surface(color = colors.first, contentColor = colors.second, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NfcHero(operation: PunchOperationState) {
    val success = operation is PunchOperationState.Success
    val error = operation is PunchOperationState.Error
    val containerColor = when {
        success -> MaterialTheme.colorScheme.secondaryContainer
        error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        success -> MaterialTheme.colorScheme.onSecondaryContainer
        error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val icon = when {
        success -> Icons.Filled.CheckCircle
        error -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.Nfc
    }
    val heading = when (operation) {
        PunchOperationState.WaitingForNfc -> R.string.punch_waiting_nfc
        PunchOperationState.Sending -> R.string.punch_sending
        is PunchOperationState.Success -> R.string.punch_success_heading
        is PunchOperationState.Error -> R.string.punch_error_heading
    }
    val support = when (operation) {
        PunchOperationState.WaitingForNfc -> R.string.punch_waiting_nfc_support
        PunchOperationState.Sending -> R.string.punch_sending_support
        is PunchOperationState.Success -> R.string.punch_success_message
        is PunchOperationState.Error -> operation.messageResId
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (operation is PunchOperationState.Sending) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = contentColor)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(52.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(heading), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(support), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PunchTypeGrid(
    selectedType: PunchType,
    enabled: Boolean,
    singleRow: Boolean,
    onSelectType: (PunchType) -> Unit,
) {
    if (singleRow) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PunchTypeButton(PunchType.CLOCK_IN, selectedType, R.string.punch_clock_in, Icons.AutoMirrored.Filled.Login, enabled, onSelectType, Modifier.weight(1f))
            PunchTypeButton(PunchType.BREAK_START, selectedType, R.string.punch_break_start, Icons.Filled.Coffee, enabled, onSelectType, Modifier.weight(1f))
            PunchTypeButton(PunchType.BREAK_END, selectedType, R.string.punch_break_end, Icons.Filled.PlayArrow, enabled, onSelectType, Modifier.weight(1f))
            PunchTypeButton(PunchType.CLOCK_OUT, selectedType, R.string.punch_clock_out, Icons.AutoMirrored.Filled.Logout, enabled, onSelectType, Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PunchTypeButton(PunchType.CLOCK_IN, selectedType, R.string.punch_clock_in, Icons.AutoMirrored.Filled.Login, enabled, onSelectType, Modifier.weight(1f))
                PunchTypeButton(PunchType.CLOCK_OUT, selectedType, R.string.punch_clock_out, Icons.AutoMirrored.Filled.Logout, enabled, onSelectType, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PunchTypeButton(PunchType.BREAK_START, selectedType, R.string.punch_break_start, Icons.Filled.Coffee, enabled, onSelectType, Modifier.weight(1f))
                PunchTypeButton(PunchType.BREAK_END, selectedType, R.string.punch_break_end, Icons.Filled.PlayArrow, enabled, onSelectType, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PunchTypeButton(
    type: PunchType,
    selectedType: PunchType,
    labelResId: Int,
    icon: ImageVector,
    enabled: Boolean,
    onSelectType: (PunchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = type == selectedType
    val accent = punchAccent(type)
    val buttonModifier = modifier
        .height(84.dp)
        .testTag("punch-type-${type.apiValue}")
        .semantics { this.selected = selected }
    if (selected) {
        Button(
            onClick = { onSelectType(type) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent.first, contentColor = accent.second),
        ) {
            PunchButtonContent(labelResId, icon, selected = true, iconColor = accent.second)
        }
    } else {
        OutlinedButton(
            onClick = { onSelectType(type) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            PunchButtonContent(labelResId, icon, selected = false, iconColor = accent.first)
        }
    }
}

@Composable
private fun PunchButtonContent(labelResId: Int, icon: ImageVector, selected: Boolean, iconColor: Color = Color.Unspecified) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
        Column {
            Text(stringResource(labelResId), style = MaterialTheme.typography.titleMedium)
            if (selected) Text(stringResource(R.string.punch_selected), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun punchAccent(type: PunchType): Pair<Color, Color> = when (type) {
    PunchType.CLOCK_IN -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    PunchType.CLOCK_OUT -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    PunchType.BREAK_START -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    PunchType.BREAK_END -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
}

@Composable
private fun PunchOperationStatus(operation: PunchOperationState) {
    if (operation !is PunchOperationState.Success) return
    val result = operation.result
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            result.employeeName?.let {
                Text(stringResource(R.string.punch_success_employee, it), style = MaterialTheme.typography.titleLarge)
            }
            val time = formatPunchTime(result.punchedAt)
            if (time != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.punch_success_time, punchTypeLabel(result.punchType), time), style = MaterialTheme.typography.bodyLarge)
                }
            }
            result.workMinutes?.let { minutes ->
                Text(
                    stringResource(R.string.punch_work_time, formatWorkMinutes(minutes)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (result.missingPunchCount > 0 || result.currentDayIncomplete) HorizontalDivider()
            if (result.missingPunchCount > 0) WarningRow(stringResource(R.string.punch_missing_warning, result.missingPunchCount))
            if (result.currentDayIncomplete) WarningRow(stringResource(R.string.punch_current_day_warning))
        }
    }
}

@Composable
private fun WarningRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DeviceStatus(state: PunchUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        val deviceId = state.deviceId
        if (deviceId != null) {
            Text(
                text = stringResource(R.string.punch_device_info, deviceId, state.appInstanceIdShort),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.punch_pending_count, state.pendingCount),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UnavailablePanel() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = stringResource(R.string.punch_unavailable_description),
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun punchTypeLabel(type: PunchType): String = stringResource(
    when (type) {
        PunchType.CLOCK_IN -> R.string.punch_clock_in
        PunchType.CLOCK_OUT -> R.string.punch_clock_out
        PunchType.BREAK_START -> R.string.punch_break_start
        PunchType.BREAK_END -> R.string.punch_break_end
    },
)

@Composable
private fun formatWorkMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return if (hours > 0) stringResource(R.string.punch_work_time_hours, hours, remaining)
    else stringResource(R.string.punch_work_time_minutes, remaining)
}

private fun formatPunchTime(value: String?): String? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: DateTimeParseException) {
        null
    }
}

@Preview(name = "Punch - Phone", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PunchScreenPhonePreview() {
    FlowOfficeReaderTheme {
        PunchScreen(
            state = PunchUiState(deviceId = "3fa85f64-5717-4562-b3fc-2c963f66afa6", appInstanceId = "12345678-demo", canPunch = true, connection = ServerConnectionState.Connected),
            onSelectType = {}, onOpenDeviceAdmin = {}, showDeviceAdminAction = true,
        )
    }
}

@Preview(name = "Punch - Success tablet", showBackground = true, widthDp = 900, heightDp = 760)
@Composable
private fun PunchScreenSuccessPreview() {
    FlowOfficeReaderTheme {
        PunchScreen(
            state = PunchUiState(
                selectedType = PunchType.CLOCK_OUT,
                operation = PunchOperationState.Success(PunchResult("山田 太郎", "2026-07-19T18:12:00+09:00", PunchType.CLOCK_OUT, 492, 1)),
                deviceId = "3fa85f64-5717-4562-b3fc-2c963f66afa6", appInstanceId = "12345678-demo", canPunch = true, connection = ServerConnectionState.Connected,
            ),
            onSelectType = {}, onOpenDeviceAdmin = {}, showDeviceAdminAction = true,
        )
    }
}

@Preview(name = "Punch - Phone landscape", showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun PunchScreenLandscapePreview() {
    FlowOfficeReaderTheme {
        PunchScreen(
            state = PunchUiState(
                deviceId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                appInstanceId = "12345678-demo",
                canPunch = true,
                connection = ServerConnectionState.Connected,
            ),
            onSelectType = {},
            onOpenDeviceAdmin = {},
            showDeviceAdminAction = false,
        )
    }
}

@Preview(name = "Punch - Dark offline", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PunchScreenDarkPreview() {
    FlowOfficeReaderTheme(darkTheme = true) {
        PunchScreen(
            state = PunchUiState(
                operation = PunchOperationState.Error(R.string.error_punch_connection_failed),
                pendingCount = 2, deviceId = "3fa85f64-5717-4562-b3fc-2c963f66afa6", appInstanceId = "12345678-demo", canPunch = true,
                connection = ServerConnectionState.Disconnected,
            ),
            onSelectType = {}, onOpenDeviceAdmin = {}, showDeviceAdminAction = false,
        )
    }
}
