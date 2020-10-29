package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxMigrationManager.MigrationResult.SUCCESS
import java.io.File
import kotlin.test.assertEquals

class EncryptionUtilsTest {

    val testSubject = spyk(EncryptionUtils)
    private val context = mockk<Context>()
    private val fileName = "filename"
    private val filesDir = File("test")
    private val encryptedFile = mockk<EncryptedFile>()
    private val migrationManager = mockk<StrongBoxMigrationManager>()

    @Before
    fun setUp() {
        every { testSubject.getDefaultMasterKey() } returns "masterKey"
        every { context.filesDir } returns filesDir
        every { testSubject.getEncryptedFile(any(), any(), any(), any()) } returns encryptedFile
        every { testSubject.getStrongBoxBackedMasterKey() } returns "strongBoxMasterKey"
        every { migrationManager.migrateToNewMasterKey(any(), any(), any()) } returns SUCCESS
    }

    @Test
    fun `creates encrypted file if strong box is not supported`() {
        every { testSubject.hasStrongBox(context) } returns false

        val encryptedFileInfo = testSubject.createEncryptedFile(context, fileName, migrationManager)

        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(File(filesDir, fileName), encryptedFileInfo.file)
    }

    @Test
    fun `do not migrate if strong box is not supported`() {
        every { testSubject.hasStrongBox(context) } returns false

        testSubject.createEncryptedFile(context, fileName, migrationManager)

        verify(exactly = 0) { migrationManager.migrateToNewMasterKey(any(), any(), any()) }
    }

    @Test
    fun `creates encrypted file if strong box is supported`() {
        every { testSubject.hasStrongBox(context) } returns true

        val encryptedFileInfo = testSubject.createEncryptedFile(context, fileName, migrationManager)

        assertEquals(encryptedFile, encryptedFileInfo.encryptedFile)
        assertEquals(File(filesDir, fileName + "_strongbox"), encryptedFileInfo.file)
    }

    @Test
    fun `migrate if strong box is supported`() {
        every { testSubject.hasStrongBox(context) } returns true

        testSubject.createEncryptedFile(context, fileName, migrationManager)

        val legacyFile = File(filesDir, fileName)
        verify(exactly = 1) {
            migrationManager.migrateToNewMasterKey(
                context,
                legacyFile,
                encryptedFile
            )
        }
    }

    @Test
    fun `don't retry on success`() {
        val function: () -> Unit = {}
        val spyFunction = spyk(function)

        testSubject.retryOnException(
            times = 5,
            factor = 1.0,
            maxDelay = 1,
            function = spyFunction
        )

        verify(exactly = 1) { spyFunction.invoke() }
    }

    @Test
    fun `retry on exception`() {
        val exception = Exception()
        val function: () -> Unit = {
            throw exception
        }
        val spyFunction = spyk(function)

        val result = runCatching {
            testSubject.retryOnException(
                times = 5,
                factor = 1.0,
                maxDelay = 1,
                function = spyFunction
            )
        }

        verify(exactly = 5) { spyFunction.invoke() }
        assertEquals(Result.failure(exception), result)
    }
}
