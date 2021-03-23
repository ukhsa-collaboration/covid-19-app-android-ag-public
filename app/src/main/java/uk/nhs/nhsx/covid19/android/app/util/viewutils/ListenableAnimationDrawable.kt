package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Handler

class ListenableAnimationDrawable(animationDrawable: AnimationDrawable, private val onAnimationFinish: () -> Unit) : AnimationDrawable() {

    init {
        /* Add each frame to our animation drawable */
        for (i in 0 until animationDrawable.numberOfFrames) {
            addFrame(animationDrawable.getFrame(i), animationDrawable.getDuration(i))
        }
    }

    final override fun addFrame(frame: Drawable, duration: Int) {
        super.addFrame(frame, duration)
    }

    private val animationHandler: Handler = Handler()

    override fun start() {
        super.start()
        animationHandler.postDelayed(onAnimationFinish, totalDuration.toLong())
    }

    override fun stop() {
        super.stop()
        animationHandler.removeCallbacks(onAnimationFinish)
    }

    /**
     * Gets the total duration of all frames.
     *
     * @return The total duration.
     */
    private val totalDuration: Int by lazy {
        var duration = 0
        for (i in 0 until this.numberOfFrames) {
            duration += getDuration(i)
        }
        duration
    }
}
