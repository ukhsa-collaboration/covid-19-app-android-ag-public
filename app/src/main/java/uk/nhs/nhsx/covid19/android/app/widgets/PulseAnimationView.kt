package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import uk.nhs.nhsx.covid19.android.app.R

interface PulseAnimationView {

    fun updateAnimations(
        context: Context,
        isAnimationEnabled: Boolean,
        animatedView: View,
        smallAnimatedView: View
    ) {
        if (!isAnimationEnabled) {
            animatedView.clearAnimation()
            smallAnimatedView.clearAnimation()
            return
        }
        val circlePulsatingAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_pulse
        )
        val circlePulsatingAnimationSmall = AnimationUtils.loadAnimation(
            context, R.anim.anim_pulse_small
        )
        animatedView.startAnimation(circlePulsatingAnimation)
        smallAnimatedView.startAnimation(circlePulsatingAnimationSmall)
    }
}
