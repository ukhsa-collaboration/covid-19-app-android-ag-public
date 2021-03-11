package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.GLOBAL_SCOPE
import javax.inject.Inject
import javax.inject.Named

class AnalyticsEventTracker @Inject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    @Named(GLOBAL_SCOPE) private val analyticsEventScope: CoroutineScope,
) {

    fun track(analyticsEvent: AnalyticsEvent) {
        analyticsEventScope.launch {
            analyticsEventProcessor.track(analyticsEvent)
        }
    }
}
