package uk.nhs.nhsx.covid19.android.app.status

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import kotlin.test.assertEquals

class RiskLevelViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val districtAreaStringProvider = mockk<DistrictAreaStringProvider>(relaxed = true)

    private val testSubject = RiskLevelViewModel(districtAreaStringProvider)

    private val buttonUrlObserver = mockk<Observer<Int>>(relaxed = true)

    @Test
    fun `risk level low`() {
        every { districtAreaStringProvider.provide(R.string.low_risk_level_text) } returns R.string.low_risk_level_text_wls

        val result = testSubject.getDistrictAwareRiskLevelInformation(LOW)

        assertEquals(R.string.low_risk_level_text_wls, result)
    }

    @Test
    fun `risk level medium`() {
        every { districtAreaStringProvider.provide(R.string.medium_risk_level_text) } returns R.string.medium_risk_level_text_wls

        val result = testSubject.getDistrictAwareRiskLevelInformation(MEDIUM)

        assertEquals(R.string.medium_risk_level_text_wls, result)
    }

    @Test
    fun `risk level high`() {
        every { districtAreaStringProvider.provide(R.string.high_risk_level_text) } returns R.string.high_risk_level_text_wls

        val result = testSubject.getDistrictAwareRiskLevelInformation(HIGH)

        assertEquals(R.string.high_risk_level_text_wls, result)
    }

    @Test
    fun `button restrictions clicked`() {
        coEvery { districtAreaStringProvider.provide(R.string.url_postal_code_risk_more_info) } returns R.string.url_postal_code_risk_more_info_wls

        testSubject.buttonUrlLiveData().observeForever(buttonUrlObserver)

        testSubject.onRestrictionsButtonClicked()

        verify { buttonUrlObserver.onChanged(R.string.url_postal_code_risk_more_info_wls) }
    }
}
