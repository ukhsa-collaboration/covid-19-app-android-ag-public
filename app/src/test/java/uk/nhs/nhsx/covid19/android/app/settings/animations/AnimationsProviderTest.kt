package uk.nhs.nhsx.covid19.android.app.settings.animations

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertTrue

class AnimationsProviderTest {

    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val testSubject = AnimationsProvider(sharedPreferences)

    @Test
    fun `when animation provider value not set, should set to true`() {
        every { sharedPreferences.all[VALUE_KEY] } returns null

        val animationsStatus = testSubject.inAppAnimationEnabled

        assertTrue(animationsStatus)
    }

    companion object {
        private const val VALUE_KEY = "ANIMATIONS_ENABLED"
    }
}
