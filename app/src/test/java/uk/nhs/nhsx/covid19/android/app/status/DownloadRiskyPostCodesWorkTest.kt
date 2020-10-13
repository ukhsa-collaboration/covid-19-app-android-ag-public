package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_POST_DISTRICTS
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.PostDistrictsResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse

class DownloadRiskyPostCodesWorkTest {

    private val riskyPostCodeApi = mockk<RiskyPostDistrictsApi>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val riskyPostCodeIndicatorProvider =
        mockk<RiskyPostCodeIndicatorProvider>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)

    private val testSubject = DownloadRiskyPostCodesWork(
        riskyPostCodeApi,
        postCodeProvider,
        riskyPostCodeIndicatorProvider,
        notificationProvider
    )

    private val lowRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.GREEN,
        name = Translatable(mapOf("en" to "low")),
        heading = Translatable(mapOf("en" to "Heading low")),
        content = Translatable(
            mapOf(
                "en" to "Content low"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
    )

    private val mediumRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.YELLOW,
        name = Translatable(mapOf("en" to "medium")),
        heading = Translatable(mapOf("en" to "Heading medium")),
        content = Translatable(
            mapOf(
                "en" to "Content medium"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
    )

    private val highRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.RED,
        name = Translatable(mapOf("en" to "high")),
        heading = Translatable(mapOf("en" to "Heading high")),
        content = Translatable(
            mapOf(
                "en" to "Content high"
            )
        ),
        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
    )

    private val fetchRiskyPostCodeDistributionResponse = RiskyPostCodeDistributionResponse(
        postDistricts = mapOf(
            "A1" to "high",
            "CM1" to "high",
            "BE3" to "medium",
            "SE1" to "low"
        ),
        riskLevels = mapOf(
            "low" to lowRiskyPostCodeIndicator,
            "medium" to mediumRiskyPostCodeIndicator,
            "high" to highRiskyPostCodeIndicator
        )
    )

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `area risk changed from low to high and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("high", highRiskyPostCodeIndicator)
        }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from medium to high and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasMedium()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("high", highRiskyPostCodeIndicator)
        }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from high to low and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from low to high and status screen showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("high", highRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from low to medium and status screen showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithMediumRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("medium", mediumRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from high to low and status screen showing`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk not changed and main post code not in list`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk not changed and main post code still in list`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("high", highRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `not found post code in fetching risky post code falls back to old api endpoint`() = runBlocking {
        mainPostCodeLevelWasHigh()

        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() }.returns(
            fetchRiskyPostCodeDistributionResponse
        )
        every { postCodeProvider.value }.returns("CF11")

        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.returns(
            PostDistrictsResponse(
                mapOf("CF11" to RiskLevel.HIGH)
            )
        )

        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        coVerify(exactly = 1) { riskyPostCodeApi.fetchRiskyPostDistricts() }

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(oldRiskLevel = RiskLevel.HIGH)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `fetching risky post code returns empty list`() = runBlocking {
        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.returns(
            PostDistrictsResponse(
                mapOf()
            )
        )

        every { postCodeProvider.value }.returns("A1")

        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 0) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `fetching risky post code throws exception`() = runBlocking {
        mainPostCodeLevelWasLow()
        statusScreenIsShowing()

        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.throws(Exception())

        every { postCodeProvider.value }.returns("A1")

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.failure())

        verify(exactly = 0) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `risky post districts feature toggled off when work is invoked`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(HIGH_RISK_POST_DISTRICTS)

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.Success())

        verify(exactly = 0) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("low", lowRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `does not send notification when changing risk from null`() = runBlocking {
        mainPostCodeLevelWasNull()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper("high", highRiskyPostCodeIndicator)
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    private fun postCodeListContainsMainPostCodeWithHighRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() }.returns(
            fetchRiskyPostCodeDistributionResponse
        )
        every { postCodeProvider.value }.returns("A1")
    }

    private fun postCodeListContainsMainPostCodeWithLowRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() }.returns(
            fetchRiskyPostCodeDistributionResponse
        )
        every { postCodeProvider.value }.returns("SE1")
    }

    private fun postCodeListContainsMainPostCodeWithMediumRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() }.returns(
            fetchRiskyPostCodeDistributionResponse
        )
        every { postCodeProvider.value }.returns("BE3")
    }

    private fun mainPostCodeLevelWasNull() {
        every { riskyPostCodeIndicatorProvider.riskyPostCodeIndicator }.returns(null)
    }

    private fun mainPostCodeLevelWasHigh() {
        every { riskyPostCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "high", highRiskyPostCodeIndicator
            )
        )
    }

    private fun mainPostCodeLevelWasMedium() {
        every { riskyPostCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "medium",
                mediumRiskyPostCodeIndicator
            )
        )
    }

    private fun mainPostCodeLevelWasLow() {
        every { riskyPostCodeIndicatorProvider.riskyPostCodeIndicator }.returns(
            RiskIndicatorWrapper(
                "low",
                lowRiskyPostCodeIndicator
            )
        )
    }

    private fun statusScreenIsNotShowing() {
        StatusActivity.isVisible = false
    }

    private fun statusScreenIsShowing() {
        StatusActivity.isVisible = true
    }
}
