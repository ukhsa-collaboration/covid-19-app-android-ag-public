package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.util.AndroidStrongBoxSupport.getStrongBoxStatus
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED

class EncryptionUtilsIntegrationTest : EspressoTest() {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.filesDir, "testfile")
    private val strongBoxFile = File(context.filesDir, "testfile_strongbox")

    @Before
    fun setUp() {
        file.delete()
        strongBoxFile.delete()

        val encryptionUtils = EncryptionUtils(AndroidStrongBoxSupport)
        val sharedPreferences = createSharedPreferences(encryptionUtils)
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun encryptedFileWhenStrongBoxPresentAndDisallowed() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )

        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_DISALLOWED
        })

        val encryptedFileInfo = createEncryptedFile(encryptionUtils)

        assertFalse(encryptedFileInfo.file.exists())
        assertEquals(file.name, encryptedFileInfo.file.name)

        encryptedFileInfo.encryptedFile.writeText("abcde")
        assertTrue(encryptedFileInfo.file.exists())
        assertEquals("abcde", encryptedFileInfo.encryptedFile.readText())

        encryptedFileInfo.file.delete()
        assertFalse(encryptedFileInfo.file.exists())

        encryptedFileInfo.encryptedFile.writeText("")
        assertEquals("", encryptedFileInfo.encryptedFile.readText())
    }

    @Test
    fun encryptedFileWhenStrongBoxPresentAndAllowed() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )
        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_ALLOWED
        })

        val encryptedFileInfo = createEncryptedFile(encryptionUtils)

        assertFalse(encryptedFileInfo.file.exists())
        assertEquals(file.name + "_strongbox", encryptedFileInfo.file.name)

        encryptedFileInfo.encryptedFile.writeText("abcde")
        assertTrue(encryptedFileInfo.file.exists())
        assertEquals("abcde", encryptedFileInfo.encryptedFile.readText())

        encryptedFileInfo.file.delete()
        assertFalse(encryptedFileInfo.file.exists())

        encryptedFileInfo.encryptedFile.writeText("")
        assertEquals("", encryptedFileInfo.encryptedFile.readText())
    }

    @Test
    fun encryptedFileWhenStrongBoxNotPresent() = notReported {

        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                NOT_PRESENT
        })

        val encryptedFileInfo = createEncryptedFile(encryptionUtils)

        assertFalse(encryptedFileInfo.file.exists())
        assertEquals(file.name, encryptedFileInfo.file.name)

        encryptedFileInfo.encryptedFile.writeText("abcde")
        assertTrue(encryptedFileInfo.file.exists())
        assertEquals("abcde", encryptedFileInfo.encryptedFile.readText())

        encryptedFileInfo.file.delete()
        assertFalse(encryptedFileInfo.file.exists())

        encryptedFileInfo.encryptedFile.writeText("")
        assertEquals("", encryptedFileInfo.encryptedFile.readText())
    }

    @Test
    fun migrateEncryptedFileFromStrongBox() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )
        val encryptionUtilsStrongBoxAllowed = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_ALLOWED
        })

        val encryptedFileInfoStrongBoxAllowed = createEncryptedFile(encryptionUtilsStrongBoxAllowed)

        assertFalse(encryptedFileInfoStrongBoxAllowed.file.exists())
        assertEquals(file.name + "_strongbox", encryptedFileInfoStrongBoxAllowed.file.name)

        encryptedFileInfoStrongBoxAllowed.encryptedFile.writeText("abcde")
        assertTrue(encryptedFileInfoStrongBoxAllowed.file.exists())

        val encryptionUtilsStrongBoxDisallowed = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_DISALLOWED
        })

        val encryptedFileInfoStrongBoxDisallowed = createEncryptedFile(encryptionUtilsStrongBoxDisallowed)

        assertFalse(encryptedFileInfoStrongBoxAllowed.file.exists())

        assertTrue(encryptedFileInfoStrongBoxDisallowed.file.exists())
        assertEquals(file.name, encryptedFileInfoStrongBoxDisallowed.file.name)

        assertEquals("abcde", encryptedFileInfoStrongBoxDisallowed.encryptedFile.readText())
    }

    @Test
    fun sharedPreferencesWhenStrongBoxPresentAndDisallowed() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )
        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_DISALLOWED
        })

        var sharedPreferences = createSharedPreferences(encryptionUtils)

        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key1", "abcde")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("abcde", sharedPreferences.getString("key1", null))

        sharedPreferences.edit().clear().commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key2", "")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("", sharedPreferences.getString("key2", null))
    }

    @Test
    fun sharedPreferencesWhenStrongBoxPresentAndAllowed() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )
        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_ALLOWED
        })

        var sharedPreferences = createSharedPreferences(encryptionUtils)

        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key1", "abcde")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("abcde", sharedPreferences.getString("key1", null))

        sharedPreferences.edit().clear().commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key2", "")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("", sharedPreferences.getString("key2", null))
    }

    @Test
    fun sharedPreferencesWhenStrongBoxNotPresent() = notReported {

        val encryptionUtils = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                NOT_PRESENT
        })

        var sharedPreferences = createSharedPreferences(encryptionUtils)

        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key1", "abcde")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("abcde", sharedPreferences.getString("key1", null))

        sharedPreferences.edit().clear().commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isEmpty())

        sharedPreferences.edit().apply {
            putString("key2", "")
        }.commit()

        sharedPreferences = createSharedPreferences(encryptionUtils)
        assertTrue(sharedPreferences.all.isNotEmpty())
        assertEquals("", sharedPreferences.getString("key2", null))
    }

    @Test
    fun migrateSharedPreferencesFromStrongBox() = notReported {
        assumeTrue(
            "Skip test on device without StrongBox",
            getStrongBoxStatus(context) == PRESENT_ALLOWED || getStrongBoxStatus(context) == PRESENT_DISALLOWED
        )
        val encryptionUtilsStrongBoxAllowed = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_ALLOWED
        })

        val sharedPreferencesStrongBoxAllowed = createSharedPreferences(encryptionUtilsStrongBoxAllowed)

        assertTrue(sharedPreferencesStrongBoxAllowed.all.isEmpty())

        sharedPreferencesStrongBoxAllowed.edit(commit = true) {
            for (record in 1..1000) {
                putString(UUID.randomUUID().toString(), UUID.randomUUID().toString())
            }
        }

        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putString("key-string", "abcde") }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putFloat("key-float", 43673F) }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putLong("key-long", 34334L) }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putBoolean("key-boolean", true) }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putInt("key-int", 643455) }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) {
            putStringSet("key-string-set", setOf("first", "second"))
        }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { putString("key-null", "for removal") }
        sharedPreferencesStrongBoxAllowed.edit(commit = true) { remove("key-null") }

        assertEquals(1006, sharedPreferencesStrongBoxAllowed.all.size)

        val encryptionUtilsStrongBoxDisallowed = EncryptionUtils(object : StrongBoxSupport {
            override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
                PRESENT_DISALLOWED
        })

        val sharedPreferencesStrongBoxDisallowed = createSharedPreferences(encryptionUtilsStrongBoxDisallowed)

        assertTrue(sharedPreferencesStrongBoxAllowed.all.isEmpty())

        assertEquals(1006, sharedPreferencesStrongBoxDisallowed.all.size)

        assertEquals("abcde", sharedPreferencesStrongBoxDisallowed.getString("key-string", null))
        assertEquals(43673F, sharedPreferencesStrongBoxDisallowed.getFloat("key-float", -1F))
        assertEquals(34334L, sharedPreferencesStrongBoxDisallowed.getLong("key-long", -1L))
        assertEquals(true, sharedPreferencesStrongBoxDisallowed.getBoolean("key-boolean", false))
        assertEquals(643455, sharedPreferencesStrongBoxDisallowed.getInt("key-int", -1))
        assertEquals(
            setOf("first", "second"),
            sharedPreferencesStrongBoxDisallowed.getStringSet("key-string-set", null)
        )
        assertFalse(sharedPreferencesStrongBoxDisallowed.contains("key-null"))
    }

    private fun createEncryptedFile(encryptionUtils: EncryptionUtils): EncryptedFileInfo {
        val encryptedSharedPreferencesUtils = EncryptedSharedPreferencesUtils(encryptionUtils)
        val encryptedFileUtils = EncryptedFileUtils(encryptionUtils)

        val migrationSharedPreferences = encryptedSharedPreferencesUtils.createGenericEncryptedSharedPreferences(
            context,
            encryptionUtils.getDefaultMasterKey(),
            SharedPrefsDelegate.migrationSharedPreferencesFileName
        )

        return encryptedFileUtils.createEncryptedFile(
            context,
            file,
            StrongBoxMigrationRetryChecker(
                StrongBoxMigrationRetryStorage(migrationSharedPreferences)
            )
        )
    }

    private fun createSharedPreferences(encryptionUtils: EncryptionUtils): SharedPreferences {
        val encryptedSharedPreferencesUtils = EncryptedSharedPreferencesUtils(encryptionUtils)

        val migrationSharedPreferences = encryptedSharedPreferencesUtils.createGenericEncryptedSharedPreferences(
            context,
            encryptionUtils.getDefaultMasterKey(),
            SharedPrefsDelegate.migrationSharedPreferencesFileName
        )

        return encryptedSharedPreferencesUtils.createEncryptedSharedPreferences(
            context,
            StrongBoxMigrationRetryChecker(
                StrongBoxMigrationRetryStorage(migrationSharedPreferences)
            )
        )
    }
}
