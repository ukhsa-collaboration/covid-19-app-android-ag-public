package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.workarounds.ConcurrentModificationExceptionWorkaround
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPrefsDelegate<T>(
    private val sharedPref: SharedPreferences,
    private val valueKey: String,
    private val commit: Boolean
) : ReadWriteProperty<Any?, T?> {

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T? =
        sharedPref.all[valueKey] as? T

    override operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?
    ) {
        if (ConcurrentModificationExceptionWorkaround.shouldApplyWorkaround) {
            try {
                setValue(value)
            } catch (e: ConcurrentModificationException) {
                Timber.e(e, "ConcurrentModificationException swallowed. Please check if fix for https://issuetracker.google.com/issues/169862952 is released")
            }
        } else {
            setValue(value)
        }
    }

    private fun setValue(value: T?) {
        sharedPref.edit(commit) {
            when (value) {
                is String -> putString(valueKey, value)
                is Float -> putFloat(valueKey, value)
                is Long -> putLong(valueKey, value)
                is Boolean -> putBoolean(valueKey, value)
                is Int -> putInt(valueKey, value)
                null -> remove(valueKey)
            }
        }
    }

    companion object {
        const val fileName = "encryptedSharedPreferences"
        const val migrationSharedPreferencesFileName = "migrationEncryptedSharedPreferences"

        fun <T> SharedPreferences.with(
            valueKey: String,
            commit: Boolean = false
        ): SharedPrefsDelegate<T> =
            SharedPrefsDelegate(this, valueKey, commit)
    }
}
