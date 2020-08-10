package uk.nhs.nhsx.covid19.android.app.qrcode

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.jsonwebtoken.Jwts
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

@JsonClass(generateAdapter = true)
class JwtHeader(
    val kid: String
)

class Jwt(
    val header: JwtHeader,
    val venue: Venue
)

class QrCodeParser @Inject constructor(
    private val base64Decoder: Base64Decoder,
    moshi: Moshi,
    private val signatureKey: SignatureKey
) {
    private val venueAdapter = moshi.adapter(Venue::class.java)
    private val jwtHeaderAdapter = moshi.adapter(JwtHeader::class.java)

    fun parse(rawValue: String): Venue {
        val split = rawValue.split(":")
        if (split.size != 3) {
            throw IllegalArgumentException("Invalid QR code content")
        }
        val (constant, version, jwtString) = split
        if (constant != QR_CODE_CONSTANT) {
            throw IllegalArgumentException("Invalid QR code content")
        }
        if (!isSupportedVersion(version)) {
            throw IllegalArgumentException("Unsupported version")
        }

        val jwt = try {
            parseJWT(jwtString)
        } catch (exception: java.lang.Exception) {
            throw IllegalArgumentException("Invalid JWT", exception)
        }

        if (!hasValidSignature(jwtString, jwt.header.kid)) {
            throw IllegalArgumentException("QR code signature validation failed")
        }
        return jwt.venue
    }

    private fun parseJWT(jwt: String): Jwt {
        val components = jwt.split(".")
        return when (components.size) {
            3 -> {
                val header = jwtHeaderAdapter.fromJson(base64Decoder.decodeUrl(components[0]))
                    ?: throw throw IllegalArgumentException("Error decoding header")
                val venue = venueAdapter.fromJson(base64Decoder.decodeUrl(components[1]))
                    ?: throw throw IllegalArgumentException("Error decoding payload")
                Jwt(header, venue)
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun hasValidSignature(jwtString: String, keyId: String): Boolean {
        if (signatureKey.id != keyId) {
            throw IllegalArgumentException("Unknown keyId received: $keyId")
        }

        return runCatching {

            val undecoratedString = signatureKey.pemRepresentation
                .split("\n")
                .filter { !(it.isEmpty() || it.startsWith("-----")) }
                .joinToString("")
            val publicKeyBytes: ByteArray = base64Decoder.decodeToBytes(undecoratedString)

            val publicKey =
                KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyBytes))
            Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parse(jwtString)
            true
        }.getOrElse {
            print(it)
            false
        }
    }

    private fun isSupportedVersion(version: String): Boolean {
        val versionCode = version.toIntOrNull() ?: return false
        return versionCode >= VERSION
    }

    companion object {
        const val QR_CODE_CONSTANT = "UKC19TRACING"
        const val VERSION = 1
    }
}
