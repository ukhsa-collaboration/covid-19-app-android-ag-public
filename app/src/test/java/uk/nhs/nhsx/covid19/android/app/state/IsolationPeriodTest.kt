package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationPeriod.Companion.mergeNewestOverlapping
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolationPeriodTest {

    //region overlaps
    @Test
    fun `periods overlap when one included in the other one`() {
        val outerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val innerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 11),
            expiryDate = LocalDate.of(2020, 1, 14)
        )

        assertTrue(outerIsolationPeriod.overlaps(innerIsolationPeriod))
        assertTrue(innerIsolationPeriod.overlaps(outerIsolationPeriod))
    }

    @Test
    fun `periods overlap when one ends after the other one starts`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 14)
        )

        assertTrue(newerIsolationPeriod.overlaps(olderIsolationPeriod))
        assertTrue(olderIsolationPeriod.overlaps(newerIsolationPeriod))
    }

    @Test
    fun `periods overlap when one ends on the same day the other one starts`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 10)
        )

        assertTrue(newerIsolationPeriod.overlaps(olderIsolationPeriod))
        assertTrue(olderIsolationPeriod.overlaps(newerIsolationPeriod))
    }

    @Test
    fun `periods do not overlap when one ends before the other one starts`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 8)
        )

        assertFalse(newerIsolationPeriod.overlaps(olderIsolationPeriod))
        assertFalse(olderIsolationPeriod.overlaps(newerIsolationPeriod))
    }
    //endregion

    //region capExpiryDate
    @Test
    fun `do not cap expiry date when period duration is shorter than max isolation`() {
        val isolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 1),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val isolationConfiguration = DurationDays(maxIsolation = 20)

        val expiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)

        assertEquals(isolationPeriod.expiryDate, expiryDate)
    }

    @Test
    fun `do not cap expiry date when period duration equals max isolation`() {
        val isolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 1),
            expiryDate = LocalDate.of(2020, 1, 20)
        )
        val isolationConfiguration = DurationDays(maxIsolation = 20)

        val expiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)

        assertEquals(isolationPeriod.expiryDate, expiryDate)
    }

    @Test
    fun `cap expiry date when period duration exceeds than max isolation`() {
        val isolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 1),
            expiryDate = LocalDate.of(2020, 1, 21)
        )
        val maxIsolation = 20
        val isolationConfiguration = DurationDays(maxIsolation = maxIsolation)

        val expiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)

        val expectedExpiryDate = isolationPeriod.startDate.plusDays(maxIsolation.toLong())
        assertEquals(expectedExpiryDate, expiryDate)
    }
    //endregion

    //region mergeNewestOverlapping
    @Test
    fun `mergeNewestOverlapping returns null for empty list`() {
        val result = mergeNewestOverlapping(listOf())

        assertNull(result)
    }

    @Test
    fun `mergeNewestOverlapping returns unchanged isolation period when only one provided`() {
        val isolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )

        val result = mergeNewestOverlapping(listOf(isolationPeriod))

        assertEquals(isolationPeriod, result)
    }

    @Test
    fun `mergeNewestOverlapping returns outer isolation period when it includes inner`() {
        val outerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val innerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 11),
            expiryDate = LocalDate.of(2020, 1, 14)
        )

        val result = mergeNewestOverlapping(listOf(outerIsolationPeriod, innerIsolationPeriod))

        assertEquals(outerIsolationPeriod, result)
    }

    @Test
    fun `mergeNewestOverlapping returns merged isolation period when periods overlap`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 14)
        )

        val result = mergeNewestOverlapping(listOf(newerIsolationPeriod, olderIsolationPeriod))

        val expected = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 15)
        )

        assertEquals(expected, result)
    }

    @Test
    fun `mergeNewestOverlapping returns newest isolation period when periods do not overlap`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 8)
        )

        val result = mergeNewestOverlapping(listOf(newerIsolationPeriod, olderIsolationPeriod))

        assertEquals(newerIsolationPeriod, result)
    }

    @Test
    fun `mergeNewestOverlapping returns merged newest isolation period when only some periods not overlap`() {
        val newerIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 15)
        )
        val newestIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 15),
            expiryDate = LocalDate.of(2020, 1, 20)
        )
        val olderIsolationPeriod = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 5),
            expiryDate = LocalDate.of(2020, 1, 8)
        )

        val result = mergeNewestOverlapping(listOf(newerIsolationPeriod, olderIsolationPeriod, newestIsolationPeriod))

        val expected = MergedIsolationPeriod(
            startDate = LocalDate.of(2020, 1, 10),
            expiryDate = LocalDate.of(2020, 1, 20)
        )

        assertEquals(expected, result)
    }
    //endregion
}
