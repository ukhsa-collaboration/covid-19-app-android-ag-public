package uk.nhs.nhsx.covid19.android.app.packagemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

interface PackageManager {

    fun resolveActivity(context: Context, intent: Intent, flags: Int): ResolveInfo?
}
