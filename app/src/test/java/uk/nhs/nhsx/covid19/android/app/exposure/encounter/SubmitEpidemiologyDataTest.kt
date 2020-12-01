package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata

class SubmitEpidemiologyDataTest {

    private val mockEpidemiologyDataApi = mockk<EpidemiologyDataApi>()
    private val mockMetadataProvider = mockk<MetadataProvider>()
    private val testSubject = SubmitEpidemiologyData(mockMetadataProvider, mockEpidemiologyDataApi)

    @Before
    fun setUp() {
        coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } returns Unit
    }

    @Test
    fun `when given a list of exposure windows and associated risk api is called`() = runBlocking {
        val dayRisk = DayRisk(startOfDayMillis = 123L, calculatedRisk = 10.0)
        val exposureWindow = ExposureWindow.Builder().setInfectiousness(Infectiousness.HIGH).build()
        val exposureWindowWithRisk = SubmitEpidemiologyData.ExposureWindowWithRisk(dayRisk, exposureWindow)

        every { mockMetadataProvider.getMetadata() } returns Metadata("", "", "", "")

        testSubject.invoke(listOf(exposureWindowWithRisk))

        coVerify { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
    }

    @Test
    fun `when given an empty list api is not called`() = runBlocking {
        testSubject.invoke(listOf())

        coVerify(exactly = 0) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
    }
}
