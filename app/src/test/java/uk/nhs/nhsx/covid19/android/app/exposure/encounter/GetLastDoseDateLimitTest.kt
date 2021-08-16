package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate
import kotlin.test.assertEquals

class GetLastDoseDateLimitTest {
    private val exposureDate = LocalDate.of(2021, 7, 20)
    private val mockIsolationStateMachine: IsolationStateMachine = mockk()

    private val getLastDoseDateLimit = GetLastDoseDateLimit(mockIsolationStateMachine)

    @Test
    fun `returns formatted string from 14 whole days back from date of encounter`() {
        val expected = LocalDate.of(2021, 7, 5)
        every { mockIsolationStateMachine.readState() } returns isolationStateWithContactCase

        assertEquals(expected, getLastDoseDateLimit())
    }

    @Test
    fun `throws exception if there is no contact case encounter date`() {
        every { mockIsolationStateMachine.readState() } returns emptyIsolationState

        assertThrows<IllegalStateException> {
            getLastDoseDateLimit()
        }
    }

    private val isolationStateWithContactCase = IsolationState(
        isolationConfiguration = mockk(),
        contactCase = ContactCase(
            exposureDate = exposureDate,
            notificationDate = mockk(),
            optOutOfContactIsolation = mockk(),
            expiryDate = mockk()
        )
    )

    private val emptyIsolationState = IsolationState(isolationConfiguration = mockk())
}
