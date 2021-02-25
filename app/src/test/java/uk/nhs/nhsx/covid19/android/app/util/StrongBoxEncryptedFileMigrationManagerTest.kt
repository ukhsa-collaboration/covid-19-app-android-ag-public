package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.io.File
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Failure
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.NoPreviousFile
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxEncryptedFileMigrationManager.MigrationResult.Success

class StrongBoxEncryptedFileMigrationManagerTest {

    private val context = mockk<Context>()
    private val oldStrongBoxFile = mockk<File>()
    private val newEncryptedFile = mockk<EncryptedFile>()
    private val oldStrongBoxEncryptedFile = mockk<EncryptedFile>()
    private val encryptionUtils = mockk<EncryptionUtils>(relaxed = true)
    private val encryptedFileFactory = mockk<EncryptedFileFactory>(relaxed = true)

    private val testSubject = StrongBoxEncryptedFileMigrationManager(encryptionUtils, encryptedFileFactory)

    @Before
    fun setUp() {
        every { oldStrongBoxFile.exists() } returns true
    }

    @Test
    fun `returns NoPreviousFile if legacy file does not exist`() {
        every { oldStrongBoxFile.exists() } returns false

        val result = testSubject.migrateToNewEncryptedFile(
            context,
            oldStrongBoxFile,
            newEncryptedFile
        )

        assertEquals(NoPreviousFile, result)
    }

    @Test
    fun `returns Failure if can't migrate file`() {
        val testException = Exception()
        every { oldStrongBoxFile.exists() } returns true
        every {
            encryptedFileFactory.getStrongBoxEncryptedFile(
                context,
                oldStrongBoxFile,
                encryptionUtils.getStrongBoxBackedMasterKey()
            )
        } throws testException

        val result = testSubject.migrateToNewEncryptedFile(
            context,
            oldStrongBoxFile,
            newEncryptedFile
        )

        assertEquals(Failure(testException), result)
    }

    @Test
    fun `migrates file and returns Success`() {
        every { oldStrongBoxFile.exists() } returns true
        every {
            encryptedFileFactory.getStrongBoxEncryptedFile(
                context,
                oldStrongBoxFile,
                encryptionUtils.getStrongBoxBackedMasterKey()
            )
        } returns oldStrongBoxEncryptedFile

        mockkStatic(
            "uk.nhs.nhsx.covid19.android.app.util.EncryptedFileUtilsKt",
        )
        every { oldStrongBoxEncryptedFile.readText() } returns "contents"
        every { newEncryptedFile.writeText(any()) } returns Unit

        every { oldStrongBoxFile.tryDelete() } returns Unit

        val result = testSubject.migrateToNewEncryptedFile(
            context,
            oldStrongBoxFile,
            newEncryptedFile
        )

        assertEquals(Success, result)

        verify { newEncryptedFile.writeText("contents") }
        verify { oldStrongBoxFile.tryDelete() }
    }
}
