package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider

class StatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val riskyPostCodeDetectedProvider = mockk<AreaRiskLevelProvider>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val districtAreaUrlProvider = mockk<DistrictAreaStringProvider>(relaxed = true)
    private val startAppReviewFlowConstraint = mockk<ShouldShowInAppReview>(relaxed = true)
    private val lastReviewFlowStartedDateProvider =
        mockk<LastAppRatingStartedDateProvider>(relaxed = true)

    private val areaRiskStateObserver = mockk<Observer<RiskyPostCodeViewState>>(relaxed = true)

    private val testSubject =
        StatusViewModel(
            postCodeProvider,
            riskyPostCodeDetectedProvider,
            sharedPreferences,
            isolationStateMachine,
            userInbox,
            periodicTasks,
            notificationProvider,
            districtAreaUrlProvider,
            startAppReviewFlowConstraint,
            lastReviewFlowStartedDateProvider
        )

    private val lowRisk = Risk("A1", 0, 0, LOW)
    private val mediumRisk = Risk("A1", 0, 0, MEDIUM)
    private val highRisk = Risk("A1", 0, 0, HIGH)

    @Before
    fun setUp() {
        every { postCodeProvider.value } returns "A1"
        testSubject.onAreaRiskLevelChanged().observeForever(areaRiskStateObserver)
    }

    @After
    fun tearDown() {
        testSubject.onAreaRiskLevelChanged().removeObserver(areaRiskStateObserver)
    }

    @Test
    fun `area risk changed from high to low`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(LOW)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(lowRisk) }
    }

    @Test
    fun `area risk changed from high to medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(mediumRisk) }
    }

    @Test
    fun `area risk changed from low to high`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(HIGH)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(highRisk) }
    }

    @Test
    fun `area risk changed from low to medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(mediumRisk) }
    }

    @Test
    fun `area risk did not change and is low`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(LOW)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(lowRisk) }
    }

    @Test
    fun `area risk did not change and is medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(mediumRisk) }
    }

    @Test
    fun `area risk did not change and is high`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(HIGH)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(highRisk) }
    }

    @Test
    fun `on resume starts periodic tasks`() {
        testSubject.onResume()

        verify { periodicTasks.schedule(keepPrevious = true) }
    }
}
