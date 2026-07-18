package jp.co.xsys.flowoffice.presentation.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviceAdminUiStateTest {
    @Test
    fun `admin action stays hidden until availability is confirmed`() {
        val state = DeviceAdminUiState()

        assertFalse(state.visible)
        assertFalse(state.canOpen)
        assertEquals(DeviceAdminAvailability.CHECKING, state.availability)
    }

    @Test
    fun `device without admin mode is unavailable`() {
        val state = DeviceAdminUiState(availability = DeviceAdminAvailability.UNAVAILABLE)

        assertEquals(DeviceAdminAvailability.UNAVAILABLE, state.availability)
        assertFalse(state.canOpen)
    }

    @Test
    fun `admin action is shown only after availability is confirmed`() {
        val state = DeviceAdminUiState(availability = DeviceAdminAvailability.AVAILABLE)

        assertEquals(true, state.canOpen)
    }
}
