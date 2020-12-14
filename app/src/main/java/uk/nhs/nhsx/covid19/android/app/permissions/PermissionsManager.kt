package uk.nhs.nhsx.covid19.android.app.permissions

import android.app.Activity
import android.content.Context

interface PermissionsManager {

    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int)

    fun checkSelfPermission(context: Context, permission: String): Int
}
