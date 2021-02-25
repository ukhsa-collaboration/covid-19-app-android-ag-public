package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with

class StrongBoxMigrationRetryChecker(
    private val strongBoxMigrationRetryStorage: StrongBoxMigrationRetryStorage
) {

    fun increment() {
        strongBoxMigrationRetryStorage.value = strongBoxMigrationRetryStorage.value?.let {
            it + 1
        } ?: 1
    }

    fun canMigrate(): Boolean =
        strongBoxMigrationRetryStorage.value?.let { it < MAX_MIGRATION_ATTEMPTS } ?: true

    companion object {
        private const val MAX_MIGRATION_ATTEMPTS = 3
    }
}

class StrongBoxMigrationRetryStorage(val sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Int>(
        STRONG_BOX_MIGRATION_RETRY_KEY,
        commit = true
    )

    var value: Int? by prefs

    companion object {
        private const val STRONG_BOX_MIGRATION_RETRY_KEY =
            "STRONG_BOX_MIGRATION_RETRY_KEY"
    }
}
