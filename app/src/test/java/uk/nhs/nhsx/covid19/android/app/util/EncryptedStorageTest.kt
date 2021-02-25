package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class EncryptedStorageTest {

    private val context = mockk<Context>()
    private val strongBoxMigrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>(relaxed = true)
    private val encryptedStorageWithRetryMechanism = mockk<EncryptedStorageWithRetryMechanism>(relaxed = true)
    private val encryptionUtils = mockk<EncryptionUtils>()

    private val encryptedFileInfo = mockk<EncryptedFileInfo>()
    private val sharedPreferences = mockk<SharedPreferences>()

    @Before
    fun setUp() {
        mockkStatic(EncryptedStorageWithRetryMechanism::class)
    }

    @Test
    fun `initialize storage`() {
        val createEncryptedVenueStorageAction = mockk<CreateEncryptedVenueStorageAction>()
        val createEncryptedSharedPreferencesAction = mockk<CreateEncryptedSharedPreferencesAction>()
        val encryptedStorage = EncryptedStorage(
            encryptedFileInfo,
            sharedPreferences
        )
        every {
            encryptedStorageWithRetryMechanism.createEncryptedStorage(
                strongBoxMigrationRetryChecker,
                createEncryptedVenueStorageAction = createEncryptedVenueStorageAction,
                createEncryptedSharedPreferencesAction = createEncryptedSharedPreferencesAction
            )
        } returns encryptedStorage

        val actual = EncryptedStorage.from(
            context,
            strongBoxMigrationRetryChecker,
            encryptionUtils,
            encryptedStorageWithRetryMechanism,
            createEncryptedVenueStorageAction,
            createEncryptedSharedPreferencesAction
        )

        verify {
            encryptedStorageWithRetryMechanism.createEncryptedStorage(
                strongBoxMigrationRetryChecker,
                createEncryptedVenueStorageAction = createEncryptedVenueStorageAction,
                createEncryptedSharedPreferencesAction = createEncryptedSharedPreferencesAction
            )
        }

        assertEquals(encryptedStorage, actual)
    }
}
