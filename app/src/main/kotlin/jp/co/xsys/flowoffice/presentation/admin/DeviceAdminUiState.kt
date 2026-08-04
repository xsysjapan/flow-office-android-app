package jp.co.xsys.flowoffice.presentation.admin

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import jp.co.xsys.flowoffice.data.remote.AdminBootstrap
import jp.co.xsys.flowoffice.data.remote.AdminUser
import jp.co.xsys.flowoffice.data.remote.AuthenticationKeySummary

enum class DeviceAdminPhase {
    ENTRY,
    BOOTSTRAP_SELECT,
    BOOTSTRAP_SCAN,
    ADMIN_SCAN,
    USERS,
    USER_DETAIL,
    REGISTER_SCAN,
    REGISTERED,
}

enum class DeviceAdminAvailability {
    CHECKING,
    AVAILABLE,
    UNAVAILABLE,
}

@Immutable
data class DeviceAdminUiState(
    val visible: Boolean = false,
    val availability: DeviceAdminAvailability = DeviceAdminAvailability.CHECKING,
    val phase: DeviceAdminPhase = DeviceAdminPhase.ENTRY,
    val bootstrap: AdminBootstrap? = null,
    val bootstrapPending: Boolean = false,
    val selectedBootstrapAdmin: AdminUser? = null,
    val sessionAdminName: String = "",
    val expiresAt: String = "",
    val users: List<AdminUser> = emptyList(),
    val searchQuery: String = "",
    val selectedUser: AdminUser? = null,
    val authenticationKeys: List<AuthenticationKeySummary> = emptyList(),
    val busy: Boolean = false,
    val pairingCleared: Boolean = false,
    @param:StringRes val errorResId: Int? = null,
) {
    val canOpen: Boolean
        get() = availability == DeviceAdminAvailability.AVAILABLE && !visible && !bootstrapPending
}
