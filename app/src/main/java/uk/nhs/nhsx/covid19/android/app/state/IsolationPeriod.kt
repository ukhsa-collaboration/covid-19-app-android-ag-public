package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import uk.nhs.nhsx.covid19.android.app.util.selectNewest
import java.time.Clock
import java.time.LocalDate

interface IsolationPeriod {
    val startDate: LocalDate
    val expiryDate: LocalDate
    fun hasExpired(clock: Clock) = expiryDate.isBeforeOrEqual(LocalDate.now(clock))

    fun overlaps(other: IsolationPeriod): Boolean =
        this.startDate.isBeforeOrEqual(other.expiryDate) &&
            this.expiryDate.isEqualOrAfter(other.startDate)

    fun capExpiryDate(isolationConfiguration: DurationDays): LocalDate {
        val maxExpiryDate = startDate.plusDays(isolationConfiguration.maxIsolation.toLong())
        return selectEarliest(maxExpiryDate, expiryDate)
    }

    companion object {
        /**
         * Go through all [isolationPeriods], find the newest ones that overlap, and merge those. That is, from those,
         * create a new isolation period where the start date is the minimum of those and the expiry date the maximum
         * of those.
         *
         * This function will only return null if [isolationPeriods] is empty.
         */
        fun mergeNewestOverlapping(isolationPeriods: List<IsolationPeriod>): IsolationPeriod? =
            isolationPeriods.reduceOrNull { latestPeriod, period ->
                MergedIsolationPeriod(
                    startDate =
                        if (latestPeriod.overlaps(period)) selectEarliest(latestPeriod.startDate, period.startDate)
                        else selectNewest(latestPeriod.startDate, period.startDate),
                    expiryDate = selectNewest(latestPeriod.expiryDate, period.expiryDate)
                )
            }
    }
}

data class MergedIsolationPeriod(
    override val startDate: LocalDate,
    override val expiryDate: LocalDate
) : IsolationPeriod
