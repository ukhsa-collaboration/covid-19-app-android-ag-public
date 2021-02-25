package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils.MigrationException

object StrongBoxEncryptedSharedPreferencesMigrationManager {

    fun migrateToNewSharedPreferences(
        oldSharedPreferences: SharedPreferences,
        newSharedPreferences: SharedPreferences
    ) {
        val allPrefs = oldSharedPreferences.all

        newSharedPreferences.edit {
            allPrefs.forEach { entry ->
                val key = entry.key
                when (val value = entry.value) {
                    is String -> putString(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Set<*> -> putStringSet(key, value as Set<String>?)
                    null -> remove(key)
                    else -> throw MigrationException("Unsupported type")
                }
            }
        }

        oldSharedPreferences.edit().clear().apply()
        Timber.d("Migration finished")
    }
}
