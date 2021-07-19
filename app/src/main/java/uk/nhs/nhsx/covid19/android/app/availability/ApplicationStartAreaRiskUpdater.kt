package uk.nhs.nhsx.covid19.android.app.availability

import androidx.annotation.VisibleForTesting
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Success
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.APPLICATION_SCOPE
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWork
import uk.nhs.nhsx.covid19.android.app.util.minutesUntilNow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApplicationStartAreaRiskUpdater(
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val downloadRiskyPostCodesWork: DownloadRiskyPostCodesWork,
    private val downloadRiskyPostCodesScope: CoroutineScope,
    private val downloadRiskyPostCodesDispatcher: CoroutineDispatcher,
    private val clock: Clock
) {

    @Inject
    constructor(
        onboardingCompletedProvider: OnboardingCompletedProvider,
        appAvailabilityProvider: AppAvailabilityProvider,
        downloadRiskyPostCodesWork: DownloadRiskyPostCodesWork,
        @Named(APPLICATION_SCOPE) downloadRiskyPostCodesScope: CoroutineScope,
        clock: Clock
    ) : this(
        onboardingCompletedProvider,
        appAvailabilityProvider,
        downloadRiskyPostCodesWork,
        downloadRiskyPostCodesScope,
        downloadRiskyPostCodesDispatcher = Dispatchers.IO,
        clock
    )

    @VisibleForTesting
    internal var lastUpdated: Instant? = null

    fun updateIfNecessary() {
        if (shouldUpdateAreaRisk()) {
            downloadRiskyPostCodesScope.launch(downloadRiskyPostCodesDispatcher) {
                when (downloadRiskyPostCodesWork()) {
                    is Success -> {
                        lastUpdated = Instant.now(clock)
                        Timber.d("Updating area risk information successful at $lastUpdated")
                    }
                    is Failure -> Timber.d("Updating area risk information failed")
                }
            }
        }
    }

    private fun shouldUpdateAreaRisk(): Boolean =
        onboardingCompletedProvider.value == true &&
            appAvailabilityProvider.isAppAvailable() &&
            isAreaRiskInformationOutdated()

    private fun isAreaRiskInformationOutdated(): Boolean =
        lastUpdated?.let {
            it.minutesUntilNow(clock) >= UPDATE_AREA_RISK_AFTER_LAST_UPDATED_MINUTES
        } ?: true

    companion object {
        private const val UPDATE_AREA_RISK_AFTER_LAST_UPDATED_MINUTES = 10
    }
}
