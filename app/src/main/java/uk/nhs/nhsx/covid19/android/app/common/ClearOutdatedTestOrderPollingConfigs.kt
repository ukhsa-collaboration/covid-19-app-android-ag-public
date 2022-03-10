package uk.nhs.nhsx.covid19.android.app.common

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class ClearOutdatedTestOrderPollingConfigs @Inject constructor(
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val getLatestConfiguration: GetLatestConfiguration,
    private val clock: Clock
) {
    operator fun invoke() {
        testOrderingTokensProvider.removeAll { isExpired() }
        Timber.d("Stored test result polling tokens: ${testOrderingTokensProvider.configs}")
    }

    private fun TestOrderPollingConfig.isExpired(): Boolean {
        val retentionPeriod = getLatestConfiguration().testResultPollingTokenRetentionPeriod
        val expiryDate = startedAt.truncatedTo(DAYS).plus(retentionPeriod.toLong(), DAYS)
        val now = Instant.now(clock)
        return now.isAfter(expiryDate)
    }
}
