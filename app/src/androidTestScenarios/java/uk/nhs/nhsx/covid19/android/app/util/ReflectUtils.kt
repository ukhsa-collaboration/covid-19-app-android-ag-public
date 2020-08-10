package uk.nhs.nhsx.covid19.android.app.util

import android.util.Log
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R? {
    val r = T::class
        .memberProperties
        .firstOrNull { it.name == name }
        ?.apply { isAccessible = true }
        ?.get(this) as? R
    Log.d("Reflection", "getPrivateProperty: ${T::class.memberProperties}")
    return r
}
