package uk.nhs.nhsx.covid19.android.app.about

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.BaseMyDataViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.about.BaseMyDataViewModel.MyDataState
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.selectNewest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MyDataViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = MyDataViewModel(
        stateMachine,
        lastVisitedBookTestTypeVenueDateProvider,
        fixedClock
    )

    private val userDataStateObserver = mockk<Observer<MyDataState>>(relaxed = true)
    private val venueVisitsEditModeChangedObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxed = true)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)

        testSubject.myDataState().observeForever(userDataStateObserver)

        every { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue } returns LastVisitedBookTestTypeVenueDate(
            lastRiskyVenueVisit,
            RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
        )
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `onResume triggers view state emission`() = runBlocking {
        setIsolationState(contactAndIndexIsolation)

        testSubject.onResume()

        verify { userDataStateObserver.onChanged(expectedInitialUserDataState) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `onResume with no changes to view state does not trigger view state emission`() = runBlocking {
        setIsolationState(contactAndIndexIsolation)

        testSubject.onResume()
        testSubject.onResume()

        verify(exactly = 1) { userDataStateObserver.onChanged(any()) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `loading user data only returns main post code when local authority is not stored`() {
        setIsolationState(contactAndIndexIsolation)

        testSubject.onResume()

        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
        verify(exactly = 0) { allUserDataDeletedObserver.onChanged(any()) }
    }

    @Test
    fun `loading user data returns exposure notification details and dailyContactTestingOptInDate when previously in contact case`() {
        val contactExposureDate = contactCaseExposureDate.minusDays(12)
        val contactNotificationDate = contactCaseNotificationDate.minusDays(12)
        val contactExpiryDate = contactExposureDate.plusDays(5)
        val dailyContactTestingOptInDate = dailyContactTestingOptInDate.minusDays(12)
        val indexSelfAssessmentDate = selfAssessmentDate.minusDays(12)
        val indexSymptomsOnsetDate = symptomsOnsetDate.minusDays(12)
        val indexExpiryDate = indexCaseExpiryDate.minusDays(12)

        setIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    exposureDate = contactExposureDate,
                    notificationDate = contactNotificationDate,
                    expiryDate = contactExpiryDate,
                    dailyContactTestingOptInDate = dailyContactTestingOptInDate
                ),
                indexInfo = IndexCase(
                    isolationTrigger = SelfAssessment(
                        selfAssessmentDate = indexSelfAssessmentDate,
                        onsetDate = indexSymptomsOnsetDate
                    ),
                    testResult = acknowledgedTestResult,
                    expiryDate = indexExpiryDate
                )
            )
        )

        testSubject.onResume()

        verify {
            userDataStateObserver.onChanged(
                MyDataState(
                    isolationState = IsolationViewState(
                        contactCaseEncounterDate = contactExposureDate,
                        contactCaseNotificationDate = contactNotificationDate,
                        indexCaseSymptomOnsetDate = indexSymptomsOnsetDate,
                        dailyContactTestingOptInDate = dailyContactTestingOptInDate
                    ),
                    lastRiskyVenueVisitDate = lastRiskyVenueVisit,
                    acknowledgedTestResult = acknowledgedTestResult
                )
            )
        }
    }

    @Test
    fun `loading user data doesn't return exposure notification details and dailyContactTestingOptInDate when previously in contact case`() {
        setIsolationState(IsolationState(isolationConfiguration = DurationDays()))

        testSubject.onResume()

        verify {
            userDataStateObserver.onChanged(
                expectedInitialUserDataState.copy(
                    isolationState = null,
                    acknowledgedTestResult = null
                )
            )
        }
    }

    private fun setIsolationState(isolationState: IsolationState) {
        every { stateMachine.readState() } returns isolationState
        every { stateMachine.readLogicalState() } returns isolationState.asLogical()
    }

    private val lastRiskyVenueVisit = LocalDate.of(2020, 8, 12)

    private val acknowledgedTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock),
        testResult = POSITIVE,
        acknowledgedDate = LocalDate.now(fixedClock),
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false,
        confirmedDate = null
    )

    private val contactCaseExposureDate = LocalDate.parse("2020-05-19")
    private val contactCaseNotificationDate = LocalDate.parse("2020-05-20")
    private val dailyContactTestingOptInDate = LocalDate.parse("2020-05-21")
    private val contactCaseExpiryDate = LocalDate.parse("2020-05-24")
    private val selfAssessmentDate = LocalDate.parse("2020-05-15")
    private val symptomsOnsetDate = LocalDate.parse("2020-05-14")
    private val indexCaseExpiryDate = LocalDate.parse("2020-05-23")

    private val contactAndIndexIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = contactCaseExposureDate,
            notificationDate = contactCaseNotificationDate,
            expiryDate = contactCaseExpiryDate,
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        ),
        indexInfo = IndexCase(
            isolationTrigger = SelfAssessment(
                selfAssessmentDate = selfAssessmentDate,
                onsetDate = symptomsOnsetDate
            ),
            testResult = acknowledgedTestResult,
            expiryDate = indexCaseExpiryDate
        )
    )

    private val expectedInitialUserDataState = MyDataState(
        isolationState = IsolationViewState(
            lastDayOfIsolation = selectNewest(contactCaseExpiryDate, indexCaseExpiryDate).minusDays(1),
            contactCaseEncounterDate = contactCaseExposureDate,
            contactCaseNotificationDate = contactCaseNotificationDate,
            indexCaseSymptomOnsetDate = symptomsOnsetDate,
            dailyContactTestingOptInDate = dailyContactTestingOptInDate
        ),
        lastRiskyVenueVisitDate = lastRiskyVenueVisit,
        acknowledgedTestResult = acknowledgedTestResult
    )
}
