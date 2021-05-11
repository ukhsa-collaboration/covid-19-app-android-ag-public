package uk.nhs.nhsx.covid19.android.app.widgets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.scenarios.view_offset_days.view.currentAppDate
import kotlinx.android.synthetic.scenarios.view_offset_days.view.currentOffset
import kotlinx.android.synthetic.scenarios.view_offset_days.view.decreaseDays
import kotlinx.android.synthetic.scenarios.view_offset_days.view.increaseDays
import kotlinx.android.synthetic.scenarios.view_offset_days.view.resetDays
import uk.nhs.nhsx.covid19.android.app.DebugActivity
import uk.nhs.nhsx.covid19.android.app.DebugActivity.Companion
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.DateChangeReceiver
import uk.nhs.nhsx.covid19.android.app.status.ScenariosDateChangeBroadcastReceiver
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

class OffsetDaysView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var offsetDaysChangedListener: OnOffsetDaysChangedListener? = null

    private var dateChangeReceiver: DateChangeReceiver? = null

    private lateinit var debugSharedPreferences: SharedPreferences

    init {
        View.inflate(context, R.layout.view_offset_days, this)
        orientation = VERTICAL

        getActivity()?.let {
            debugSharedPreferences = it.getSharedPreferences(Companion.DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

            updateTextViews(getOffsetDays())

            initializeViews()
        }
    }

    private fun initializeViews() {
        /* ktlint-disable */
        decreaseDays.setOnClickListener {
            setOffsetDays(getOffsetDays() - 1)
        }
        increaseDays.setOnClickListener {
            setOffsetDays(getOffsetDays() + 1)
        }
        resetDays.setOnClickListener {
            setOffsetDays(0)
        }
        /* ktlint-enable */
    }

    private fun getOffsetDays() = debugSharedPreferences.getLong(DebugActivity.OFFSET_DAYS, 0L)

    private fun setOffsetDays(offsetDays: Long) {
        debugSharedPreferences.edit().putLong(DebugActivity.OFFSET_DAYS, offsetDays).apply()
        updateTextViews(offsetDays)
        offsetDaysChangedListener?.offsetChanged(offsetDays)
        (dateChangeReceiver as? ScenariosDateChangeBroadcastReceiver)?.trigger()
    }

    private fun updateTextViews(offsetDays: Long) {
        currentOffset.text = offsetDays.toString()
        currentAppDate.text = "Current app date: ${computeAppDate(offsetDays)}"
    }

    private fun computeAppDate(offsetDays: Long) =
        LocalDate.now(Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(offsetDays)))

    fun setListener(listener: OnOffsetDaysChangedListener) {
        this.offsetDaysChangedListener = listener
    }

    fun setDateChangeReceiver(dateChangeReceiver: DateChangeReceiver) {
        this.dateChangeReceiver = dateChangeReceiver
    }

    private fun getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    interface OnOffsetDaysChangedListener {
        fun offsetChanged(offsetDays: Long)
    }
}
