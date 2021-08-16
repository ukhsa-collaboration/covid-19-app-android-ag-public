package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Build.VERSION
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider.Companion.APP_AVAILABILITY_RESPONSE
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppAvailabilityProviderTest : ProviderTest<AppAvailabilityProvider, AppAvailabilityResponse?>() {

    override val getTestSubject = ::AppAvailabilityProvider
    override val property = AppAvailabilityProvider::appAvailability
    override val key = APP_AVAILABILITY_RESPONSE
    override val defaultValue: AppAvailabilityResponse? = null

    override val expectations: List<ProviderTestExpectation<AppAvailabilityResponse?>> = listOf(
        ProviderTestExpectation(json = appAvailableResponseJson, objectValue = appAvailableResponse),
        ProviderTestExpectation(json = minimumAppVersionGreaterAvailabilityResponseJson, objectValue = minimumAppVersionGreaterAvailabilityResponse),
        ProviderTestExpectation(json = null, objectValue = null, OBJECT_TO_JSON)
    )

    @Test
    fun `when appAvailability is null app is available`() {
        sharedPreferencesReturns(null)

        val result = testSubject.isAppAvailable()

        assertTrue(result)
    }

    @Test
    fun `when appAvailability is not null and app is available`() = runBlocking {
        sharedPreferencesReturns(appAvailableResponseJson)

        val result = testSubject.isAppAvailable()

        assertTrue(result)
    }

    @Test
    fun `when minimumSdkVersion greater app is not available`() = runBlocking {
        sharedPreferencesReturns(minimumAppVersionGreaterAvailabilityResponseJson)

        val result = testSubject.isAppAvailable()

        assertFalse(result)
    }

    @Test
    fun `when available minimumAppVersion greater app is not available`() = runBlocking {
        appAvailableResponse.minimumSdkVersion.value.inc()
        sharedPreferencesReturns(minimumSdkVersionGreaterAvailabilityResponseJson)

        val result = testSubject.isAppAvailable()

        assertFalse(result)
    }

    companion object {
        private fun appAvailabilityResponse(
            minAppVersion: Int = BuildConfig.VERSION_CODE,
            minSdkVersion: Int = VERSION.SDK_INT
        ) = AppAvailabilityResponse(
            MinimumAppVersion(
                TranslatableString(
                    mapOf()
                ),
                minAppVersion
            ),
            MinimumSdkVersion(
                TranslatableString(
                    mapOf()
                ),
                minSdkVersion
            ),
            RecommendedAppVersion(
                TranslatableString(
                    mapOf()
                ),
                BuildConfig.VERSION_CODE,
                title = TranslatableString(
                    mapOf()
                )
            )
        )

        private val appAvailableResponse = appAvailabilityResponse()
        private val appAvailableResponseJson = appAvailabilityResponseJson()

        private val minimumAppVersionGreaterAvailabilityResponse = appAvailabilityResponse(minAppVersion = BuildConfig.VERSION_CODE + 1)
        private val minimumAppVersionGreaterAvailabilityResponseJson = appAvailabilityResponseJson(minAppVersion = BuildConfig.VERSION_CODE + 1)

        private val minimumSdkVersionGreaterAvailabilityResponseJson = appAvailabilityResponseJson(minSdkVersion = VERSION.SDK_INT + 1)

        private fun appAvailabilityResponseJson(minAppVersion: Int = BuildConfig.VERSION_CODE, minSdkVersion: Int = 0) =
            """{"minimumAppVersion":{"description":{},"value":$minAppVersion},"minimumSDKVersion":{"description":{},"value":$minSdkVersion},"recommendedAppVersion":{"description":{},"value":${BuildConfig.VERSION_CODE},"title":{}}}"""
    }
}
