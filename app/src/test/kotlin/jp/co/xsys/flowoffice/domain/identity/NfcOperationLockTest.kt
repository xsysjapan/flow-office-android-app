package jp.co.xsys.flowoffice.domain.identity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcOperationLockTest {
    @Test
    fun `rejects duplicate callbacks until operation completes`() {
        val lock = NfcOperationLock()

        assertTrue(lock.tryLock())
        assertFalse(lock.tryLock())

        lock.unlock()
        assertTrue(lock.tryLock())
    }
}
