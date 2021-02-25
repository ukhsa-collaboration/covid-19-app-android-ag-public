package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedFile
import java.io.File
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Failure
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.NoPreviousFile
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Success

class StrongBoxEncryptedFileMigrationManager(
    private val encryptionUtils: EncryptionUtils,
    private val encryptedFileFactory: EncryptedFileFactory = EncryptedFileFactory
) {

    @RequiresApi(VERSION_CODES.P)
    fun migrateToNewEncryptedFile(
        context: Context,
        oldStrongBoxFile: File,
        newEncryptedFile: EncryptedFile,
    ): MigrationResult {
        if (!oldStrongBoxFile.exists()) {
            return NoPreviousFile
        }
        Timber.d("Needs file migration")

        return try {
            val strongBoxBackedEncryptedFile = encryptedFileFactory.getStrongBoxEncryptedFile(
                context,
                oldStrongBoxFile,
                encryptionUtils.getStrongBoxBackedMasterKey()
            )
            val contents = strongBoxBackedEncryptedFile.readText()
            newEncryptedFile.writeText(contents)
            oldStrongBoxFile.tryDelete()
            Timber.d("Migrated")
            Success
        } catch (exception: Exception) {
            Timber.d(exception, "Could not migrate from file: $oldStrongBoxFile")
            Failure(exception)
        }
    }

    sealed class MigrationResult {
        object NoPreviousFile : MigrationResult()
        object Success : MigrationResult()
        data class Failure(val exception: Exception) : MigrationResult()
    }
}
