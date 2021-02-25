package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Success
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED
import java.io.File
import kotlin.test.assertEquals

class EncryptedFileUtilsTest {

    private val context = mockk<Context>()
    private val filesDir = File("test")
    private val file = mockk<File>()
    private val encryptedFile = mockk<EncryptedFile>()
    private val migrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>()
    private val migrationManager = mockk<StrongBoxEncryptedFileMigrationManager>()
    private val encryptionUtils = mockk<EncryptionUtils>()
    private val encryptedFileFactory = mockk<EncryptedFileFactory>(relaxed = true)

    private val testSubject = EncryptedFileUtils(encryptionUtils, encryptedFileFactory, migrationManager)

    @Before
    fun setUp() {
        every { encryptionUtils.getDefaultMasterKey() } returns "masterKey"
        every { encryptedFileFactory.getStrongBoxEncryptedFile(any(), any(), any()) } returns encryptedFile
        every { encryptedFileFactory.getEncryptedFile(any(), any(), any(), any()) } returns encryptedFile
        every { encryptionUtils.getStrongBoxBackedMasterKey() } returns "strongBoxMasterKey"
        every { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) } returns Success
    }

    @Test
    fun `creates encrypted file and migrates if no encrypted file, strong box is present, disallowed and can migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { file.exists() } returns false
        every { migrationRetryChecker.canMigrate() } returns true
        every { context.filesDir } returns filesDir
        every { file.name } returns "testFileName"

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        val oldStrongBoxFile = File(filesDir, file.name + "_strongbox")
        verify(exactly = 1) {
            migrationManager.migrateToNewEncryptedFile(
                context,
                oldStrongBoxFile = oldStrongBoxFile,
                newEncryptedFile = encryptedFile
            )
        }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(file, encryptedFileInfo.file)
    }

    @Test
    fun `creates encrypted file and doesn't migrate if encrypted file exists, strong box is present, disallowed and can migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { file.exists() } returns true
        every { migrationRetryChecker.canMigrate() } returns true

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(file, encryptedFileInfo.file)
    }

    @Test
    fun `creates encrypted file and doesn't migrate if no encrypted file, strong box is present, disallowed and cannot migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { file.exists() } returns false
        every { migrationRetryChecker.canMigrate() } returns false

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(file, encryptedFileInfo.file)
    }

    @Test
    fun `creates encrypted file and doesn't migrate if encrypted file exists, strong box is present, disallowed and cannot migrate`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_DISALLOWED
        every { file.exists() } returns true
        every { migrationRetryChecker.canMigrate() } returns false

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(file, encryptedFileInfo.file)
    }

    @Test
    fun `creates strong box file and doesn't migrate if strong box is present and allowed`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns PRESENT_ALLOWED
        every { context.filesDir } returns filesDir
        every { file.name } returns "testFileName"

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(File(filesDir, file.name + "_strongbox"), encryptedFileInfo.file)
    }

    @Test
    fun `creates encrypted file and does't migrate if strong box is not present`() {
        every { encryptionUtils.getStrongBoxStatus(context) } returns NOT_PRESENT

        val encryptedFileInfo = testSubject.createEncryptedFile(
            context,
            file,
            migrationRetryChecker
        )

        verify(exactly = 0) { migrationManager.migrateToNewEncryptedFile(any(), any(), any()) }
        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(file, encryptedFileInfo.file)
    }
}
