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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val activationStore = DeviceActivationStore(application)
    private val repository = DeviceAdminRepository(
        DeviceAdminApiClient(),
        activationStore,
    )
    private val _uiState = MutableStateFlow(DeviceAdminUiState())
    val uiState: StateFlow<DeviceAdminUiState> = _uiState.asStateFlow()
    private var availabilityJob: Job? = null
    private var requestJob: Job? = null

    init {
        refreshAvailability()
    }

    fun refreshAvailability() {
        availabilityJob?.cancel()
        val bootstrapPending = activationStore.isAdminBootstrapPending()
        _uiState.update {
            it.copy(
                availability = DeviceAdminAvailability.CHECKING,
                visible = bootstrapPending,
                bootstrapPending = bootstrapPending,
                phase = DeviceAdminPhase.ENTRY,
                busy = bootstrapPending,
            )
        }
        availabilityJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { repository.getBootstrap() }
            }
            result.onSuccess { bootstrap ->
                if (activationStore.isAdminBootstrapPending()) {
                    _uiState.value = bootstrapState(bootstrap)
                } else {
                    _uiState.update {
                        it.copy(
                            availability = DeviceAdminAvailability.AVAILABLE,
                            bootstrap = null,
                            bootstrapPending = false,
                            visible = false,
                            busy = false,
                            phase = DeviceAdminPhase.ADMIN_SCAN,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (throwable is DeviceAdminApiException && throwable.statusCode == 403) {
                    activationStore.consumeAdminBootstrap()
                }
                _uiState.update {
                    it.copy(
                        availability = DeviceAdminAvailability.UNAVAILABLE,
                        bootstrap = null,
                        bootstrapPending = false,
                        visible = false,
                        busy = false,
                    )
                }
            }
        }
    }

    fun open() {
        val current = _uiState.value
        if (current.availability != DeviceAdminAvailability.AVAILABLE) return
        _uiState.value = DeviceAdminUiState(
            visible = true,
            availability = current.availability,
            phase = DeviceAdminPhase.ADMIN_SCAN,
        )
    }

    fun close() {
        requestJob?.cancel()
        activationStore.consumeAdminBootstrap()
        val hasActiveSession = _uiState.value.sessionAdminName.isNotBlank()
        _uiState.value = hiddenState()
        if (hasActiveSession) {
            viewModelScope.launch {
                runCatching { withContext(Dispatchers.IO) { repository.endSession() } }
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
        if (_uiState.value.bootstrapPending) {
            activationStore.consumeAdminBootstrap()
        }
        _uiState.update {
            it.copy(
                bootstrapPending = false,
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

    private fun <T> launchRequest(
        block: () -> T,
        success: (T) -> Unit,
        failure: ((Throwable) -> Unit)? = null,
    ) {
        requestJob = viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { block() } }
            result.onSuccess(success).onFailure { throwable ->
                if (failure != null) {
                    failure(throwable)
                } else {
                    _uiState.update {
                        it.copy(busy = false, errorResId = throwable.toStringResId())
                    }
                }
            }
        }
    }

    private fun bootstrapState(bootstrap: AdminBootstrap): DeviceAdminUiState = when (bootstrap) {
        is AdminBootstrap.Self -> DeviceAdminUiState(
            visible = true,
            availability = DeviceAdminAvailability.AVAILABLE,
            phase = DeviceAdminPhase.BOOTSTRAP_SCAN,
            bootstrap = bootstrap,
            bootstrapPending = true,
            selectedBootstrapAdmin = bootstrap.adminUser,
        )
        is AdminBootstrap.Select -> DeviceAdminUiState(
            visible = true,
            availability = DeviceAdminAvailability.AVAILABLE,
            phase = DeviceAdminPhase.BOOTSTRAP_SELECT,
            bootstrap = bootstrap,
            bootstrapPending = true,
        )
    }

    private fun hiddenState(): DeviceAdminUiState {
        val current = _uiState.value
        return DeviceAdminUiState(
            availability = current.availability,
            bootstrapPending = false,
            phase = DeviceAdminPhase.ADMIN_SCAN,
        )
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
