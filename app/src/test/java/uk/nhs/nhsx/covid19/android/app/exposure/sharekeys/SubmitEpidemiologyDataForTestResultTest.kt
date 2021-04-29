package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import java.time.Instant

class SubmitEpidemiologyDataForTestResultTest {

    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>()
    private val submitEpidemiologyData = mockk<SubmitEpidemiologyData>(relaxUnitFun = true)

    private val testSubject = SubmitEpidemiologyDataForTestResult(epidemiologyEventProvider, submitEpidemiologyData)

    @Test
    fun `submit epidemiology data for positive test result is called`() {
        val epidemiologyEvents = listOf(epidemiologyEvent)

        every { epidemiologyEventProvider.epidemiologyEvents } returns epidemiologyEvents

        testSubject(keySharingInfo)

        verify {
            submitEpidemiologyData.submitAfterPositiveTest(
                epidemiologyEventList = epidemiologyEvents,
                testKitType = keySharingInfo.testKitType,
                requiresConfirmatoryTest = keySharingInfo.requiresConfirmatoryTest
            )
        }
    }

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token1",
        acknowledgedDate = Instant.parse("2020-07-10T01:00:00.00Z"),
        notificationSentDate = null,
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false
    )

    private val epidemiologyEvent = EpidemiologyEvent(
        payload = EpidemiologyEventPayload(
            date = Instant.now(),
            infectiousness = HIGH,
            scanInstances = listOf(),
            riskScore = 10.0,
            riskCalculationVersion = 2,
            isConsideredRisky = false
        )
    )
}
