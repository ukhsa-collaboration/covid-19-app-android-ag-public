package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AreaRiskLevelProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<Editor>(relaxed = true)

    private val testSubject = AreaRiskLevelProvider(sharedPreferences)

    @Before
    fun setUp() {
        every {
            sharedPreferences.edit()
        } returns sharedPreferencesEditor
    }

    @Test
    fun `get risky post code level to high`() {
        every { sharedPreferences.all["RISKY_POST_CODE_LEVEL_KEY"] } returns "H"

        val actual = testSubject.toRiskLevel()

        assertEquals(HIGH, actual)
    }

    @Test
    fun `set risky post code level to high`() {
        testSubject.setRiskyPostCodeLevel(HIGH)
        verify {
            sharedPreferencesEditor.putString(
                "RISKY_POST_CODE_LEVEL_KEY",
                "H"
            )
        }
    }

    @Test
    fun `get risky post code level to medium`() {
        every { sharedPreferences.all["RISKY_POST_CODE_LEVEL_KEY"] } returns "M"

        val actual = testSubject.toRiskLevel()

        assertEquals(MEDIUM, actual)
    }

    @Test
    fun `set risky post code level to medium`() {
        testSubject.setRiskyPostCodeLevel(MEDIUM)
        verify {
            sharedPreferencesEditor.putString(
                "RISKY_POST_CODE_LEVEL_KEY",
                "M"
            )
        }
    }

    @Test
    fun `get risky post code level to low`() {
        every { sharedPreferences.all["RISKY_POST_CODE_LEVEL_KEY"] } returns "L"

        val actual = testSubject.toRiskLevel()

        assertEquals(LOW, actual)
    }

    @Test
    fun `set risky post code level to low`() {
        testSubject.setRiskyPostCodeLevel(LOW)
        verify {
            sharedPreferencesEditor.putString(
                "RISKY_POST_CODE_LEVEL_KEY",
                "L"
            )
        }
    }

    @Test
    fun `get unknown post code level`() {
        every { sharedPreferences.all["RISKY_POST_CODE_LEVEL_KEY"] } returns "unknown"

        val actual = testSubject.toRiskLevel()

        assertNull(actual)
    }
}
