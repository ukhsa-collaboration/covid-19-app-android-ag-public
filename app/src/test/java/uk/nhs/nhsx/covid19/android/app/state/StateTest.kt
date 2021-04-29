package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StateTest {

    @Test
    fun `get symptomsOnset Date from Default state`() {
        val dateNow = LocalDate.now()
        val onsetDate = dateNow.minusDays(7)
        val state = Default(
            previousIsolation = Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = onsetDate,
                    expiryDate = dateNow.minusDays(2),
                    selfAssessment = false
                )
            )
        )

        assertEquals(onsetDate, state.symptomsOnsetDate)
    }

    @Test
    fun `get symptomsOnset Date from Default without previousIsolation`() {
        val state = Default()

        assertNull(state.symptomsOnsetDate)
    }

    @Test
    fun `get symptomsOnset Date from Isolation state`() {
        val dateNow = LocalDate.now()
        val onsetDate = dateNow.minusDays(4)
        val state = Isolation(
            isolationStart = Instant.now(),
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = onsetDate,
                expiryDate = dateNow.plusDays(2),
                selfAssessment = false
            )
        )

        assertEquals(onsetDate, state.symptomsOnsetDate)
    }

    @Test
    fun `get symptomsOnset Date from Isolation state without index case`() {
        val state = Isolation(
            isolationStart = Instant.now(),
            isolationConfiguration = DurationDays(),
        )

        assertNull(state.symptomsOnsetDate)
    }
}
