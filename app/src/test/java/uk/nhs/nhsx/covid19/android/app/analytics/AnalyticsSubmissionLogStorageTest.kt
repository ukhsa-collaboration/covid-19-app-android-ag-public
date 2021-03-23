package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.LocalDate
import kotlin.test.assertEquals

class AnalyticsSubmissionLogStorageTest {

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .build()
    private val analyticsSubmissionLogJsonStorage = mockk<AnalyticsSubmissionLogJsonStorage>(relaxUnitFun = true)

    private val testSubject = AnalyticsSubmissionLogStorage(analyticsSubmissionLogJsonStorage, moshi)

    @Test
    fun `read empty storage returns default set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns null

        assertEquals(defaultSet, testSubject.getLogForAnalyticsWindow(yesterday))
    }

    @Test
    fun `read corrupted storage returns default set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns "adfjskg"

        assertEquals(defaultSet, testSubject.getLogForAnalyticsWindow(yesterday))
    }

    @Test
    fun `verify serialization from empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns "[]"
        testSubject.addDate(localDate1)
        verify { analyticsSubmissionLogJsonStorage.value = serializedSet1 }
    }

    @Test
    fun `verify serialization from non empty set`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1
        testSubject.addDate(localDate2)
        verify { analyticsSubmissionLogJsonStorage.value = serializedSet1and2 }
    }

    @Test
    fun `verify deserialization`() {
        every { analyticsSubmissionLogJsonStorage.value } returns serializedSet1and2

        assertEquals(setOfLocalDates, testSubject.getLogForAnalyticsWindow(yesterday))
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

    private val today = LocalDate.of(2020, 10, 9)
    private val yesterday = today.minusDays(1)

    private val defaultSet = setOf<LocalDate>(
        yesterday.minusDays(1),
        yesterday.minusDays(2),
        yesterday.minusDays(3),
        yesterday.minusDays(4),
        yesterday.minusDays(5),
        yesterday.minusDays(6),
        yesterday.minusDays(7)
    )
}
