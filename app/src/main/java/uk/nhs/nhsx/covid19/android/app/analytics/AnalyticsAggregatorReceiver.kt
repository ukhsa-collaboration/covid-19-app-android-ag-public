package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.appComponent
import javax.inject.Inject

class AnalyticsAggregatorReceiver : BroadcastReceiver() {

    @Inject
    lateinit var aggregateAnalytics: AggregateAnalytics

    @Inject
    lateinit var analyticsAlarm: AnalyticsAlarm

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        GlobalScope.launch {
            aggregateAnalytics.invoke()
            analyticsAlarm.scheduleNextAnalyticsAggregator()
        }
    }
}
