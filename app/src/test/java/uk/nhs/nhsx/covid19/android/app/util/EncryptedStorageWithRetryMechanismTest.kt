package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test

class EncryptedStorageWithRetryMechanismTest {

    private val strongBoxMigrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>(relaxed = true)
    private val retryMechanism = mockk<RetryMechanism>()
    private val createEncryptedVenueStorageAction = mockk<CreateEncryptedVenueStorageAction>()
    private val createEncryptedSharedPreferencesAction = mockk<CreateEncryptedSharedPreferencesAction>()

    private val encryptedFileInfo = mockk<EncryptedFileInfo>()
    private val sharedPreferences = mockk<SharedPreferences>()

    @Before
    fun setUp() {
        mockkStatic(EncryptedStorageWithRetryMechanism::class)
    }

    @Test
    fun `successfully initialize storage`() {
        every { retryMechanism.retryWithBackOff(any(), action = ofType<CreateEncryptedVenueStorageAction>()) } returns encryptedFileInfo
        every { retryMechanism.retryWithBackOff(any(), action = ofType<CreateEncryptedSharedPreferencesAction>()) } returns sharedPreferences

        val actual = EncryptedStorageWithRetryMechanism.createEncryptedStorage(
            strongBoxMigrationRetryChecker,
            createEncryptedVenueStorageAction,
            createEncryptedSharedPreferencesAction,
            retryMechanism
        )

        assertEquals(encryptedFileInfo, actual.encryptedFile)
        assertEquals(sharedPreferences, actual.sharedPreferences)

        verify(exactly = 0) { strongBoxMigrationRetryChecker.increment() }
    }

    @Test
    fun `increment retry and propagate exception on failed storage initialization in retry mechanism`() {
        val expectedException = Exception()
        every { retryMechanism.retryWithBackOff(any(), any<() -> Any>()) } throws expectedException

        val actualException = assertFailsWith<Exception> {
            EncryptedStorageWithRetryMechanism.createEncryptedStorage(
                strongBoxMigrationRetryChecker,
                createEncryptedVenueStorageAction,
                createEncryptedSharedPreferencesAction,
                retryMechanism
            )
        }

        verify(exactly = 1) { strongBoxMigrationRetryChecker.increment() }
        assertEquals(expectedException, actualException)
    }
}
