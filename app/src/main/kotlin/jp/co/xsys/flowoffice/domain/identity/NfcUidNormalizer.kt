package jp.co.xsys.flowoffice.domain.identity

object NfcUidNormalizer {
    fun normalize(uid: ByteArray): String =
        uid.joinToString(separator = "") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
}
