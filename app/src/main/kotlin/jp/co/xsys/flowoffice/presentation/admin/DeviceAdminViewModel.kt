package jp.co.xsys.flowoffice.presentation.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.xsys.flowoffice.R
import jp.co.xsys.flowoffice.data.remote.AdminBootstrap
import jp.co.xsys.flowoffice.data.remote.AdminSession
import jp.co.xsys.flowoffice.data.remote.AdminUser
import jp.co.xsys.flowoffice.data.remote.DeviceAdminApiClient
import jp.co.xsys.flowoffice.data.remote.DeviceAdminApiException
import jp.co.xsys.flowoffice.data.repository.DeviceAdminRepository
import jp.co.xsys.flowoffice.data.repository.DeviceAdminRepositoryException
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.identity.NfcUidNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceAdminRepository(
        DeviceAdminApiClient(),
        DeviceActivationStore(application),
    )
    private val _uiState = MutableStateFlow(DeviceAdminUiState())
    val uiState: StateFlow<DeviceAdminUiState> = _uiState.asStateFlow()

    fun open() {
        _uiState.value = DeviceAdminUiState(visible = true, busy = true)
        launchRequest(
            block = repository::getBootstrap,
            success = { bootstrap ->
                _uiState.update { it.copy(bootstrap = bootstrap, busy = false) }
            },
        )
    }

    fun close() {
        if (_uiState.value.sessionAdminName.isBlank()) {
            _uiState.value = DeviceAdminUiState()
            return
        }
        _uiState.update { it.copy(busy = true, errorResId = null) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.endSession() } }
            _uiState.value = DeviceAdminUiState()
        }
    }

    fun beginAdminCardScan() {
        _uiState.update { it.copy(phase = DeviceAdminPhase.ADMIN_SCAN, errorResId = null) }
    }

    fun beginBootstrap() {
        val bootstrap = _uiState.value.bootstrap ?: return
        _uiState.update {
            when (bootstrap) {
                is AdminBootstrap.Self -> it.copy(
                    phase = DeviceAdminPhase.BOOTSTRAP_SCAN,
                    selectedBootstrapAdmin = bootstrap.adminUser,
                    errorResId = null,
                )
                is AdminBootstrap.Select -> it.copy(
                    phase = DeviceAdminPhase.BOOTSTRAP_SELECT,
                    errorResId = null,
                )
            }
        }
    }

    fun selectBootstrapAdmin(user: AdminUser) {
        _uiState.update {
            it.copy(
                phase = DeviceAdminPhase.BOOTSTRAP_SCAN,
                selectedBootstrapAdmin = user,
                errorResId = null,
            )
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun searchUsers() = loadUsers(_uiState.value.searchQuery)

    fun selectUser(user: AdminUser) {
        _uiState.update { it.copy(busy = true, selectedUser = user, errorResId = null) }
        launchRequest(
            block = { repository.getKeys(user.id) },
            success = { keys ->
                _uiState.update {
                    it.copy(
                        phase = DeviceAdminPhase.USER_DETAIL,
                        authenticationKeys = keys,
                        busy = false,
                    )
                }
            },
        )
    }

    fun beginCardRegistration() {
        _uiState.update { it.copy(phase = DeviceAdminPhase.REGISTER_SCAN, errorResId = null) }
    }

    fun backToUsers() {
        _uiState.update {
            it.copy(
                phase = DeviceAdminPhase.USERS,
                selectedUser = null,
                authenticationKeys = emptyList(),
                errorResId = null,
            )
        }
    }

    fun onNfcTagDiscovered(tagId: ByteArray?) {
        if (_uiState.value.busy) return
        val uid = tagId?.let(NfcUidNormalizer::normalize).orEmpty()
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorResId = R.string.error_admin_nfc_missing) }
            return
        }

        when (_uiState.value.phase) {
            DeviceAdminPhase.ADMIN_SCAN -> startSession(uid)
            DeviceAdminPhase.BOOTSTRAP_SCAN -> registerBootstrap(uid)
            DeviceAdminPhase.REGISTER_SCAN -> registerEmployeeCard(uid)
            else -> Unit
        }
    }

    private fun startSession(uid: String) {
        _uiState.update { it.copy(busy = true, errorResId = null) }
        launchRequest(
            block = { repository.startSession(uid) },
            success = ::onSessionStarted,
        )
    }

    private fun registerBootstrap(uid: String) {
        val bootstrap = _uiState.value.bootstrap ?: return
        val adminUserId = when (bootstrap) {
            is AdminBootstrap.Self -> null
            is AdminBootstrap.Select -> _uiState.value.selectedBootstrapAdmin?.id ?: return
        }
        _uiState.update { it.copy(busy = true, errorResId = null) }
        launchRequest(
            block = { repository.registerBootstrapCard(adminUserId, uid) },
            success = ::onSessionStarted,
        )
    }

    private fun onSessionStarted(session: AdminSession) {
        _uiState.update {
            it.copy(
                sessionAdminName = session.adminUser.name,
                expiresAt = session.expiresAt,
                busy = true,
                errorResId = null,
            )
        }
        loadUsers("")
    }

    private fun loadUsers(query: String) {
        _uiState.update { it.copy(busy = true, errorResId = null) }
        launchRequest(
            block = { repository.getUsers(query) },
            success = { users ->
                _uiState.update {
                    it.copy(phase = DeviceAdminPhase.USERS, users = users, busy = false)
                }
            },
        )
    }

    private fun registerEmployeeCard(uid: String) {
        val user = _uiState.value.selectedUser ?: return
        _uiState.update { it.copy(busy = true, errorResId = null) }
        launchRequest(
            block = { repository.registerKey(user.id, uid) },
            success = { key ->
                _uiState.update {
                    it.copy(
                        phase = DeviceAdminPhase.REGISTERED,
                        authenticationKeys = listOf(key) + it.authenticationKeys,
                        busy = false,
                    )
                }
            },
        )
    }

    private fun <T> launchRequest(block: () -> T, success: (T) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { block() } }
            result.onSuccess(success).onFailure { throwable ->
                _uiState.update {
                    it.copy(busy = false, errorResId = throwable.toStringResId())
                }
            }
        }
    }

    private fun Throwable.toStringResId(): Int {
        val error = when (this) {
            is DeviceAdminApiException -> error
            is DeviceAdminRepositoryException -> error
            else -> AppError.AdminUnknown
        }
        return when (error) {
            AppError.ActivationMissing -> R.string.error_activation_missing
            AppError.AdminNfcMissing -> R.string.error_admin_nfc_missing
            AppError.AdminTimeout -> R.string.error_admin_timeout
            AppError.AdminConnectionFailed -> R.string.error_admin_connection_failed
            AppError.AdminResponseInvalid -> R.string.error_admin_response_invalid
            AppError.AdminUnauthorized -> R.string.error_admin_unauthorized
            AppError.AdminForbidden -> R.string.error_admin_forbidden
            AppError.AdminValidationFailed -> R.string.error_admin_validation_failed
            AppError.AdminRateLimited -> R.string.error_admin_rate_limited
            AppError.AdminServerError -> R.string.error_admin_server_error
            else -> R.string.error_admin_unknown
        }
    }
}
