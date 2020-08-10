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
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.HighRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.LowRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.MediumRisk

class StatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val riskyPostCodeDetectedProvider = mockk<RiskyPostCodeDetectedProvider>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)

    private val areaRiskStateObserver = mockk<Observer<RiskyPostCodeViewState>>(relaxed = true)

    private val testSubject =
        StatusViewModel(
            postCodeProvider,
            riskyPostCodeDetectedProvider,
            sharedPreferences,
            isolationStateMachine,
            userInbox,
            periodicTasks,
            notificationProvider
        )

    @Before
    fun setUp() {
        postCodeProvider.value = "A1"
        testSubject.areaRiskState().observeForever(areaRiskStateObserver)
    }

    @After
    fun tearDown() {
        testSubject.areaRiskState().removeObserver(areaRiskStateObserver)
    }

    @Test
    fun `area risk changed from high to low`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(LOW)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(LowRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk changed from high to medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(MediumRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk changed from low to high`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(HIGH)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(HighRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk changed from low to medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(MediumRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk did not change and is low`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(LOW)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(LowRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk did not change and is medium`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(MediumRisk(postCodeProvider.value)) }
    }

    @Test
    fun `area risk did not change and is high`() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(HIGH)

        testSubject.updateAreaRisk()

        verify { areaRiskStateObserver.onChanged(HighRisk(postCodeProvider.value)) }
    }

    @Test
    fun `on resume starts periodic tasks`() {
        testSubject.onResume()

        verify { periodicTasks.schedule(keepPrevious = true) }
    }
}
