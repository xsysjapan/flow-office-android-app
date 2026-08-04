package jp.co.xsys.flowoffice.domain.identity

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Prevents concurrent NFC callbacks from starting the same operation more than once.
 */
class NfcOperationLock {
    private val locked = AtomicBoolean(false)

    fun tryLock(): Boolean = locked.compareAndSet(false, true)

    fun unlock() {
        locked.set(false)
    }
}
