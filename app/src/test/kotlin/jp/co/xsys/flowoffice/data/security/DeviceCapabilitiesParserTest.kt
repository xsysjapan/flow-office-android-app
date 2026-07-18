package jp.co.xsys.flowoffice.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilitiesParserTest {
    @Test
    fun `attendance reader role enables punching`() {
        val capabilities = DeviceCapabilitiesParser.parse(
            """{"roles":["attendance_reader"],"scopes":["admin:mode"]}""",
        )

        assertTrue(capabilities.canPunch)
    }

    @Test
    fun `attendance clock scope enables punching`() {
        val capabilities = DeviceCapabilitiesParser.parse(
            """{"roles":["authentication_device"],"scopes":["attendance:clock"]}""",
        )

        assertTrue(capabilities.canPunch)
    }

    @Test
    fun `admin mode alone does not enable punching`() {
        val capabilities = DeviceCapabilitiesParser.parse(
            """{"roles":["authentication_device"],"scopes":["admin:mode"]}""",
        )

        assertFalse(capabilities.canPunch)
    }

    @Test
    fun `missing device metadata does not enable punching`() {
        assertFalse(DeviceCapabilitiesParser.parse(null).canPunch)
        assertFalse(DeviceCapabilitiesParser.parse("not-json").canPunch)
    }
}
