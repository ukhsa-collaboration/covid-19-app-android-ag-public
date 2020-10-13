package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.FAILURE
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.NO_PREVIOUS_FILE
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.SUCCESS
import java.io.File
import kotlin.test.assertEquals

class StrongBoxMigrationManagerTest {

    private val testSubject = StrongBoxMigrationManager
    private val context = mockk<Context>()
    private val legacyFile = mockk<File>()
    private val encryptedFile = mockk<EncryptedFile>()
    private val legacyEncryptedFile = mockk<EncryptedFile>()
    private val encryptionUtils = mockk<EncryptionUtils>()

    @Before
    fun setUp() {
        every { legacyFile.exists() } returns true
    }

    @Test
    fun `returns NO_PREVIOUS_FILE if legacy file does not exist`() {
        every { legacyFile.exists() } returns false

        val result =
            testSubject.migrateToNewMasterKey(context, legacyFile, encryptedFile, encryptionUtils)

        assertEquals(NO_PREVIOUS_FILE, result)
    }

    @Test
    fun `returns FAILURE if can't migrate file`() {
        every { encryptionUtils.getDefaultMasterKey() } throws IllegalArgumentException()

        val result =
            testSubject.migrateToNewMasterKey(context, legacyFile, encryptedFile, encryptionUtils)

        assertEquals(FAILURE, result)
    }

    @Test
    fun `migrates file and returns SUCCESS`() {
        every { encryptionUtils.getDefaultMasterKey() } returns "masterKey"
        every {
            encryptionUtils.getEncryptedFile(
                context, legacyFile, "masterKey",
                EncryptionUtils.KEYSET_PREF_NAME
            )
        } returns legacyEncryptedFile

        mockkStatic("uk.nhs.nhsx.covid19.android.app.util.EncryptionUtilsKt", "uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManagerKt")
        every { legacyEncryptedFile.readText() } returns "contents"
        every { encryptedFile.writeText(any()) } returns Unit

        every { legacyFile.tryDelete() } returns Unit

        val result =
            testSubject.migrateToNewMasterKey(context, legacyFile, encryptedFile, encryptionUtils)

        assertEquals(SUCCESS, result)

        verify { encryptedFile.writeText("contents") }
        verify { legacyFile.tryDelete() }
    }
}
