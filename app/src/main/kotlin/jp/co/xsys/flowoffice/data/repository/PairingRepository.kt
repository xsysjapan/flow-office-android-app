package jp.co.xsys.flowoffice.data.repository

import jp.co.xsys.flowoffice.data.remote.PairingApiClient
import jp.co.xsys.flowoffice.data.security.DeviceActivation
import jp.co.xsys.flowoffice.data.security.DeviceActivationStore

class PairingRepository(
    private val apiClient: PairingApiClient,
    private val activationStore: DeviceActivationStore,
) {
    fun activateDevice(
        payload: PairingClaimPayload,
    ) {
        val response = apiClient.claimPairing(
            claimUrl = payload.claimUrl,
            claimToken = payload.claimToken,
        )

        activationStore.saveActivation(
            DeviceActivation(
                apiBaseUrl = response.apiBaseUrl,
                deviceId = response.deviceId,
                token = response.token,
                deviceJson = response.deviceJson,
            ),
        )
    }
}
