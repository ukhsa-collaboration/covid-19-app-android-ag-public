package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class AnalyticsSubmissionLogStorageTest {

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .build()

    private val analyticsSubmissionLogJsonStorage = mockk<AnalyticsSubmissionLogJsonStorage>(relaxUnitFun = true)
    private val today = LocalDate.of(2020, 10, 9)
    private val fixedClock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val testSubject = AnalyticsSubmissionLogStorage(analyticsSubmissionLogJsonStorage, fixedClock, moshi)

    @Test
    fun `read empty storage returns default set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns null

        assertEquals(defaultSet, testSubject.value)
    }

    @Test
    fun `read corrupted storage returns default set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns "adfjskg"

        assertEquals(defaultSet, testSubject.value)
    }

    @Test
    fun `verify serialization from empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns "[]"
        testSubject.add(localDate1)
        verify { analyticsSubmissionLogJsonStorage.value = serializedSet1 }
    }

    @Test
    fun `verify serialization from non empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1
        testSubject.add(localDate2)
        verify { analyticsSubmissionLogJsonStorage.value = serializedSet1and2 }
    }

    @Test
    fun `verify deserialization`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1and2

        assertEquals(setOfLocalDates, testSubject.value)
    }

    @Test
    fun `verify deletion to non empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1and2

        testSubject.removeBeforeOrEqual(localDate2)

        verify { analyticsSubmissionLogJsonStorage.value = serializedSet2 }
    }

    @Test
    fun `verify deletion to empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1

        testSubject.removeBeforeOrEqual(localDate2)

        verify { analyticsSubmissionLogJsonStorage.value = "[]" }
    }

    private val localDate1 = LocalDate.of(2020, 10, 10)
    private val localDate2 = LocalDate.of(2020, 10, 11)

    private val setOfLocalDates = setOf(
        localDate1, localDate2
    )

    private val serializedSet1 =
        """
            ["2020-10-10"]
        """.trimIndent()

    private val serializedSet1and2 =
        """
            ["2020-10-10","2020-10-11"]
        """.trimIndent()

    private val serializedSet2 =
        """
            ["2020-10-11"]
        """.trimIndent()

    private val defaultSet = setOf<LocalDate>(
        today.minusDays(1),
        today.minusDays(2),
        today.minusDays(3),
        today.minusDays(4),
        today.minusDays(5),
        today.minusDays(6),
        today.minusDays(7),
        today.minusDays(8),
    )
}
