package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.provider.Settings.Global
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_isolation_status.view.imgCirclePulseAnim
import kotlinx.android.synthetic.main.view_isolation_status.view.imgCircleSmallPulseAnim
import kotlinx.android.synthetic.main.view_isolation_status.view.isolationCountdownView
import kotlinx.android.synthetic.main.view_isolation_status.view.isolationDaysToGo
import kotlinx.android.synthetic.main.view_isolation_status.view.subTitleIsolationCountdown
import kotlinx.android.synthetic.main.view_isolation_status.view.titleDaysToGo
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.toReadableFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class IsolationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var daysToGo = 0

    var isAnimationEnabled = false
        set(value) {
            field = value
            if (isAnimationEnabled) startAnimations() else stopAnimations()
        }

    init {
        initializeViews()
    }

    fun initialize(startDate: Instant, endDate: LocalDate) {
        val totalDurationInDays = ChronoUnit.DAYS.between(
            startDate.truncatedTo(ChronoUnit.DAYS),
            endDate.atStartOfDay(
                ZoneId.systemDefault()
            )
        )
        daysToGo = ChronoUnit.DAYS.between(
            LocalDateTime.now().truncatedTo(ChronoUnit.DAYS),
            endDate.atStartOfDay()
        ).toInt()

        titleDaysToGo.text =
            context.resources.getQuantityString(R.plurals.isolation_days_to_go, daysToGo)

        isolationCountdownView.progress = daysToGo.toFloat()
        isolationCountdownView.progressMax = totalDurationInDays.toFloat()

        val lastIsolationDate = endDate.minusDays(1)
        subTitleIsolationCountdown.text = context.getString(
            R.string.isolation_until_date, lastIsolationDate.toReadableFormat()
        )

        isolationDaysToGo.text = daysToGo.toString()

        contentDescription = context.resources.getQuantityString(
            R.plurals.isolation_view_accessibility_description,
            daysToGo,
            lastIsolationDate.toReadableFormat(),
            daysToGo
        )

        startAnimations()
    }

    private fun initializeViews() {
        LayoutInflater.from(context).inflate(R.layout.view_isolation_status, this, true)
    }

    private fun startAnimations() {
        if (animationsDisabled() || !isAnimationEnabled) {
            return
        }
        val circlePulsatingAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_pulse
        )
        val circlePulsatingAnimationSmall = AnimationUtils.loadAnimation(
            context, R.anim.anim_pulse_small
        )
        imgCirclePulseAnim.startAnimation(circlePulsatingAnimation)
        imgCircleSmallPulseAnim.startAnimation(circlePulsatingAnimationSmall)
    }

    private fun stopAnimations() {
        imgCirclePulseAnim.clearAnimation()
        imgCircleSmallPulseAnim.clearAnimation()
    }

    private fun animationsDisabled(): Boolean {
        val animationDurationScale = Global.getFloat(
            context.contentResolver,
            Global.ANIMATOR_DURATION_SCALE, 1f
        )
        return animationDurationScale == 0.0f
    }
}
