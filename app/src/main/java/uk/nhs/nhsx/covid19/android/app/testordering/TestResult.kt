package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.LocalDate

interface TestResult {
    val testKitType: VirologyTestKitType?
    val confirmatoryDayLimit: Int?

    fun testEndDate(clock: Clock): LocalDate

    fun isPositive(): Boolean

    fun isNegative(): Boolean

    fun isOlderThan(otherTest: TestResult, clock: Clock): Boolean =
        testEndDate(clock).isBefore(otherTest.testEndDate(clock))

    fun isDateWithinConfirmatoryDayLimit(date: LocalDate, clock: Clock): Boolean {
        val dayLimit = confirmatoryDayLimit?.toLong()

        return when {
            dayLimit == null -> true
            dayLimit < 0 -> false
            else -> {
                val latestConfirmationDate = testEndDate(clock).plusDays(dayLimit)
                date.isEqualOrAfter(testEndDate(clock)) && date.isBeforeOrEqual(latestConfirmationDate)
            }
        }
    }
}
