package uk.nhs.nhsx.covid19.android.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventsGroup
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.analytics.CreateAnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.analytics.toAnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityAnalyticsReportBinding
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import javax.inject.Inject

class AnalyticsReportActivity : AppCompatActivity() {

    @Inject
    lateinit var analyticsLogStorage: AnalyticsLogStorage

    @Inject
    lateinit var createAnalyticsPayload: CreateAnalyticsPayload

    @Inject
    lateinit var moshi: Moshi

    private lateinit var binding: ActivityAnalyticsReportBinding

    private var showAsItems = true
    private val indent = "  "

    private val payloadAdapter: JsonAdapter<AnalyticsPayload> by lazy {
        moshi.adapter(AnalyticsPayload::class.java)
    }

    private val entryAdapter: JsonAdapter<List<AnalyticsLogEntry>> by lazy {
        moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                AnalyticsLogEntry::class.java
            ),
            emptySet(), "AnalyticsLogEntry"
        )
    }

    private fun getHeading() =
        if (showAsItems) R.string.analytics_report_title_items else R.string.analytics_report_title_payload

    private suspend fun logAsPayload(formatted: Boolean = false): String {
        val logItems = analyticsLogStorage.value
        val start = logItems.minOf { it.instant }
        val end = logItems.maxOf { it.instant }

        return (if (formatted) payloadAdapter.indent(indent) else payloadAdapter).toJson(
            createAnalyticsPayload.invoke(
                AnalyticsEventsGroup(
                    analyticsWindow = Pair(start, end).toAnalyticsWindow(),
                    entries = analyticsLogStorage.value
                )
            )
        )
    }

    private fun logAsEntryList(formatted: Boolean = false): String =
        (if (formatted) entryAdapter.indent(indent) else entryAdapter).toJson(analyticsLogStorage.value.sortedBy { it.instant })

    private fun updateTitle() {
        setNavigateUpToolbar(binding.primaryToolbar.toolbar, getHeading(), upIndicator = R.drawable.ic_close_white)
    }

    private fun updateDataDump(formatted: Boolean = false) {
        lifecycleScope.launch {
            binding.analyticsData.text = if (showAsItems)
                logAsEntryList(formatted)
            else
                logAsPayload(formatted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityAnalyticsReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateDataDump()
        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_report, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.actionToggleView)
        // The option is indicating a change - that's why this may seem counter-intuitive
        item.title = if (showAsItems) "Display payload" else "Display list"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionExport -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, binding.analyticsData.text)
                sendIntent.type = "text/plain"
                val shareIntent = Intent.createChooser(sendIntent, getString(getHeading()))
                startActivity(shareIntent)
                true
            }
            R.id.actionFormat -> {
                updateDataDump(true)
                true
            }
            R.id.actionToggleView -> {
                showAsItems = !showAsItems
                invalidateOptionsMenu()
                updateTitle()
                updateDataDump()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
