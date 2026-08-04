package jp.co.xsys.flowoffice.presentation.punch

import jp.co.xsys.flowoffice.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PunchGreetingTest {
    @Test
    fun `selects greeting for the time of day`() {
        assertEquals(R.string.punch_greeting_morning, greetingResId(5))
        assertEquals(R.string.punch_greeting_morning, greetingResId(10))
        assertEquals(R.string.punch_greeting_daytime, greetingResId(11))
        assertEquals(R.string.punch_greeting_daytime, greetingResId(17))
        assertEquals(R.string.punch_greeting_evening, greetingResId(18))
        assertEquals(R.string.punch_greeting_evening, greetingResId(4))
    }
}
