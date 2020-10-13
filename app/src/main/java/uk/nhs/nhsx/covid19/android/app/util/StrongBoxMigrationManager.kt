package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.FAILURE
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.NO_PREVIOUS_FILE
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.SUCCESS
import java.io.File

object StrongBoxMigrationManager {

    fun migrateToNewMasterKey(
        context: Context,
        legacyFile: File,
        strongBoxBackedFile: EncryptedFile,
        encryptionUtils: EncryptionUtils = EncryptionUtils
    ): MigrationResult {
        if (!legacyFile.exists()) {
            return NO_PREVIOUS_FILE
        }
        Timber.d("Needs file migration")

        try {
            val masterKeyAlias = encryptionUtils.getDefaultMasterKey()
            val fileEncryptedWithMasterKeyNotBackedByStrongBox =
                encryptionUtils.getEncryptedFile(
                    context,
                    legacyFile,
                    masterKeyAlias,
                    EncryptionUtils.KEYSET_PREF_NAME
                )
            val contents = fileEncryptedWithMasterKeyNotBackedByStrongBox.readText()
            strongBoxBackedFile.writeText(contents)
            legacyFile.tryDelete()
            Timber.d("Migrated")
            return SUCCESS
        } catch (exception: Exception) {
            Timber.d(exception, "Could not migrate")
            return FAILURE
        }
    }

    enum class MigrationResult {
        NO_PREVIOUS_FILE,
        SUCCESS,
        FAILURE
    }
}

fun File.tryDelete() {
    try {
        delete()
    } catch (exception: Exception) {
        Timber.d(exception, "Can't delete file")
    }
}
