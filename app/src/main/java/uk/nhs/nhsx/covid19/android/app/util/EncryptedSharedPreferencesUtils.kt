package uk.nhs.nhsx.covid19.android.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils.MigrationException
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED

class EncryptedSharedPreferencesUtils(
    val encryptionUtils: EncryptionUtils,
    private val sharedPreferencesMigrationManager: StrongBoxEncryptedSharedPreferencesMigrationManager = StrongBoxEncryptedSharedPreferencesMigrationManager
) {

    @SuppressLint("NewApi")
    fun createEncryptedSharedPreferences(
        context: Context,
        migrationRetryChecker: StrongBoxMigrationRetryChecker,
    ): SharedPreferences =
        when (encryptionUtils.getStrongBoxStatus(context)) {
            PRESENT_DISALLOWED -> {
                Timber.d("StrongBox present and disallowed")
                createEncryptedSharedPreferencesAfterStrongBoxMigration(
                    context,
                    migrationRetryChecker
                )
            }
            PRESENT_ALLOWED -> {
                Timber.d("StrongBox present and allowed")
                createStrongBoxBackedEncryptedSharedPreferences(
                    context
                )
            }
            NOT_PRESENT -> {
                Timber.d("StrongBox not present")
                createGenericEncryptedSharedPreferences(
                    context,
                    encryptionUtils.getDefaultMasterKey(),
                    SharedPrefsDelegate.fileName
                )
            }
        }

    internal fun createGenericEncryptedSharedPreferences(
        context: Context,
        masterKeyAlias: String,
        fileName: String
    ) =
        EncryptedSharedPreferences.create(
            fileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    @RequiresApi(VERSION_CODES.P)
    private fun createEncryptedSharedPreferencesAfterStrongBoxMigration(
        context: Context,
        migrationRetryChecker: StrongBoxMigrationRetryChecker
    ): SharedPreferences {
        val sharedPrefs = createGenericEncryptedSharedPreferences(
            context,
            encryptionUtils.getDefaultMasterKey(),
            SharedPrefsDelegate.fileName
        )

        if (sharedPrefs.all.isEmpty() && migrationRetryChecker.canMigrate()) {
            try {
                val oldStrongBoxBackedPrefs = createStrongBoxBackedEncryptedSharedPreferences(
                    context
                )
                sharedPreferencesMigrationManager.migrateToNewSharedPreferences(
                    oldSharedPreferences = oldStrongBoxBackedPrefs,
                    newSharedPreferences = sharedPrefs
                )
            } catch (exception: Exception) {
                throw MigrationException(exception)
            }
        }

        return sharedPrefs
    }

    @RequiresApi(VERSION_CODES.P)
    private fun createStrongBoxBackedEncryptedSharedPreferences(
        context: Context,
    ): SharedPreferences {
        val strongBoxMasterKeyAlias = encryptionUtils.getStrongBoxBackedMasterKey()
        return createGenericEncryptedSharedPreferences(
            context,
            strongBoxMasterKeyAlias,
            "strongBoxBackedPrefs"
        )
    }
}
