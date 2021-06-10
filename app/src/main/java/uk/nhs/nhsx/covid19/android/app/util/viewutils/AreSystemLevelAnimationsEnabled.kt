package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import javax.inject.Inject

class AreSystemLevelAnimationsEnabled @Inject constructor(private val context: Context) {
    operator fun invoke() = !animationsDisabled(context)
}
