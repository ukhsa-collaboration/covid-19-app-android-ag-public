package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED
import kotlin.test.assertEquals

class EncryptedSharedPreferencesUtilsTest {

    private val context = mockk<Context>()
    private val sharedPreferences = mockk<SharedPreferences>()
    private val strongBoxSharedPreferences = mockk<SharedPreferences>()
    private val migrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>()
    private val migrationManager = mockk<StrongBoxEncryptedSharedPreferencesMigrationManager>()
    private val encryptionUtils = mockk<EncryptionUtils>()

    private val testSubject = spyk(EncryptedSharedPreferencesUtils(encryptionUtils, migrationManager))

    @Before
    fun setUp() {
        every { encryptionUtils.getDefaultMasterKey() } returns "masterKey"
        every { encryptionUtils.getStrongBoxBackedMasterKey() } returns "strongBoxMasterKey"
        every {
            testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences")
        } returns sharedPreferences
        every {
            testSubject.createGenericEncryptedSharedPreferences(any(), "strongBoxMasterKey", "strongBoxBackedPrefs")
        } returns strongBoxSharedPreferences
        every { migrationManager.migrateToNewSharedPreferences(any(), any()) } returns Unit
    }

    @Test
    fun `creates encrypted prefs and migrates if no encrypted prefs, strong box is present, disallowed and can migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { sharedPreferences.all.isEmpty() } returns true
        every { migrationRetryChecker.canMigrate() } returns true

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 1) {
            migrationManager.migrateToNewSharedPreferences(
                oldSharedPreferences = strongBoxSharedPreferences,
                newSharedPreferences = sharedPreferences
            )
        }
        verifyOrder {
            testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences")
            testSubject.createGenericEncryptedSharedPreferences(any(), "strongBoxMasterKey", "strongBoxBackedPrefs")
        }
        assertEquals(sharedPreferences, actual)
    }

    @Test
    fun `creates encrypted prefs and doesn't migrate if encrypted prefs exists, strong box is present, disallowed and can migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { sharedPreferences.all.isEmpty() } returns false
        every { migrationRetryChecker.canMigrate() } returns true

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewSharedPreferences(any(), any()) }
        verify { testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences") }
        assertEquals(sharedPreferences, actual)
    }

    @Test
    fun `creates encrypted file and doesn't migrate if no encrypted file, strong box is present, disallowed and cannot migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { sharedPreferences.all.isEmpty() } returns true
        every { migrationRetryChecker.canMigrate() } returns false

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewSharedPreferences(any(), any()) }
        verify { testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences") }
        assertEquals(sharedPreferences, actual)
    }

    @Test
    fun `creates encrypted file and doesn't migrate if encrypted file exists, strong box is present, disallowed and cannot migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { sharedPreferences.all.isEmpty() } returns false
        every { migrationRetryChecker.canMigrate() } returns false

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewSharedPreferences(any(), any()) }
        verify { testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences") }
        assertEquals(sharedPreferences, actual)
    }

    @Test
    fun `creates strong box file and doesn't migrate if strong box is present and allowed`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_ALLOWED

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewSharedPreferences(any(), any()) }
        verify {
            testSubject.createGenericEncryptedSharedPreferences(
                any(),
                "strongBoxMasterKey",
                "strongBoxBackedPrefs"
            )
        }
        assertEquals(strongBoxSharedPreferences, actual)
    }

    @Test
    fun `creates encrypted file and does't migrate if strong box is not present`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns NOT_PRESENT

        val actual = testSubject.createEncryptedSharedPreferences(
            context,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewSharedPreferences(any(), any()) }
        verify { testSubject.createGenericEncryptedSharedPreferences(any(), "masterKey", "encryptedSharedPreferences") }
        assertEquals(sharedPreferences, actual)
    }
}
