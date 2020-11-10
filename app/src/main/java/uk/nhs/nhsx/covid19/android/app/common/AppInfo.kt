package uk.nhs.nhsx.covid19.android.app.common

import android.os.Build
import uk.nhs.nhsx.covid19.android.app.BuildConfig

data class AppInfo(
    val osVersion: Int = Build.VERSION.SDK_INT,
    val buildNumber: Int = BuildConfig.VERSION_CODE,
    val shortVersionName: String = BuildConfig.VERSION_NAME_SHORT
)
