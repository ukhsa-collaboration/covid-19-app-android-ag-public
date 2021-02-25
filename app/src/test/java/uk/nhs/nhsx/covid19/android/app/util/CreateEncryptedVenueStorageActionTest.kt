package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.io.File

class CreateEncryptedVenueStorageActionTest {

    private val context = mockk<Context>()
    private val encryptedFileUtils = mockk<EncryptedFileUtils>(relaxed = true)
    private val strongBoxMigrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>()
    private val fileFactory = mockk<FileFactory>(relaxed = true)
    private val strongBoxEncryptedFileMigrationManager = mockk<StrongBoxEncryptedFileMigrationManager>(relaxed = true)

    private val testSubject = CreateEncryptedVenueStorageAction(
        context,
        encryptedFileUtils,
        strongBoxMigrationRetryChecker,
        fileFactory
    )

    @Test
    fun `create encrypted venue storage`() {
        val parent = File("testfile")
        every { context.filesDir } returns parent

        testSubject()

        verify {
            encryptedFileUtils.createEncryptedFile(
                context,
                fileFactory.createFile(parent, "venues"),
                strongBoxMigrationRetryChecker
            )
        }
    }
}
