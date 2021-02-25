package uk.nhs.nhsx.covid19.android.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedFile.Builder
import androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils.MigrationException
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Failure
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED
import java.io.File
import java.io.OutputStreamWriter

class EncryptedFileUtils(
    val encryptionUtils: EncryptionUtils,
    private val encryptedFileFactory: EncryptedFileFactory = EncryptedFileFactory,
    private val encryptedFileMigrationManager: StrongBoxEncryptedFileMigrationManager = StrongBoxEncryptedFileMigrationManager(encryptionUtils)
) {

    private val KEYSET_PREF_NAME = "__androidx_security_crypto_encrypted_file_pref__"
    private val STRONGBOX_FILE_NAME_SUFFIX = "_strongbox"

    @SuppressLint("NewApi")
    fun createEncryptedFile(
        context: Context,
        file: File,
        migrationRetryChecker: StrongBoxMigrationRetryChecker
    ): EncryptedFileInfo =
        when (encryptionUtils.getStrongBoxStatus(context)) {
            PRESENT_DISALLOWED -> {
                Timber.d("StrongBox present and disallowed")
                createEncryptedFileAfterStrongBoxMigration(
                    context,
                    file,
                    migrationRetryChecker
                )
            }
            PRESENT_ALLOWED -> {
                Timber.d("StrongBox present and allowed")
                createStrongBoxBackedEncryptedFile(
                    context,
                    file
                )
            }
            NOT_PRESENT -> {
                Timber.d("StrongBox not present")
                createGenericEncryptedFile(
                    context,
                    file,
                    encryptionUtils
                )
            }
        }

    @RequiresApi(VERSION_CODES.P)
    private fun createEncryptedFileAfterStrongBoxMigration(
        context: Context,
        file: File,
        migrationRetryChecker: StrongBoxMigrationRetryChecker
    ): EncryptedFileInfo {
        val migrationNeeded = !file.exists() && migrationRetryChecker.canMigrate()

        val masterKeyAlias = encryptionUtils.getDefaultMasterKey()
        val encryptedFile = encryptedFileFactory.getEncryptedFile(context, file, masterKeyAlias, KEYSET_PREF_NAME)

        if (migrationNeeded) {
            val strongBoxFile = File(context.filesDir, file.name + STRONGBOX_FILE_NAME_SUFFIX)
            val migrationResult = encryptedFileMigrationManager.migrateToNewEncryptedFile(
                context,
                oldStrongBoxFile = strongBoxFile,
                newEncryptedFile = encryptedFile
            )
            if (migrationResult is Failure) {
                throw MigrationException(migrationResult.exception)
            }
        }

        return EncryptedFileInfo(
            file,
            encryptedFile
        )
    }

    @RequiresApi(VERSION_CODES.P)
    private fun createStrongBoxBackedEncryptedFile(
        context: Context,
        file: File,
    ): EncryptedFileInfo {
        val strongBoxFile = File(context.filesDir, file.name + STRONGBOX_FILE_NAME_SUFFIX)
        val strongBoxBackedEncryptedFile = encryptedFileFactory.getStrongBoxEncryptedFile(
            context,
            strongBoxFile,
            encryptionUtils.getStrongBoxBackedMasterKey()
        )

        return EncryptedFileInfo(
            strongBoxFile,
            strongBoxBackedEncryptedFile
        )
    }

    private fun createGenericEncryptedFile(
        context: Context,
        file: File,
        encryptionUtils: EncryptionUtils
    ) =
        EncryptedFileInfo(
            file,
            encryptedFileFactory.getEncryptedFile(context, file, encryptionUtils.getDefaultMasterKey(), KEYSET_PREF_NAME)
        )
}

data class EncryptedFileInfo(
    val file: File,
    val encryptedFile: EncryptedFile
)

fun EncryptedFile.readText(): String {
    val inputStream = openFileInput()
    return inputStream.bufferedReader().use { it.readText() }
}

fun EncryptedFile.writeText(content: String) {
    val writer = OutputStreamWriter(openFileOutput(), Charsets.UTF_8)
    writer.use { it.write(content) }
}

object EncryptedFileFactory {
    private val STRONGBOX_KEYSET_PREF_NAME =
        "__androidx_security_crypto_encrypted_file_strongbox_pref__"

    @RequiresApi(VERSION_CODES.P)
    fun getStrongBoxEncryptedFile(
        context: Context,
        file: File,
        strongBoxMasterKeyAlias: String,
    ): EncryptedFile {
        return getEncryptedFile(
            context,
            file,
            strongBoxMasterKeyAlias,
            STRONGBOX_KEYSET_PREF_NAME
        )
    }

    fun getEncryptedFile(
        context: Context,
        file: File,
        masterKeyAlias: String,
        keySetPrefName: String
    ): EncryptedFile {
        return Builder(file, context, masterKeyAlias, AES256_GCM_HKDF_4KB)
            .setKeysetPrefName(keySetPrefName)
            .build()
    }
}
