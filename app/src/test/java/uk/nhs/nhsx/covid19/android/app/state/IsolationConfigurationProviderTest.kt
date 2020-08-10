package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import kotlin.test.assertEquals

class IsolationConfigurationProviderTest {

    private val moshi = Moshi.Builder().build()

    private val isolationConfigurationJsonProvider =
        mockk<IsolationConfigurationJsonProvider>(relaxed = true)

    private val testSubject = IsolationConfigurationProvider(
        isolationConfigurationJsonProvider,
        moshi
    )

    @Test
    fun `verify serialization`() {
        every { isolationConfigurationJsonProvider.durationJson } returns durationDaysJson

        val parsedDurationDays = testSubject.durationDays

        assertEquals(durationDays, parsedDurationDays)
    }

    @Test
    fun `verify serialization without housekeeping period`() {
        every { isolationConfigurationJsonProvider.durationJson } returns durationDaysJsonWithoutHousekeepingPeriod

        val parsedDurationDays = testSubject.durationDays

        assertEquals(durationDaysDefaultHousekeepingPeriod, parsedDurationDays)
    }

    @Test
    fun `verify deserialization`() {

        testSubject.durationDays = durationDays

        verify { isolationConfigurationJsonProvider.durationJson = durationDaysJson }
    }

    @Test
    fun `on exception will return default values`() {
        every { isolationConfigurationJsonProvider.durationJson } returns """wrong_format"""

        val parsedDurationDays = testSubject.durationDays

        assertEquals(DurationDays(), parsedDurationDays)
    }

    private val durationDays = DurationDays(
        contactCase = 32,
        indexCaseSinceSelfDiagnosisOnset = 7,
        indexCaseSinceSelfDiagnosisUnknownOnset = 5,
        maxIsolation = 21,
        pendingTasksRetentionPeriod = 8
    )

    private val durationDaysJson =
        """{"contactCase":32,"indexCaseSinceSelfDiagnosisOnset":7,"indexCaseSinceSelfDiagnosisUnknownOnset":5,"maxIsolation":21,"pendingTasksRetentionPeriod":8}"""

    private val durationDaysDefaultHousekeepingPeriod = DurationDays(
        contactCase = 32,
        indexCaseSinceSelfDiagnosisOnset = 7,
        indexCaseSinceSelfDiagnosisUnknownOnset = 5,
        maxIsolation = 21,
        pendingTasksRetentionPeriod = 14
    )

    private val durationDaysJsonWithoutHousekeepingPeriod =
        """{"contactCase":32,"indexCaseSinceSelfDiagnosisOnset":7,"indexCaseSinceSelfDiagnosisUnknownOnset":5,"maxIsolation":21}"""
}
