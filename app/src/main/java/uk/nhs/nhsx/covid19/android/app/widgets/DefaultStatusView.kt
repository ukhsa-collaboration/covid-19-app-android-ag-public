package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.provider.Settings.Global
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_default_state.view.imgCirclePulseAnim
import kotlinx.android.synthetic.main.view_default_state.view.imgCircleSmallPulseAnim
import uk.nhs.nhsx.covid19.android.app.R

class DefaultStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var isAnimationEnabled = false
        set(value) {
            field = value
            if (isAnimationEnabled) startAnimations() else stopAnimations()
        }

    init {
        initializeViews()
    }

    private fun initializeViews() {
        LayoutInflater.from(context).inflate(R.layout.view_default_state, this, true)

        startAnimations()
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
