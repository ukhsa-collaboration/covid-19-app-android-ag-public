package uk.nhs.nhsx.covid19.android.app.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsFilterProvider @Inject constructor(private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider) {
    suspend operator fun invoke(): AnalyticsFilter = withContext(Dispatchers.IO) {
        return@withContext AnalyticsFilter(
            shouldFilterRiskyContactInfo = shouldFilterRiskyContactEvents(),
            shouldFilterSelfIsolation = shouldFilterSelfIsolationHubEvents(),
            shouldFilterVenueCheckIn = shouldFilterVenueCheckInEvents(),
            enabledCustomAnalyticsFilters = getCustomAnalyticsFilters()
        )
    }

    private suspend fun shouldFilterRiskyContactEvents(): Boolean {
        return when (localAuthorityPostCodeProvider.getPostCodeDistrict()) {
            WALES -> !RuntimeBehavior.isFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW)
            ENGLAND -> !RuntimeBehavior.isFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW)
            else -> false
        }
    }

    private suspend fun shouldFilterSelfIsolationHubEvents(): Boolean {
        return when (localAuthorityPostCodeProvider.getPostCodeDistrict()) {
            WALES -> !RuntimeBehavior.isFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES)
            ENGLAND -> !RuntimeBehavior.isFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND)
            else -> false
        }
    }

    private fun shouldFilterVenueCheckInEvents(): Boolean {
        return !RuntimeBehavior.isFeatureEnabled(VENUE_CHECK_IN_BUTTON)
    }

    private fun getCustomAnalyticsFilters(): List<CustomAnalyticsFilter> {
        return CUSTOM_ANALYTICS_FILTERS
    }

    companion object {
        private val CUSTOM_ANALYTICS_FILTERS = listOf<CustomAnalyticsFilter>()
    }
}
