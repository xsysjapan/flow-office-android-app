package jp.co.xsys.flowoffice.domain.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class NfcUidNormalizerTest {
    @Test
    fun `normalizes uid as uppercase hexadecimal without separators`() {
        val uid = byteArrayOf(0x04, 0xA2.toByte(), 0x24, 0x19, 0xCC.toByte(), 0x21, 0x80.toByte())

        assertEquals("04A22419CC2180", NfcUidNormalizer.normalize(uid))
    }

    @Test
    fun `empty uid becomes empty string`() {
        assertEquals("", NfcUidNormalizer.normalize(byteArrayOf()))
    }
}
