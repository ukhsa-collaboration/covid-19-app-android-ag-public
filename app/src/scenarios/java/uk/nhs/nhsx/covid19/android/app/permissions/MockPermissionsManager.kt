package uk.nhs.nhsx.covid19.android.app.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

class MockPermissionsManager : PermissionsManager {

    private val permissionResponses = mutableMapOf<String, Int>()

    override fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        activity.onRequestPermissionsResult(
            requestCode,
            permissions,
            permissions.map { permission -> permissionResponses[permission] ?: PackageManager.PERMISSION_GRANTED }.toIntArray()
        )
    }

    override fun checkSelfPermission(context: Context, permission: String): Int =
        permissionResponses[permission] ?: PackageManager.PERMISSION_DENIED

    fun setResponseForPermissionRequest(permission: String, response: Int) {
        permissionResponses[permission] = response
    }

    fun clear() {
        permissionResponses.clear()
    }
}
