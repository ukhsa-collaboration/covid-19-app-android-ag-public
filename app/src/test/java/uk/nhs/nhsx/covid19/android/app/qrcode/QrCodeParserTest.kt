package uk.nhs.nhsx.covid19.android.app.qrcode

import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.Test.None
import uk.nhs.covid19.config.SignatureKey
import uk.nhs.nhsx.covid19.android.app.utils.Java8Base64Decoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QrCodeParserTest {

    private val moshi = Moshi.Builder().build()
    private val base64Decoder = Java8Base64Decoder()
    private val signatureKey = SignatureKey(
        id = "3",
        pemRepresentation =
            """
    -----BEGIN PUBLIC KEY-----
    MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9
    q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==
    -----END PUBLIC KEY-----
            """.trimIndent()
    )

    private val testSubject = QrCodeParser(base64Decoder, moshi, signatureKey)

    @Test
    fun `valid QR code`() {
        val payload =
            "UKC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"
        val venue = Venue(
            id = "4WT59M5Y",
            organizationPartName = "Government Office Of Human Resources"
        )

        assertEquals(venue, testSubject.parse(payload))
    }

    @Test(expected = None::class)
    fun `future versions are allowed`() {
        val payload =
            "UKC19TRACING:2:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"
        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid QR code with redundant segments`() {
        val payload =
            "UKC19TRACING:1:2:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"
        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with invalid prefix`() {
        val payload =
            "DEC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid QR code without prefix`() {
        val payload =
            "1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `qr code with version zero`() {
        val payload =
            "UKC19TRACING:0:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with empty version`() {
        val payload =
            "UKC19TRACING::eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with invalid version`() {
        val payload =
            "UKC19TRACING:Z:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid qr code without version`() {
        val payload =
            "UKC19TRACING:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"

        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with invalid key is rejected`() {
        val payload =
            "UKC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Im5vdC1teS1rZXkifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.B5zt7ZOZ-SzoR2wqaGHJ4GJrTuJuuFXTznAPnMN9Z5NNkJmI6fyYIokCLPdYwWowqZrvfO4O8pqR-BWoQyjAzA"
        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with invalid signature is rejected`() {
        val payload =
            "UKC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiTXkgR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZX2PHtcxFmNwyrHRIbtcQCAHdhwUTb1h1GqVTNqAK7WiE_qO1nbJVU3BWvRu15DR6WBF14MN1I8yC6B1uK7ghg"
        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `QR code with missing signature is rejected`() {
        val payload =
            "UKC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiTXkgR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0."
        testSubject.parse(payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid QR code with non base64 encoded payload`() {
        val payload = "UKC19TRACING:1:$§fg§:"
        testSubject.parse(payload)
    }

    @Test
    fun `QR code with alg-none is rejected`() {
        val testSubject = QrCodeParser(base64Decoder, moshi, signatureKey)

        assertFailsWith(IllegalArgumentException::class, "QR code signature validation failed") {
            testSubject.parse("UKC19TRACING:1:eyJhbGciOiJub25lIiwidHlwIjoiSldUIiwia2lkIjoiMyJ9.eyJpZCI6IkZBS0VfSUQiLCJ0eXAiOiJlbnRyeSIsIm9wbiI6IkZBS0UgTkFNRSIsImFkciI6IkZBS0UgQUREUkVTUyIsInB0IjoiVU5JVEVEIEtJTkdET00iLCJwYyI6IkZBSyJ9.ZX2PHtcxFmNwyrHRIbtcQCAHdhwUTb1h1GqVTNqAK7WiE_qO1nbJVU3BWvRu15DR6WBF14MN1I8yC6B1uK7ghg")
        }
    }

    @Test
    fun `QR code with alg-none no signature is rejected`() {
        val testSubject = QrCodeParser(base64Decoder, moshi, signatureKey)

        assertFailsWith(IllegalArgumentException::class, "QR code signature validation failed") {
            testSubject.parse("UKC19TRACING:1:eyJhbGciOiJub25lIiwidHlwIjoiSldUIiwia2lkIjoiMyJ9.eyJpZCI6IkZBS0VfSUQiLCJ0eXAiOiJlbnRyeSIsIm9wbiI6IkZBS0UgTkFNRSIsImFkciI6IkZBS0UgQUREUkVTUyIsInB0IjoiVU5JVEVEIEtJTkdET00iLCJwYyI6IkZBSyJ9.")
        }
    }
}
