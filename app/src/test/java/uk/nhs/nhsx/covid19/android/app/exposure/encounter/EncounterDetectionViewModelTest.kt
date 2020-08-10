package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import java.time.Instant
import java.time.LocalDate

class EncounterDetectionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)

    private val resultObserver = mockk<Observer<ExposedNotificationResult>>(
        relaxed = true
    )

    private val testSubject = EncounterDetectionViewModel(isolationStateMachine, userInbox)

    @Before
    fun setUp() {
        testSubject.isolationState().observeForever(resultObserver)
    }

    @Test
    fun `provide the days of isolation left when in contact case`() {

        every { isolationStateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(),
            expiryDate = LocalDate.now().plusDays(14),
            contactCase = ContactCase(startDate = Instant.now())
        )

        testSubject.getIsolationDays()

        verify { resultObserver.onChanged(ExposedNotificationResult.IsolationDurationDays(14)) }
    }

    @Test
    fun `confirm consent`() {
        testSubject.confirmConsent()

        verify { userInbox.clearItem(ShowEncounterDetection) }

        verify { resultObserver.onChanged(ExposedNotificationResult.ConsentConfirmation) }
    }
}
