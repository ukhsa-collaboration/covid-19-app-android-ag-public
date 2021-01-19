package uk.nhs.nhsx.covid19.android.app.common

import android.os.Build.VERSION
import javax.inject.Inject

class BuildVersionProvider @Inject constructor() {
    fun version() = VERSION.SDK_INT
}
