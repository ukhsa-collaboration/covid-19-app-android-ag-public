package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import java.time.Instant

class KeySharingInfoProviderTest : ProviderTest<KeySharingInfoProvider, KeySharingInfo?>() {

    override val getTestSubject = ::KeySharingInfoProvider
    override val property = KeySharingInfoProvider::keySharingInfo
    override val key = KeySharingInfoProvider.VALUE_KEY
    override val defaultValue: KeySharingInfo? = null
    override val expectations: List<ProviderTestExpectation<KeySharingInfo?>> = listOf(
        ProviderTestExpectation(json = null, objectValue = null, OBJECT_TO_JSON),
        ProviderTestExpectation(json = keySharingInfoJson(), objectValue = keySharingInfo),
        ProviderTestExpectation(
            json = keySharingInfoJsonWithAdditionalProperties(),
            objectValue = keySharingInfo.copy(notificationSentDate = Instant.parse(notificationSentDate)),
            direction = JSON_TO_OBJECT
        ),

    )

    @Test
    fun `test setting notificationSent true`() {
        sharedPreferencesReturns(keySharingInfoJson())
        val notificationSentDate = "2020-07-26T10:00:00Z"

        testSubject.setNotificationSentDate(notificationSentDate = Instant.parse(notificationSentDate))

        assertSharedPreferenceSetsValue(keySharingInfoJson(notificationSentDate = notificationSentDate))
    }

    @Test
    fun `test setting hasDeclinedSharingKeys true`() {
        sharedPreferencesReturns(keySharingInfoJson())

        testSubject.setHasDeclinedSharingKeys()

        assertSharedPreferenceSetsValue(keySharingInfoJson(hasDeclinedSharingKeys = true))
    }

    @Test
    fun `test calling reset`() {
        sharedPreferencesReturns(keySharingInfoJson())

        testSubject.reset()

        assertSharedPreferenceSetsValue(null)
    }

    companion object {
        private const val token = "token"
        private const val acknowledgedDate = "2020-07-25T10:00:00Z"
        private const val notificationSentDate = "2020-07-25T11:00:00Z"
        private val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = token,
            acknowledgedDate = Instant.parse(acknowledgedDate),
            notificationSentDate = null,
            hasDeclinedSharingKeys = false
        )

        private fun keySharingInfoJson(hasDeclinedSharingKeys: Boolean = false) =
            """{"diagnosisKeySubmissionToken":"$token","acknowledgedDate":"$acknowledgedDate","hasDeclinedSharingKeys":$hasDeclinedSharingKeys}"""

        private fun keySharingInfoJson(notificationSentDate: String, hasDeclinedSharingKeys: Boolean = false) =
            """{"diagnosisKeySubmissionToken":"$token","acknowledgedDate":"$acknowledgedDate","notificationSentDate":"$notificationSentDate","hasDeclinedSharingKeys":$hasDeclinedSharingKeys}"""

        private fun keySharingInfoJsonWithAdditionalProperties() =
            """{"diagnosisKeySubmissionToken":"$token","acknowledgedDate":"$acknowledgedDate","notificationSentDate":"2020-07-25T11:00:00Z","hasDeclinedSharingKeys":false,"testKitType":"LAB_RESULT"}"""
    }
}
