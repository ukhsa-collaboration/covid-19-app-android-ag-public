package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_isolation_status.view.imgCirclePulseAnim
import kotlinx.android.synthetic.main.view_isolation_status.view.imgCircleSmallPulseAnim
import kotlinx.android.synthetic.main.view_isolation_status.view.isolationCountdownView
import kotlinx.android.synthetic.main.view_isolation_status.view.isolationDaysToGo
import kotlinx.android.synthetic.main.view_isolation_status.view.subTitleIsolationCountdown
import kotlinx.android.synthetic.main.view_isolation_status.view.titleDaysToGo
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class IsolationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    PulseAnimationView {

    private var daysToGo = 0

    var isAnimationEnabled = false
        set(value) {
            field = value
            updateAnimations(
                context = context,
                isAnimationEnabled = isAnimationEnabled,
                animatedView = imgCirclePulseAnim,
                smallAnimatedView = imgCircleSmallPulseAnim
            )
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_isolation_status, this, true)
    }

    fun initialize(isolation: Isolating, currentDate: LocalDate) {
        setUpAccessibilityHeading()

        val totalDurationInDays = ChronoUnit.DAYS.between(
            isolation.isolationStart,
            isolation.expiryDate
        )
        daysToGo = ChronoUnit.DAYS.between(currentDate, isolation.expiryDate).toInt()

        titleDaysToGo.text =
            context.resources.getQuantityString(R.plurals.isolation_days_to_go, daysToGo, daysToGo)

        isolationCountdownView.progress = daysToGo.toFloat()
        isolationCountdownView.progressMax = totalDurationInDays.toFloat()

        val lastDayOfIsolation = isolation.expiryDate.minusDays(1)
        subTitleIsolationCountdown.text = context.getString(
            R.string.isolation_until_date, lastDayOfIsolation.uiFormat(context)
        )

        isolationDaysToGo.text = daysToGo.toString()

        contentDescription = context.resources.getQuantityString(
            R.plurals.isolation_view_accessibility_description,
            daysToGo,
            lastDayOfIsolation.uiFormat(context),
            daysToGo
        )
    }
}
