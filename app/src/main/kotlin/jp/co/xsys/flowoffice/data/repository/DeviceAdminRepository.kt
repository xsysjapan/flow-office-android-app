package jp.co.xsys.flowoffice.data.repository

import jp.co.xsys.flowoffice.data.remote.AdminBootstrap
import jp.co.xsys.flowoffice.data.remote.AdminSession
import jp.co.xsys.flowoffice.data.remote.AdminUser
import jp.co.xsys.flowoffice.data.remote.AuthenticationKeySummary
import jp.co.xsys.flowoffice.data.remote.DeviceAdminApiClient
import jp.co.xsys.flowoffice.data.remote.DeviceAdminRequest
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError

class DeviceAdminRepository(
    private val apiClient: DeviceAdminApiClient,
    private val activationStore: DeviceActivationStore,
) {
    fun getBootstrap(): AdminBootstrap = apiClient.getBootstrap(request())
    fun registerBootstrapCard(adminUserId: String?, rawKeyValue: String): AdminSession =
        apiClient.registerBootstrapCard(request(), adminUserId, rawKeyValue)
    fun startSession(rawKeyValue: String): AdminSession = apiClient.startSession(request(), rawKeyValue)
    fun endSession() = apiClient.endSession(request())
    fun getUsers(query: String): List<AdminUser> = apiClient.getUsers(request(), query)
    fun getKeys(userId: String): List<AuthenticationKeySummary> =
        apiClient.getAuthenticationKeys(request(), userId)
    fun registerKey(userId: String, rawKeyValue: String): AuthenticationKeySummary =
        apiClient.registerAuthenticationKey(request(), userId, rawKeyValue)

    private fun request(): DeviceAdminRequest {
        val activation = activationStore.readActivation()
            ?: throw DeviceAdminRepositoryException(AppError.ActivationMissing)
        return DeviceAdminRequest(
            activation.apiBaseUrl,
            activation.token,
            activation.appInstanceId,
        )
    }
}

class DeviceAdminRepositoryException(val error: AppError) : Exception(error.name)
