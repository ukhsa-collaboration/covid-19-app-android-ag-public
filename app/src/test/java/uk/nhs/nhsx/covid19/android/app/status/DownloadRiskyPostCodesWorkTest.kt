package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_POST_DISTRICTS
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.PostDistrictsResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM

class DownloadRiskyPostCodesWorkTest {

    private val riskyPostCodeApi = mockk<RiskyPostDistrictsApi>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val riskyPostCodeDetectedProvider = mockk<RiskyPostCodeDetectedProvider>(relaxed = true)
    private val areaRiskChangedProvider = mockk<AreaRiskChangedProvider>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)

    private val testSubject = DownloadRiskyPostCodesWork(
        riskyPostCodeApi,
        postCodeProvider,
        riskyPostCodeDetectedProvider,
        areaRiskChangedProvider,
        notificationProvider
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

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(HIGH) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from medium to high and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasMedium()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(HIGH) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from high to low and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsNotShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from low to high and status screen showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(HIGH) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from low to medium and status screen showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithMediumRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(MEDIUM) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from high to low and status screen showing`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify { areaRiskChangedProvider setProperty "value" value eq(true) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk not changed and main post code not in list`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithLowRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify { areaRiskChangedProvider setProperty "value" value eq(false) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk not changed and main post code still in list`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithHighRiskLevel()
        statusScreenIsShowing()

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(HIGH) }
        verify { areaRiskChangedProvider setProperty "value" value eq(false) }
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

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 0) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify(exactly = 0) { areaRiskChangedProvider setProperty "value" value eq(false) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `fetching risky post code throws exception`() = runBlocking {
        mainPostCodeLevelWasLow()
        statusScreenIsShowing()

        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.throws(Exception())

        every { postCodeProvider.value }.returns("A1")

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.failure())

        verify(exactly = 0) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify(exactly = 0) { areaRiskChangedProvider setProperty "value" value eq(false) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `risky post districts feature toggled off when work is invoked`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(HIGH_RISK_POST_DISTRICTS)

        val checkAreaRiskChangedResult = testSubject.doWork()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.Success())

        verify(exactly = 0) { riskyPostCodeDetectedProvider.setRiskyPostCodeLevel(LOW) }
        verify(exactly = 0) { areaRiskChangedProvider setProperty "value" value eq(false) }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    private fun postCodeListContainsMainPostCodeWithHighRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.returns(
            PostDistrictsResponse(
                mapOf(
                    "A1" to HIGH,
                    "CM1" to HIGH,
                    "BE3" to MEDIUM,
                    "SE1" to LOW
                )
            )
        )
        every { postCodeProvider.value }.returns("A1")
    }

    private fun postCodeListContainsMainPostCodeWithLowRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.returns(
            PostDistrictsResponse(
                mapOf(
                    "A1" to HIGH,
                    "CM1" to HIGH,
                    "BE3" to MEDIUM,
                    "SE1" to LOW
                )
            )
        )
        every { postCodeProvider.value }.returns("SE1")
    }

    private fun postCodeListContainsMainPostCodeWithMediumRiskLevel() {
        coEvery { riskyPostCodeApi.fetchRiskyPostDistricts() }.returns(
            PostDistrictsResponse(
                mapOf(
                    "A1" to HIGH,
                    "CM1" to HIGH,
                    "BE3" to MEDIUM,
                    "SE1" to LOW
                )
            )
        )
        every { postCodeProvider.value }.returns("BE3")
    }

    private fun mainPostCodeLevelWasHigh() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(HIGH)
    }

    private fun mainPostCodeLevelWasMedium() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(MEDIUM)
    }

    private fun mainPostCodeLevelWasLow() {
        every { riskyPostCodeDetectedProvider.toRiskLevel() }.returns(LOW)
    }

    private fun statusScreenIsNotShowing() {
        StatusActivity.isVisible = false
    }

    private fun statusScreenIsShowing() {
        StatusActivity.isVisible = true
    }
}
