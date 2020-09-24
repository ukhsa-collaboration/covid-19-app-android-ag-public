package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptionUtilsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val filename = "testfile"

    @Before
    fun setup() {
        File(context.filesDir, filename).delete()
    }

    @Test
    fun testEncryptedFileUtils() {
        val testSubject = EncryptionUtils

        val encryptedFileInfo = testSubject.createEncryptedFile(context, filename)
        assertFalse(encryptedFileInfo.file.exists())

        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
            assertEquals(filename + "_strongbox", encryptedFileInfo.file.name)
        } else {
            assertEquals(filename, encryptedFileInfo.file.name)
        }

        encryptedFileInfo.encryptedFile.writeText("abcde")
        assertTrue(encryptedFileInfo.file.exists())
        assertEquals("abcde", encryptedFileInfo.encryptedFile.readText())

        encryptedFileInfo.file.delete()
        assertFalse(encryptedFileInfo.file.exists())

        encryptedFileInfo.encryptedFile.writeText("")
        assertEquals("", encryptedFileInfo.encryptedFile.readText())
    }
}
