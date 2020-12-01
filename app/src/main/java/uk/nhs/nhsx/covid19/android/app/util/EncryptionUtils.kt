package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedFile.Builder
import androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import timber.log.Timber
import java.io.File
import java.io.OutputStreamWriter

data class EncryptedFileInfo(
    val file: File,
    val encryptedFile: EncryptedFile
)

object EncryptionUtils {
    private const val STRONG_BOX_BACKED_MASTER_KEY = "_master_key_strongbox_"
    internal const val KEYSET_PREF_NAME = "__androidx_security_crypto_encrypted_file_pref__"
    private const val STRONGBOX_KEYSET_PREF_NAME =
        "__androidx_security_crypto_encrypted_file_strongbox_pref__"

    fun createEncryptedFile(
        context: Context,
        name: String,
        migrationManager: StrongBoxMigrationManager = StrongBoxMigrationManager
    ): EncryptedFileInfo {
        val (file, encryptedFile) = if (hasStrongBox(context)) {
            val file = File(context.filesDir, name + "_strongbox")
            Timber.d("Has StrongBox")
            val strongBoxMasterKeyAlias = getStrongBoxBackedMasterKey()
            val strongBoxBackedEncryptedFile =
                getEncryptedFile(context, file, strongBoxMasterKeyAlias, STRONGBOX_KEYSET_PREF_NAME)
            val oldFile = File(context.filesDir, name)
            migrationManager.migrateToNewMasterKey(context, oldFile, strongBoxBackedEncryptedFile)

            file to strongBoxBackedEncryptedFile
        } else {
            val file = File(context.filesDir, name)
            val masterKeyAlias = getDefaultMasterKey()
            file to getEncryptedFile(context, file, masterKeyAlias, KEYSET_PREF_NAME)
        }

        return EncryptedFileInfo(
            file,
            encryptedFile
        )
    }

    fun getEncryptedFile(
        context: Context,
        file: File,
        masterKeyAlias: String,
        keySetPrefName: String
    ): EncryptedFile {
        return Builder(file, context, masterKeyAlias, AES256_GCM_HKDF_4KB)
            .setKeysetPrefName(keySetPrefName)
            .build()
    }

    internal fun getDefaultMasterKey(): String {
        // We can’t use UserAuthenticationRequired because that limits our background access to encrypted data
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    @VisibleForTesting
    @RequiresApi(VERSION_CODES.P)
    internal fun getStrongBoxBackedMasterKey(): String {
        // We can’t use UserAuthenticationRequired because that limits our background access to encrypted data
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            STRONG_BOX_BACKED_MASTER_KEY,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setIsStrongBoxBacked(true)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return MasterKeys.getOrCreate(keyGenParameterSpec)
    }

    @VisibleForTesting
    internal fun hasStrongBox(context: Context) = Build.VERSION.SDK_INT >= 28 &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return if (hasStrongBox(context)) {
            Timber.d("Has StrongBox")
            createStrongBoxBackedEncryptedSharedPreferences(context)
        } else {
            createEncryptedSharedPreferences(context, getDefaultMasterKey())
        }
    }

    @RequiresApi(VERSION_CODES.P)
    private fun createStrongBoxBackedEncryptedSharedPreferences(context: Context): SharedPreferences {
        val strongBoxMasterKeyAlias = getStrongBoxBackedMasterKey()
        val strongBoxBackedPrefs = createEncryptedSharedPreferences(
            context,
            strongBoxMasterKeyAlias,
            "strongBoxBackedPrefs"
        )

        migrateSharedPrefsToNewMasterKey(context, strongBoxBackedPrefs)
        return strongBoxBackedPrefs
    }

    private fun migrateSharedPrefsToNewMasterKey(
        context: Context,
        strongBoxBackedPrefs: SharedPreferences
    ) {
        val oldPrefs = createEncryptedSharedPreferences(context, getDefaultMasterKey())
        val allPrefs = oldPrefs.all

        strongBoxBackedPrefs.edit {
            allPrefs.forEach { entry ->
                val key = entry.key
                when (val value = entry.value) {
                    is String -> putString(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Set<*> -> putStringSet(key, value as MutableSet<String>?)
                    null -> remove(key)
                }
            }
        }

        oldPrefs.edit().clear().apply()
        Timber.d("Migration finished")
    }

    internal fun createEncryptedSharedPreferences(
        context: Context,
        masterKeyAlias: String,
        fileName: String = SharedPrefsDelegate.fileName
    ) =
        EncryptedSharedPreferences.create(
            fileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

fun EncryptedFile.readText(): String {
    val inputStream = openFileInput()
    return inputStream.bufferedReader().use { it.readText() }
}

fun EncryptedFile.writeText(content: String) {
    val writer = OutputStreamWriter(openFileOutput(), Charsets.UTF_8)
    writer.use { it.write(content) }
}
