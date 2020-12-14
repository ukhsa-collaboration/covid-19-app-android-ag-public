package uk.nhs.nhsx.covid19.android.app.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

class MockPermissionsManager : PermissionsManager {

    private val grantedPermissions = mutableSetOf<String>()
    private val permissionResponses = mutableMapOf<String, Int>()

    override fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        grantedPermissions.addAll(permissions)
        activity.onRequestPermissionsResult(
            requestCode,
            permissions,
            permissions.map { permission -> permissionResponses[permission] ?: PackageManager.PERMISSION_GRANTED }.toIntArray()
        )
    }

    override fun checkSelfPermission(context: Context, permission: String): Int =
        if (grantedPermissions.contains(permission)) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED

    fun addGrantedPermission(permission: String) {
        grantedPermissions.add(permission)
    }

    fun removeGrantedPermission(permission: String) {
        grantedPermissions.remove(permission)
    }

    fun setResponseForPermissionRequest(permission: String, response: Int) {
        permissionResponses[permission] = response
    }

    fun clear() {
        grantedPermissions.clear()
        permissionResponses.clear()
    }
}
