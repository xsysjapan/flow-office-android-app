package jp.co.xsys.flowoffice.data.repository

import java.time.OffsetDateTime
import java.util.UUID
import jp.co.xsys.flowoffice.data.remote.PunchApiClient
import jp.co.xsys.flowoffice.data.remote.PunchRequest
import jp.co.xsys.flowoffice.data.remote.PunchResponse
import jp.co.xsys.flowoffice.data.remote.HeartbeatRequest
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore
import jp.co.xsys.flowoffice.data.security.DeviceCapabilitiesParser
import jp.co.xsys.flowoffice.domain.error.AppError
import jp.co.xsys.flowoffice.domain.punch.PunchType
import jp.co.xsys.flowoffice.domain.punch.PunchTimestampFormatter

class PunchRepository(
    private val apiClient: PunchApiClient,
    private val activationStore: DeviceActivationStore,
) {
    fun sendPunch(
        punchType: PunchType,
        authenticationKeyValue: String,
    ): PunchResponse {
        val activation = activationStore.readActivation()
            ?: throw PunchRepositoryException(AppError.ActivationMissing)
        val timestamp = PunchTimestampFormatter.format(OffsetDateTime.now())

        return apiClient.sendPunch(
            PunchRequest(
                apiBaseUrl = activation.apiBaseUrl,
                token = activation.token,
                appInstanceId = activation.appInstanceId,
                punchType = punchType,
                workDate = timestamp.workDate,
                punchedAt = timestamp.punchedAt,
                authenticationKeyValue = authenticationKeyValue,
                idempotencyKey = UUID.randomUUID().toString(),
            ),
        )
    }

    fun checkServerConnection(appVersion: String) {
        val activation = activationStore.readActivation()
            ?: throw PunchRepositoryException(AppError.ActivationMissing)
        apiClient.sendHeartbeat(
            HeartbeatRequest(
                apiBaseUrl = activation.apiBaseUrl,
                token = activation.token,
                appInstanceId = activation.appInstanceId,
                appVersion = appVersion,
            ),
        )
    }

    fun readDeviceSummary(): DeviceSummary? {
        val activation = activationStore.readActivation() ?: return null
        return DeviceSummary(
            deviceId = activation.deviceId,
            appInstanceId = activation.appInstanceId,
            canPunch = DeviceCapabilitiesParser.parse(activation.deviceJson).canPunch,
        )
    }
}

data class DeviceSummary(
    val deviceId: Long,
    val appInstanceId: String,
    val canPunch: Boolean,
)

class PunchRepositoryException(
    val error: AppError,
) : Exception(error.name)
