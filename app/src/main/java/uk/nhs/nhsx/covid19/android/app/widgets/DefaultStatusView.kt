package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_default_state.view.imgCirclePulseAnim
import kotlinx.android.synthetic.main.view_default_state.view.imgCircleSmallPulseAnim
import uk.nhs.nhsx.covid19.android.app.R

class DefaultStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    PulseAnimationView {

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
        LayoutInflater.from(context).inflate(R.layout.view_default_state, this, true)
    }
}
