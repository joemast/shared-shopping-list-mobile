package com.example.sharedshoppinglist

import com.example.sharedshoppinglist.fake.FakeShoppingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AND-11 — base-URL configuration check (AC22, R4, R7).
 *
 * This is a configuration assertion, not a behavioral test. Its value is
 * catching the classic emulator mistake of pointing at `localhost` instead of
 * `10.0.2.2`, and confirming the base URL is overridable for a real device or
 * the (deferred) VPS URL.
 *
 * Documented manual fallback (per the testing plan): if this assertion ever
 * becomes impractical, the same guarantee is verified manually in the Stage 8
 * demo runbook by confirming the debug build targets `http://10.0.2.2:8000/` and
 * that [AppContainer] accepts a device/VPS override. It is therefore not a hard
 * release gate.
 */
class BaseUrlConfigTest {

    @Test
    fun `AND-11 debug base url targets the emulator host alias`() {
        assertEquals("http://10.0.2.2:8000/", BuildConfig.API_BASE_URL)
        assertTrue(BuildConfig.API_BASE_URL.startsWith("http://10.0.2.2:"))
    }

    @Test
    fun `AND-11 base url is overridable for a device or VPS`() {
        val container = AppContainer(
            baseUrl = "http://192.168.1.50:8000/",
            repository = FakeShoppingRepository(),
        )

        assertEquals("http://192.168.1.50:8000/", container.baseUrl)
    }
}
