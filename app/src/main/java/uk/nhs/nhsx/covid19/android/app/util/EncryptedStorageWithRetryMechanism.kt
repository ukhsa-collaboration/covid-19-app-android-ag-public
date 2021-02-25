package uk.nhs.nhsx.covid19.android.app.util

object EncryptedStorageWithRetryMechanism {

    fun createEncryptedStorage(
        strongBoxMigrationRetryChecker: StrongBoxMigrationRetryChecker,
        createEncryptedVenueStorageAction: CreateEncryptedVenueStorageAction,
        createEncryptedSharedPreferencesAction: CreateEncryptedSharedPreferencesAction,
        retryMechanism: RetryMechanism = RetryMechanism
    ): EncryptedStorage {
        try {
            val encryptedFile = retryMechanism.retryWithBackOff(
                action = createEncryptedVenueStorageAction
            )
            val sharedPreferences = retryMechanism.retryWithBackOff(
                action = createEncryptedSharedPreferencesAction
            )
            return EncryptedStorage(encryptedFile, sharedPreferences)
        } catch (exception: Exception) {
            strongBoxMigrationRetryChecker.increment()
            throw exception
        }
    }
}
