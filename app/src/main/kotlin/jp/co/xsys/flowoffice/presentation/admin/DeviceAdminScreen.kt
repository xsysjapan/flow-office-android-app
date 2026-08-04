package jp.co.xsys.flowoffice.presentation.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.data.remote.AdminBootstrap
import jp.co.xsys.flowoffice.data.remote.AdminUser

@Composable
fun DeviceAdminScreen(
    state: DeviceAdminUiState,
    onClose: () -> Unit,
    onSelectBootstrapAdmin: (AdminUser) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectUser: (AdminUser) -> Unit,
    onBeginCardRegistration: () -> Unit,
    onBackToUsers: () -> Unit,
    onClearPairing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearPairingConfirmation by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    if (showClearPairingConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearPairingConfirmation = false },
            title = { Text(stringResource(R.string.admin_unpair_confirm_title)) },
            text = { Text(stringResource(R.string.admin_unpair_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearPairingConfirmation = false
                        onClearPairing()
                    },
                ) {
                    Text(stringResource(R.string.admin_unpair_confirm_action))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearPairingConfirmation = false }) {
                    Text(stringResource(R.string.admin_unpair_cancel))
                }
            },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.admin_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    OutlinedButton(onClick = onClose) {
                        Text(stringResource(R.string.admin_back_to_punch))
                    }
                }
                if (state.sessionAdminName.isNotBlank()) {
                    Text(
                        stringResource(R.string.admin_session_user, state.sessionAdminName),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
                when (state.phase) {
                    DeviceAdminPhase.ENTRY -> BootstrapLoadingContent()
                    DeviceAdminPhase.BOOTSTRAP_SELECT -> UserList(
                        heading = stringResource(R.string.admin_select_admin),
                        users = (state.bootstrap as? AdminBootstrap.Select)?.adminUsers.orEmpty(),
                        onSelect = onSelectBootstrapAdmin,
                    )
                    DeviceAdminPhase.BOOTSTRAP_SCAN -> ScanPrompt(
                        stringResource(
                            R.string.admin_scan_bootstrap_card,
                            state.selectedBootstrapAdmin?.name.orEmpty(),
                        ),
                        state.busy,
                    )
                    DeviceAdminPhase.ADMIN_SCAN -> ScanPrompt(
                        stringResource(R.string.admin_scan_admin_card),
                        state.busy,
                    )
                    DeviceAdminPhase.USERS -> UsersContent(
                        state = state,
                        onQueryChange = onSearchQueryChange,
                        onSearch = onSearch,
                        onSelect = onSelectUser,
                        onClearPairing = { showClearPairingConfirmation = true },
                    )
                    DeviceAdminPhase.USER_DETAIL -> UserDetailContent(
                        state = state,
                        onRegister = onBeginCardRegistration,
                        onBack = onBackToUsers,
                    )
                    DeviceAdminPhase.REGISTER_SCAN -> ScanPrompt(
                        stringResource(
                            R.string.admin_scan_employee_card,
                            state.selectedUser?.name.orEmpty(),
                        ),
                        state.busy,
                    )
                    DeviceAdminPhase.REGISTERED -> RegisteredContent(
                        userName = state.selectedUser?.name.orEmpty(),
                        onRegisterAnother = onBeginCardRegistration,
                        onBack = onBackToUsers,
                    )
                }
                state.errorResId?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        }
    }
}

@Composable
private fun BootstrapLoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.admin_bootstrap_loading),
            style = MaterialTheme.typography.titleLarge,
        )
        CircularProgressIndicator()
    }
}

@Composable
private fun UsersContent(
    state: DeviceAdminUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (AdminUser) -> Unit,
    onClearPairing: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.admin_select_employee), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.admin_search_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSearch, enabled = !state.busy) {
                Text(stringResource(R.string.admin_search))
            }
        }
        if (state.busy) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        UserList(null, state.users, onSelect)
        HorizontalDivider()
        OutlinedButton(
            onClick = onClearPairing,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.admin_unpair_action))
        }
    }
}

@Composable
private fun UserList(heading: String?, users: List<AdminUser>, onSelect: (AdminUser) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        heading?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        LazyColumn(
            modifier = Modifier.heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(users, key = { it.id }) { user ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(user) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        val detail = listOf(user.department, user.email).filter { it.isNotBlank() }.joinToString(" / ")
                        if (detail.isNotBlank()) {
                            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserDetailContent(
    state: DeviceAdminUiState,
    onRegister: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.selectedUser?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.admin_existing_keys, state.authenticationKeys.size))
        state.authenticationKeys.forEach { key ->
            Text("• ${key.displayName} (${key.status})")
        }
        Button(onClick = onRegister, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_register_card))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_back_to_users))
        }
    }
}

@Composable
private fun ScanPrompt(message: String, busy: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(message, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.admin_scan_once), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (busy) CircularProgressIndicator()
    }
}

@Composable
private fun RegisteredContent(userName: String, onRegisterAnother: () -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.admin_registered_heading), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.admin_registered_message, userName))
        OutlinedButton(onClick = onRegisterAnother, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_register_another))
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_back_to_users))
        }
    }
}
