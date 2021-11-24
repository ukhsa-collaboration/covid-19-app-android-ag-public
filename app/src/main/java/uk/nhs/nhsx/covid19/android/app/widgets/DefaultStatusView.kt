package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.databinding.ViewDefaultStateBinding

class DefaultStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    PulseAnimationView {

    private val binding = ViewDefaultStateBinding.inflate(LayoutInflater.from(context), this)

    var isAnimationEnabled = false
        set(value) {
            field = value

            with(binding) {

                updateAnimations(
                    context = context,
                    isAnimationEnabled = isAnimationEnabled,
                    animatedView = imgCirclePulseAnim,
                    smallAnimatedView = imgCircleSmallPulseAnim,
                )
                imgCircleStatic.isVisible = !isAnimationEnabled
            }
        }

    fun focusOnActiveLabel() = binding.contactTracingActiveLabel.performAccessibilityAction(
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null
    )
}
