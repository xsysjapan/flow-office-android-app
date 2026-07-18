package jp.co.xsys.flowoffice.data.repository

import java.time.OffsetDateTime
import java.util.UUID
import jp.co.xsys.flowoffice.data.remote.PunchApiClient
import jp.co.xsys.flowoffice.data.remote.PunchRequest
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.punch.PunchType

class PunchRepository(
    private val apiClient: PunchApiClient,
    private val activationStore: DeviceActivationStore,
) {
    fun sendPunch(
        punchType: PunchType,
        authenticationKeyValue: String,
    ) {
        val activation = activationStore.readActivation()
            ?: throw PunchRepositoryException(AppError.ActivationMissing)

        apiClient.sendPunch(
            PunchRequest(
                apiBaseUrl = activation.apiBaseUrl,
                token = activation.token,
                appInstanceId = activation.appInstanceId,
                punchType = punchType,
                punchedAt = OffsetDateTime.now().toString(),
                authenticationKeyValue = authenticationKeyValue,
                idempotencyKey = UUID.randomUUID().toString(),
            ),
        )
    }

    fun readDeviceSummary(): DeviceSummary? {
        val activation = activationStore.readActivation() ?: return null
        return DeviceSummary(
            deviceId = activation.deviceId,
            appInstanceId = activation.appInstanceId,
        )
    }
}

data class DeviceSummary(
    val deviceId: Long,
    val appInstanceId: String,
)

class PunchRepositoryException(
    val error: AppError,
) : Exception(error.name)
