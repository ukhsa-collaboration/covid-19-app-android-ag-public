package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

@Deprecated("Use SubmitAnalytics, this is only for migration")
class AnalyticsAggregatorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("analytics aggregator triggered")
    }
}
