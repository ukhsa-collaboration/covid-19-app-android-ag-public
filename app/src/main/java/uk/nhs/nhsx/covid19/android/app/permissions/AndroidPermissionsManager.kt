package uk.nhs.nhsx.covid19.android.app.permissions

import android.app.Activity
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AndroidPermissionsManager : PermissionsManager {

    override fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    override fun checkSelfPermission(context: Context, permission: String): Int {
        return ContextCompat.checkSelfPermission(context, permission)
    }
}
