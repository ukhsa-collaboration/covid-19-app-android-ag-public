package uk.nhs.nhsx.covid19.android.app.analytics

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsSubmissionLogStorage.Companion.ANALYTICS_SUBMISSION_LOG
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.LocalDate

private val today = LocalDate.of(2020, 10, 9)
private val yesterday = today.minusDays(1)

private val AnalyticsSubmissionLogStorage.propertyForTest: Set<LocalDate>
    get() = getLogForAnalyticsWindow(yesterday)

class AnalyticsSubmissionLogStorageTest : ProviderTest<AnalyticsSubmissionLogStorage, Set<LocalDate>>() {

    override val getTestSubject = ::AnalyticsSubmissionLogStorage
    override val property = AnalyticsSubmissionLogStorage::propertyForTest
    override val key = ANALYTICS_SUBMISSION_LOG
    override val defaultValue: Set<LocalDate> = defaultSet
    override val expectations: List<ProviderTestExpectation<Set<LocalDate>>> = listOf(
        ProviderTestExpectation(json = serializedSet1and2, objectValue = setOfLocalDates, direction = JSON_TO_OBJECT)
    )

    @Test
    fun `verify serialization from empty set`() {
        sharedPreferencesReturns("[]")

        testSubject.addDate(localDate1)

        assertSharedPreferenceSetsValue(serializedSet1)
    }

    @Test
    fun `verify serialization from non empty set`() {
        sharedPreferencesReturns(serializedSet1)

        testSubject.addDate(localDate2)

        assertSharedPreferenceSetsValue(serializedSet1and2)
    }

    @Test
    fun `verify deletion to non empty set`() {
        sharedPreferencesReturns(serializedSet1and2)

        testSubject.removeBeforeOrEqual(localDate2)

        assertSharedPreferenceSetsValue(serializedSet2)
    }

    @Test
    fun `verify deletion to empty set`() {
        sharedPreferencesReturns(serializedSet1)

        testSubject.removeBeforeOrEqual(localDate2)

        assertSharedPreferenceSetsValue("[]")
    }

    companion object {
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
            yesterday.minusDays(1),
            yesterday.minusDays(2),
            yesterday.minusDays(3),
            yesterday.minusDays(4),
            yesterday.minusDays(5),
            yesterday.minusDays(6),
            yesterday.minusDays(7)
        )
    }
}
