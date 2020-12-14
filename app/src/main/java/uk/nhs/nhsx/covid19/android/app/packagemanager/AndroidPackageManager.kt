package uk.nhs.nhsx.covid19.android.app.packagemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

class AndroidPackageManager : PackageManager {

    override fun resolveActivity(context: Context, intent: Intent, flags: Int): ResolveInfo? =
        context.packageManager.resolveActivity(intent, flags)
}
