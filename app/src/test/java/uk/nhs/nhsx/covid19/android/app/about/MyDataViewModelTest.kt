package uk.nhs.nhsx.covid19.android.app.about

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.IsolationViewState
import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel.MyDataState
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationStateMachineSetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MyDataViewModelTest : IsolationStateMachineSetupHelper {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    override val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    override val clock = Clock.fixed(Instant.parse("2020-05-22T10:00:00Z"), ZoneOffset.UTC)!!

    private val testSubject = MyDataViewModel(
        isolationStateMachine,
        lastVisitedBookTestTypeVenueDateProvider,
        clock
    )

    private val userDataStateObserver = mockk<Observer<MyDataState>>(relaxed = true)
    private val venueVisitsEditModeChangedObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val allUserDataDeletedObserver = mockk<Observer<Unit>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.myDataState().observeForever(userDataStateObserver)

        every { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue } returns LastVisitedBookTestTypeVenueDate(
            lastRiskyVenueVisit,
            RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
        )
    }

    @Test
    fun `onResume triggers view state emission`() = runBlocking {
        givenIsolationState(contactAndIndexIsolation)

        testSubject.onResume()

        verify { userDataStateObserver.onChanged(expectedInitialUserDataState) }
        verify { venueVisitsEditModeChangedObserver wasNot called }
        verify { allUserDataDeletedObserver wasNot called }
    }

    @Test
    fun `onResume with no changes to view state does not trigger view state emission`() = runBlocking {
        givenIsolationState(contactAndIndexIsolation)

        testSubject.onResume()
        testSubject.onResume()

        verify(exactly = 1) { userDataStateObserver.onChanged(any()) }
        verify { venueVisitsEditModeChangedObserver wasNot called }
        verify { allUserDataDeletedObserver wasNot called }
    }

    @Test
    fun `loading user data only returns main post code when local authority is not stored`() {
        givenIsolationState(contactAndIndexIsolation)

        testSubject.onResume()

        verify { venueVisitsEditModeChangedObserver wasNot called }
        verify { allUserDataDeletedObserver wasNot called }
    }

    @Test
    fun `loading user data returns exposure notification details and opt-out date when previously in contact case`() {
        val contactExposureDate = contactCaseExposureDate.minusDays(12)
        val contactNotificationDate = contactCaseNotificationDate.minusDays(12)
        val optOutOfContactIsolationDate = optOutOfContactIsolationDate.minusDays(12)
        val indexSelfAssessmentDate = selfAssessmentDate.minusDays(12)
        val indexSymptomsOnsetDate = symptomsOnsetDate.minusDays(12)

        givenIsolationState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = Contact(
                    exposureDate = contactExposureDate,
                    notificationDate = contactNotificationDate,
                    optOutOfContactIsolation = OptOutOfContactIsolation(optOutOfContactIsolationDate)
                ),
                selfAssessment = SelfAssessment(
                    selfAssessmentDate = indexSelfAssessmentDate,
                    onsetDate = indexSymptomsOnsetDate
                ),
                testResult = acknowledgedTestResult
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
                        optOutOfContactIsolationDate = optOutOfContactIsolationDate
                    ),
                    lastRiskyVenueVisitDate = lastRiskyVenueVisit,
                    acknowledgedTestResult = acknowledgedTestResult
                )
            )
        }
    }

    @Test
    fun `loading user data doesn't return exposure notification details when previously in contact case`() {
        givenIsolationState(IsolationState(isolationConfiguration = IsolationConfiguration()))

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

    private val lastRiskyVenueVisit = LocalDate.of(2020, 8, 12)

    private val acknowledgedTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(clock),
        testResult = POSITIVE,
        acknowledgedDate = LocalDate.now(clock),
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false,
        confirmedDate = null
    )

    private val contactCaseExposureDate = LocalDate.parse("2020-05-19")
    private val contactCaseNotificationDate = LocalDate.parse("2020-05-20")
    private val optOutOfContactIsolationDate = LocalDate.parse("2020-05-21")
    private val selfAssessmentDate = LocalDate.parse("2020-05-15")
    private val symptomsOnsetDate = LocalDate.parse("2020-05-14")
    private val lastDayOfIsolation = LocalDate.parse("2020-05-24")

    private val contactAndIndexIsolation = IsolationState(
        isolationConfiguration = IsolationConfiguration(),
        contact = Contact(
            exposureDate = contactCaseExposureDate,
            notificationDate = contactCaseNotificationDate,
            optOutOfContactIsolation = OptOutOfContactIsolation(optOutOfContactIsolationDate)
        ),
        selfAssessment = SelfAssessment(
            selfAssessmentDate = selfAssessmentDate,
            onsetDate = symptomsOnsetDate
        ),
        testResult = acknowledgedTestResult
    )

    private val expectedInitialUserDataState = MyDataState(
        isolationState = IsolationViewState(
            lastDayOfIsolation = lastDayOfIsolation,
            contactCaseEncounterDate = contactCaseExposureDate,
            contactCaseNotificationDate = contactCaseNotificationDate,
            indexCaseSymptomOnsetDate = symptomsOnsetDate,
            optOutOfContactIsolationDate = optOutOfContactIsolationDate,
        ),
        lastRiskyVenueVisitDate = lastRiskyVenueVisit,
        acknowledgedTestResult = acknowledgedTestResult
    )
}
