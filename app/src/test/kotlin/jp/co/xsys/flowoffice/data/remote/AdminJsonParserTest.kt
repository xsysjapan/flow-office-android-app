package jp.co.xsys.flowoffice.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminJsonParserTest {
    @Test
    fun `parses self bootstrap response`() {
        val result = AdminJsonParser.parseBootstrap(
            """{"mode":"self","admin_user":{"id":"01960000-0000-7000-8000-000000000010","name":"管理 太郎","email":"admin@example.jp","department":"管理部"}}""",
        )

        assertTrue(result is AdminBootstrap.Self)
        assertEquals("01960000-0000-7000-8000-000000000010", (result as AdminBootstrap.Self).adminUser.id)
        assertEquals("管理 太郎", result.adminUser.name)
    }

    @Test
    fun `parses paginated user collection`() {
        val users = AdminJsonParser.parseUsers(
            """{"data":[{"id":"01960000-0000-7000-8000-000000000020","name":"社員 花子","email":"user@example.jp","department":"営業部"}],"links":{}}""",
        )

        assertEquals(1, users.size)
        assertEquals("01960000-0000-7000-8000-000000000020", users.single().id)
    }

    @Test
    fun `parses wrapped authentication key collection`() {
        val keys = AdminJsonParser.parseAuthenticationKeys(
            """{"data":[{"id":"01960000-0000-7000-8000-000000000030","display_name":"NFCカード","status":"active"}]}""",
        )

        assertEquals("NFCカード", keys.single().displayName)
        assertEquals("active", keys.single().status)
    }

    @Test
    fun `parses wrapped registered authentication key`() {
        val key = AdminJsonParser.parseAuthenticationKeyResponse(
            """{"data":{"id":"01960000-0000-7000-8000-000000000031","display_name":"NFCカード","status":"active"}}""",
        )

        assertEquals("01960000-0000-7000-8000-000000000031", key.id)
    }
}
