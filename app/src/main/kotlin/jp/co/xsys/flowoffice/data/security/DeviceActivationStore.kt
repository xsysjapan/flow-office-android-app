package jp.co.xsys.flowoffice.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DeviceActivationStore(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun saveActivation(activation: DeviceActivation) {
        val encryptedToken = encryptToken(activation.token)
        preferences.edit()
            .putString(KEY_APP_INSTANCE_ID, getOrCreateAppInstanceId())
            .putLong(KEY_DEVICE_ID, activation.deviceId)
            .putString(KEY_API_BASE_URL, activation.apiBaseUrl)
            .putString(KEY_DEVICE_JSON, activation.deviceJson)
            .putString(KEY_TOKEN_CIPHERTEXT, encryptedToken.ciphertext)
            .putString(KEY_TOKEN_IV, encryptedToken.iv)
            .putLong(KEY_PAIRED_AT_MILLIS, System.currentTimeMillis())
            .putBoolean(KEY_ADMIN_BOOTSTRAP_PENDING, true)
            .apply()
    }

    fun isAdminBootstrapPending(): Boolean =
        preferences.getBoolean(KEY_ADMIN_BOOTSTRAP_PENDING, false)

    fun consumeAdminBootstrap() {
        preferences.edit().putBoolean(KEY_ADMIN_BOOTSTRAP_PENDING, false).apply()
    }

    fun readActivation(): StoredDeviceActivation? {
        val apiBaseUrl = preferences.getString(KEY_API_BASE_URL, null) ?: return null
        val ciphertext = preferences.getString(KEY_TOKEN_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_TOKEN_IV, null) ?: return null
        val deviceId = preferences.getLong(KEY_DEVICE_ID, MISSING_DEVICE_ID)
        if (deviceId == MISSING_DEVICE_ID) return null

        return StoredDeviceActivation(
            apiBaseUrl = apiBaseUrl,
            deviceId = deviceId,
            appInstanceId = getOrCreateAppInstanceId(),
            deviceJson = preferences.getString(KEY_DEVICE_JSON, null),
            token = decryptToken(
                EncryptedToken(
                    ciphertext = ciphertext,
                    iv = iv,
                ),
            ),
        )
    }

    fun getOrCreateAppInstanceId(): String {
        preferences.getString(KEY_APP_INSTANCE_ID, null)?.let { return it }

        val appInstanceId = UUID.randomUUID().toString()
        preferences.edit()
            .putString(KEY_APP_INSTANCE_ID, appInstanceId)
            .apply()
        return appInstanceId
    }

    private fun encryptToken(token: String): EncryptedToken {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return EncryptedToken(
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decryptToken(encryptedToken: EncryptedToken): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(encryptedToken.iv, Base64.NO_WRAP)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )

        val ciphertext = Base64.decode(encryptedToken.ciphertext, Base64.NO_WRAP)
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private data class EncryptedToken(
        val ciphertext: String,
        val iv: String,
    )

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "flow_office_reader_device_token"
        const val PREFERENCES_NAME = "device_activation"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val MISSING_DEVICE_ID = -1L
        const val KEY_APP_INSTANCE_ID = "app_instance_id"
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_JSON = "device_json"
        const val KEY_TOKEN_CIPHERTEXT = "token_ciphertext"
        const val KEY_TOKEN_IV = "token_iv"
        const val KEY_PAIRED_AT_MILLIS = "paired_at_millis"
        const val KEY_ADMIN_BOOTSTRAP_PENDING = "admin_bootstrap_pending"
    }
}

data class DeviceActivation(
    val apiBaseUrl: String,
    val deviceId: Long,
    val token: String,
    val deviceJson: String?,
)

data class StoredDeviceActivation(
    val apiBaseUrl: String,
    val deviceId: Long,
    val appInstanceId: String,
    val deviceJson: String?,
    val token: String,
)
