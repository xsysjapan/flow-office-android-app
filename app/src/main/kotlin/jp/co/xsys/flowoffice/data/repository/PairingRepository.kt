package jp.co.xsys.flowoffice.data.repository

import jp.co.xsys.flowoffice.data.remote.PairingApiClient
import jp.co.xsys.flowoffice.data.security.DeviceActivation
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore

class PairingRepository(
    private val apiClient: PairingApiClient,
    private val activationStore: DeviceActivationStore,
) {
    fun activateDevice(
        apiBaseUrl: String,
        deviceId: Long,
        pairingCode: String,
    ) {
        val response = apiClient.exchangePairingCode(
            apiBaseUrl = apiBaseUrl,
            deviceId = deviceId,
            pairingCode = pairingCode,
        )

        activationStore.saveActivation(
            DeviceActivation(
                apiBaseUrl = apiBaseUrl.trim().trimEnd('/'),
                deviceId = deviceId,
                token = response.token,
                deviceJson = response.deviceJson,
            ),
        )
    }
}
