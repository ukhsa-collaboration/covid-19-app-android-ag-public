package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion

class UpdateRecommendedViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val observer = mockk<Observer<UpdateRecommendedViewModel.RecommendationInfo>>(relaxed = true)

    private val testSubject =
        UpdateRecommendedViewModel(
            appAvailabilityProvider
        )

    private val appAvailableResponse = AppAvailabilityResponse(
        MinimumAppVersion(
            TranslatableString(mapOf("en" to "en")),
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
                mapOf("en-GB" to "description")
            ),
            BuildConfig.VERSION_CODE,
            title = TranslatableString(
                mapOf("en-GB" to "title")
            )
        )
    )

    @Before
    fun setUp() {
        testSubject.observeRecommendationInfo().observeForever(observer)
    }

    @Test
    fun `can fetch recommendation info`() = runBlocking {
        every { appAvailabilityProvider.appAvailability } returns appAvailableResponse
        testSubject.fetchRecommendationInfo()
        verify { observer.onChanged(UpdateRecommendedViewModel.RecommendationInfo("title", "description")) }
    }
}
