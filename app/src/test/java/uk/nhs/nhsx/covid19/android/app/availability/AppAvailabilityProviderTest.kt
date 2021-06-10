package uk.nhs.nhsx.covid19.android.app.availability

import android.content.SharedPreferences
import android.os.Build
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppAvailabilityProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private val moshi = mockk<Moshi>(relaxed = true)
    private val moshiAdapter = mockk<JsonAdapter<AppAvailabilityResponse>>(relaxed = true)

    private val testSubject = AppAvailabilityProvider(sharedPreferences, moshi)

    private val appAvailableResponse = AppAvailabilityResponse(
        MinimumAppVersion(
            TranslatableString(
                mapOf()
            ),
            BuildConfig.VERSION_CODE
        ),
        MinimumSdkVersion(
            TranslatableString(
                mapOf()
            ),
            Build.VERSION.SDK_INT
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

    private val minimumAppVersionGreaterAvailabilityResponse = AppAvailabilityResponse(
        MinimumAppVersion(
            TranslatableString(
                mapOf()
            ),
            BuildConfig.VERSION_CODE + 1
        ),
        MinimumSdkVersion(
            TranslatableString(
                mapOf()
            ),
            Build.VERSION.SDK_INT
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

    private val minimumSdkVersionGreaterAvailabilityResponse = AppAvailabilityResponse(
        MinimumAppVersion(
            TranslatableString(
                mapOf()
            ),
            BuildConfig.VERSION_CODE
        ),
        MinimumSdkVersion(
            TranslatableString(
                mapOf()
            ),
            Build.VERSION.SDK_INT + 1
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

    @Before
    fun setUp() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { moshi.adapter<AppAvailabilityResponse>(AppAvailabilityResponse::class.java) } returns moshiAdapter
    }

    @Test
    fun `when appAvailability is null app is available`() {
        every { testSubject.appAvailability } returns null

        val result = testSubject.isAppAvailable()

        assertTrue(result)
    }

    @Test
    fun `when appAvailability is not null and app is available`() = runBlocking {
        every { sharedPreferences.all["APP_AVAILABILITY_RESPONSE"] } returns ""
        every { moshiAdapter.fromJson(any<String>()) } returns appAvailableResponse

        val result = testSubject.isAppAvailable()

        assertTrue(result)
    }

    @Test
    fun `when minimumSdkVersion greater app is not available`() = runBlocking {
        every { sharedPreferences.all["APP_AVAILABILITY_RESPONSE"] } returns ""
        every { moshiAdapter.fromJson("") } returns minimumAppVersionGreaterAvailabilityResponse

        val result = testSubject.isAppAvailable()

        assertFalse(result)
    }

    @Test
    fun `when available minimumAppVersion greater app is not available`() = runBlocking {
        appAvailableResponse.minimumSdkVersion.value.inc()
        every { sharedPreferences.all["APP_AVAILABILITY_RESPONSE"] } returns ""
        every { moshiAdapter.fromJson("") } returns minimumSdkVersionGreaterAvailabilityResponse

        val result = testSubject.isAppAvailable()

        assertFalse(result)
    }
}
