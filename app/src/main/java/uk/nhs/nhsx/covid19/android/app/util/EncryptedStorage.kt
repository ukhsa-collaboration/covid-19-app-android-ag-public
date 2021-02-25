package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.SharedPreferences

class EncryptedStorage(
    val encryptedFile: EncryptedFileInfo,
    val sharedPreferences: SharedPreferences,
) {

    companion object {
        fun from(
            context: Context,
            strongBoxMigrationRetryChecker: StrongBoxMigrationRetryChecker,
            encryptionUtils: EncryptionUtils,
            encryptedStorageWithRetryMechanism: EncryptedStorageWithRetryMechanism = EncryptedStorageWithRetryMechanism,
            createEncryptedVenueStorageAction: CreateEncryptedVenueStorageAction = CreateEncryptedVenueStorageAction(
                context,
                EncryptedFileUtils(encryptionUtils),
                strongBoxMigrationRetryChecker,
                FileFactory
            ),
            createEncryptedSharedPreferencesAction: CreateEncryptedSharedPreferencesAction = CreateEncryptedSharedPreferencesAction(
                context,
                EncryptedSharedPreferencesUtils(encryptionUtils),
                strongBoxMigrationRetryChecker
            )
        ): EncryptedStorage {
            return encryptedStorageWithRetryMechanism.createEncryptedStorage(
                strongBoxMigrationRetryChecker,
                createEncryptedVenueStorageAction = createEncryptedVenueStorageAction,
                createEncryptedSharedPreferencesAction = createEncryptedSharedPreferencesAction
            )
        }
    }
}

class CreateEncryptedVenueStorageAction(
    private val context: Context,
    private val encryptedFileUtils: EncryptedFileUtils,
    private val strongBoxMigrationRetryChecker: StrongBoxMigrationRetryChecker,
    private val fileFactory: FileFactory
) : () -> EncryptedFileInfo {

    override operator fun invoke(): EncryptedFileInfo =
        encryptedFileUtils.createEncryptedFile(
            context,
            fileFactory.createFile(context.filesDir, "venues"),
            strongBoxMigrationRetryChecker
        )
}

class CreateEncryptedSharedPreferencesAction(
    private val context: Context,
    private val encryptedSharedPreferencesUtils: EncryptedSharedPreferencesUtils,
    private val strongBoxMigrationRetryChecker: StrongBoxMigrationRetryChecker
) : () -> SharedPreferences {

    override operator fun invoke(): SharedPreferences =
        encryptedSharedPreferencesUtils.createEncryptedSharedPreferences(
            context,
            strongBoxMigrationRetryChecker
        )
}
