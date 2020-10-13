package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import android.provider.Settings.Global

fun animationsDisabled(context: Context): Boolean {
    val animationDurationScale = Global.getFloat(
        context.contentResolver,
        Global.ANIMATOR_DURATION_SCALE, 1f
    )
    return animationDurationScale == 0.0f
}
