package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewIsolationStatusBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.IsolationViewState.Isolating
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_DISABLED
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView.AnimationState.ANIMATION_DISABLED_EN_ENABLED
import uk.nhs.nhsx.covid19.android.app.widgets.IsolationStatusView.AnimationState.ANIMATION_ENABLED_EN_ENABLED
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class IsolationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    PulseAnimationView {

    private val binding = ViewIsolationStatusBinding.inflate(LayoutInflater.from(context), this)

    private var daysToGo = 0

    var animationState = ANIMATION_ENABLED_EN_ENABLED
        set(value) {
            field = value

            with(binding) {

                updateAnimations(
                    context = context,
                    isAnimationEnabled = value == ANIMATION_ENABLED_EN_ENABLED,
                    animatedView = imgCirclePulseAnim,
                    smallAnimatedView = imgCircleSmallPulseAnim,
                )
                imgCircleIsolationStatic.isVisible = when (value) {
                    ANIMATION_DISABLED_EN_ENABLED -> true
                    ANIMATION_ENABLED_EN_ENABLED, ANIMATION_DISABLED_EN_DISABLED -> false
                }
            }
        }

    fun initialize(isolation: Isolating, currentDate: LocalDate) = with(binding) {
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

    enum class AnimationState {
        ANIMATION_ENABLED_EN_ENABLED, ANIMATION_DISABLED_EN_ENABLED, ANIMATION_DISABLED_EN_DISABLED
    }
}
