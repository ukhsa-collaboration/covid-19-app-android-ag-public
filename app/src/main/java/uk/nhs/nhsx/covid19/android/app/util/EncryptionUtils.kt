package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.security.crypto.MasterKeys
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.NOT_PRESENT
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_ALLOWED
import uk.nhs.nhsx.covid19.android.app.util.StrongBoxStatus.PRESENT_DISALLOWED

class EncryptionUtils(
    private val strongBoxSupport: StrongBoxSupport
) {

    internal fun getStrongBoxStatus(context: Context): StrongBoxStatus =
        strongBoxSupport.getStrongBoxStatus(context)

    internal fun getDefaultMasterKey(): String {
        // We can’t use UserAuthenticationRequired because that limits our background access to encrypted data
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

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

    class MigrationException(exception: Exception) : Exception(exception) {
        constructor(message: String) : this(Exception(message))
    }

    companion object {
        private const val STRONG_BOX_BACKED_MASTER_KEY = "_master_key_strongbox_"
    }
}

object AndroidStrongBoxSupport : StrongBoxSupport {

    private const val PIXEL_3 = "blueline"
    private const val PIXEL_3_XL = "crosshatch"
    private const val PIXEL_3A = "sargo"
    private const val PIXEL_3A_XL = "bonito"
    private const val PIXEL_4 = "flame"
    private const val PIXEL_4_XL = "coral"
    private const val PIXEL_4A = "sunfish"
    private const val PIXEL_4A_5G = "bramble"
    private const val PIXEL_5 = "redfin"
    private const val SAMSUNG_S20_5G = "x1q"

    override fun getStrongBoxStatus(context: Context): StrongBoxStatus =
        if (hasStrongBox(context)) {
            if (hasStrongBoxDisallowed()) {
                PRESENT_DISALLOWED
            } else {
                PRESENT_ALLOWED
            }
        } else {
            NOT_PRESENT
        }

    private fun hasStrongBox(context: Context) =
        Build.VERSION.SDK_INT >= 28 &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    private fun hasStrongBoxDisallowed() =
        when (Build.DEVICE) {
            PIXEL_3, PIXEL_3_XL, PIXEL_3A, PIXEL_3A_XL, PIXEL_4, PIXEL_4_XL, PIXEL_4A, PIXEL_4A_5G, PIXEL_5 -> true
            SAMSUNG_S20_5G -> BuildConfig.DEBUG
            else -> false
        }.also {
            if (it) Timber.d("StrongBox disallowed device: ${Build.DEVICE}")
        }
}

interface StrongBoxSupport {

    fun getStrongBoxStatus(context: Context): StrongBoxStatus
}

enum class StrongBoxStatus {
    NOT_PRESENT,
    PRESENT_ALLOWED,
    PRESENT_DISALLOWED
}
