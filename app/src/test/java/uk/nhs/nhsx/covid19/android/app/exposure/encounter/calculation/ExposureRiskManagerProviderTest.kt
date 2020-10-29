package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi

class ExposureRiskManagerProviderTest {
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val exposureInformationRiskManager = mockk<ExposureInformationRiskManager>()
    private val exposureWindowRiskManager = mockk<ExposureWindowRiskManager>()

    private lateinit var provider: ExposureRiskManagerProvider

    @Before
    fun setUp() {
        provider = ExposureRiskManagerProvider(exposureNotificationApi, exposureInformationRiskManager, exposureWindowRiskManager)
    }

    @Test
    fun `returns exposure window risk manager when exposure notification api returns a version`() = runBlocking {
        coEvery { exposureNotificationApi.version() } returns 2

        val riskManager = provider.riskManager()

        assertThat(riskManager, instanceOf(ExposureWindowRiskManager::class.java))
    }

    @Test
    fun `returns exposure information risk manager when en api returns null version`() = runBlocking {
        coEvery { exposureNotificationApi.version() } returns null

        val riskManager = provider.riskManager()

        assertThat(riskManager, instanceOf(ExposureInformationRiskManager::class.java))
    }
}
