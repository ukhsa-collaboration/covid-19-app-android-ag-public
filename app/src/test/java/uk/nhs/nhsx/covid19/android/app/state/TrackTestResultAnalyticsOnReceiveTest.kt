package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TrackTestResultAnalyticsOnReceiveTest {

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-05-01T10:00:00Z"), ZoneOffset.UTC)

    private val trackTestResultAnalyticsOnReceive = TrackTestResultAnalyticsOnReceive(analyticsEventProcessor)

    @Test
    fun `track analytics events on negative PCR result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createLabResult(NEGATIVE), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on positive PCR result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createLabResult(VirologyTestResult.POSITIVE), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(VirologyTestResult.POSITIVE, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void PCR result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createLabResult(VOID), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on negative assisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(NEGATIVE, RAPID_RESULT), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, RAPID_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on negative unassisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(NEGATIVE, RAPID_SELF_REPORTED), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(
                ResultReceived(
                    NEGATIVE,
                    RAPID_SELF_REPORTED,
                    OUTSIDE_APP
                )
            )
        }
    }

    @Test
    fun `track analytics events on positive assisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(VirologyTestResult.POSITIVE, RAPID_RESULT), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(VirologyTestResult.POSITIVE, RAPID_RESULT, OUTSIDE_APP))
            analyticsEventProcessor.track(ReceivedUnconfirmedPositiveTestResult)
        }
    }

    @Test
    fun `track analytics events on positive unassisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(VirologyTestResult.POSITIVE, RAPID_SELF_REPORTED), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(
                ResultReceived(
                    VirologyTestResult.POSITIVE,
                    RAPID_SELF_REPORTED,
                    OUTSIDE_APP
                )
            )
            analyticsEventProcessor.track(ReceivedUnconfirmedPositiveTestResult)
        }
    }

    @Test
    fun `track analytics events on void assisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(VOID, RAPID_RESULT), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, RAPID_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void unassisted LFD result`() = runBlocking {
        trackTestResultAnalyticsOnReceive(createRapidResult(VOID, RAPID_SELF_REPORTED), OUTSIDE_APP)

        verifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, RAPID_SELF_REPORTED, OUTSIDE_APP))
        }
    }

    private fun createLabResult(
        virologyTestResult: VirologyTestResult,
    ) = ReceivedTestResult(
        testEndDate = Instant.now(fixedClock),
        testResult = virologyTestResult,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        diagnosisKeySubmissionToken = ""
    )

    private fun createRapidResult(
        virologyTestResult: VirologyTestResult,
        kitType: VirologyTestKitType
    ): ReceivedTestResult = mockk {
        every { testResult } returns virologyTestResult
        every { testKitType } returns kitType
        every { requiresConfirmatoryTest } returns true
    }
}
