package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmissionDateRangeTest {

    @Test
    fun `test SubmissionDateRange`() {
        val testSubject = SubmissionDateRange(
            firstSubmissionDate = LocalDate.of(2021, 4, 1),
            lastSubmissionDate = LocalDate.of(2021, 4, 6)
        )

        assertTrue { testSubject.containsAtLeastOneDay() }
        assertFalse { testSubject.includes(LocalDate.of(2021, 3, 31)) }
        assertTrue { testSubject.includes(LocalDate.of(2021, 4, 1)) }
        assertTrue { testSubject.includes(LocalDate.of(2021, 4, 4)) }
        assertTrue { testSubject.includes(LocalDate.of(2021, 4, 6)) }
        assertFalse { testSubject.includes(LocalDate.of(2021, 4, 7)) }
    }
}
