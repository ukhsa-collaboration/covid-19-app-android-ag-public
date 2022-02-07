package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import kotlin.test.assertEquals

class CreateIsolationStateTest {

    @MockK
    private lateinit var isolationState: IsolationState

    @MockK
    private lateinit var isolationConfigurationProvider: IsolationConfigurationProvider

    @MockK
    private lateinit var isolationInfo: IsolationInfo

    @MockK
    private lateinit var durationDays: DurationDays

    @MockK
    private lateinit var mockIsolationConfiguration: DurationDays

    private lateinit var createIsolationState: CreateIsolationState

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        with(isolationInfo) {
            every { selfAssessment } returns mockk()
            every { testResult } returns mockk()
            every { contact } returns mockk()
            every { hasAcknowledgedEndOfIsolation } returns false
        }

        createIsolationState = CreateIsolationState(isolationState, isolationConfigurationProvider)
    }

    @Test
    fun `given IsolationInfo is provided when contact,selfAssessment and testResult for IsolationState is null then new IsolationState can be created with isolationConfigurationProvider duration days`() {
        with(isolationState) {
            every { contact } returns null
            every { selfAssessment } returns null
            every { testResult } returns null
            every { isolationConfigurationProvider.durationDays } returns durationDays
        }

        val newIsolationState = createIsolationState(isolationInfo)

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            selfAssessment = isolationInfo.selfAssessment,
            testResult = isolationInfo.testResult,
            contact = isolationInfo.contact,
            hasAcknowledgedEndOfIsolation = isolationInfo.hasAcknowledgedEndOfIsolation
        )

        assertEquals(expectedIsolationState, newIsolationState)
    }

    @Test
    fun `given IsolationInfo is provided when contact is null, selfAssessment and testResult are not null then new IsolationState can be created with isolationConfiguration`() {
        with(isolationState) {
            every { contact } returns null
            every { selfAssessment } returns mockk()
            every { testResult } returns mockk()
            every { isolationConfiguration } returns mockIsolationConfiguration
        }

        val newIsolationState = createIsolationState(isolationInfo)

        val expectedIsolationState = IsolationState(
            isolationConfiguration = mockIsolationConfiguration,
            selfAssessment = isolationInfo.selfAssessment,
            testResult = isolationInfo.testResult,
            contact = isolationInfo.contact,
            hasAcknowledgedEndOfIsolation = isolationInfo.hasAcknowledgedEndOfIsolation
        )

        assertEquals(expectedIsolationState, newIsolationState)
    }

    @Test
    fun `given IsolationInfo is provided when selfAssessment is null contact and testResult are not null then new IsolationState can be created with isolationConfiguration`() {
        with(isolationState) {
            every { contact } returns mockk()
            every { selfAssessment } returns null
            every { testResult } returns mockk()
            every { isolationConfiguration } returns mockIsolationConfiguration
        }

        val newIsolationState = createIsolationState(isolationInfo)

        val expectedIsolationState = IsolationState(
            isolationConfiguration = mockIsolationConfiguration,
            selfAssessment = isolationInfo.selfAssessment,
            testResult = isolationInfo.testResult,
            contact = isolationInfo.contact,
            hasAcknowledgedEndOfIsolation = isolationInfo.hasAcknowledgedEndOfIsolation
        )

        assertEquals(expectedIsolationState, newIsolationState)
    }

    @Test
    fun `given IsolationInfo is provided when testResult is null contact and selfAssessment are not null then new IsolationState can be created with isolationConfiguration`() {
        with(isolationState) {
            every { contact } returns mockk()
            every { selfAssessment } returns mockk()
            every { testResult } returns null
            every { isolationConfiguration } returns mockIsolationConfiguration
        }

        val newIsolationState = createIsolationState(isolationInfo)

        val expectedIsolationState = IsolationState(
            isolationConfiguration = mockIsolationConfiguration,
            selfAssessment = isolationInfo.selfAssessment,
            testResult = isolationInfo.testResult,
            contact = isolationInfo.contact,
            hasAcknowledgedEndOfIsolation = isolationInfo.hasAcknowledgedEndOfIsolation
        )

        assertEquals(expectedIsolationState, newIsolationState)
    }
}
