package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodes
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse
import kotlin.test.assertEquals

class DownloadRiskyPostCodesWorkTest {

    private val riskyPostCodeApi = mockk<RiskyPostDistrictsApi>()
    private val postCodeProvider = mockk<PostCodeProvider>()
    private val riskyPostCodeIndicatorProvider = mockk<RiskyPostCodeIndicatorProvider>(relaxUnitFun = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val localAuthorityProvider = mockk<LocalAuthorityProvider>(relaxed = true)
    private val localAuthorityPostCodesLoader = mockk<LocalAuthorityPostCodesLoader>()

    private val testSubject = DownloadRiskyPostCodesWork(
        riskyPostCodeApi,
        postCodeProvider,
        riskyPostCodeIndicatorProvider,
        notificationProvider,
        localAuthorityProvider,
        localAuthorityPostCodesLoader
    )

    private val lowRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.GREEN,
        colorSchemeV2 = ColorScheme.GREEN,
        name = TranslatableString(mapOf("en" to "[postcode] is in local COVID area level: low")),
        heading = TranslatableString(mapOf("en" to "Heading low")),
        content = TranslatableString(
            mapOf(
                "en" to "Content low"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val mediumRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.YELLOW,
        colorSchemeV2 = ColorScheme.YELLOW,
        name = TranslatableString(mapOf("en" to "[postcode] is in local COVID area level: medium")),
        heading = TranslatableString(mapOf("en" to "Heading medium")),
        content = TranslatableString(
            mapOf(
                "en" to "Content medium"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val mediumRiskyPostCodeIndicatorWithPolicyData = mediumRiskyPostCodeIndicator.copy(
        policyData = PolicyData(
            heading = TranslatableString(mapOf("en" to "")),
            content = TranslatableString(mapOf("en" to "")),
            footer = TranslatableString(mapOf("en" to "")),
            localAuthorityRiskTitle = TranslatableString(mapOf("en" to "[local authority] ([postcode])")),
            policies = listOf()
        )
    )

    private val highRiskyPostCodeIndicator = RiskIndicator(
        colorScheme = ColorScheme.RED,
        colorSchemeV2 = ColorScheme.RED,
        name = TranslatableString(mapOf("en" to "[postcode] is in local COVID area level: high")),
        heading = TranslatableString(mapOf("en" to "Heading high")),
        content = TranslatableString(
            mapOf(
                "en" to "Content high"
            )
        ),
        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
        policyData = null
    )

    private val riskLevels = mapOf(
        "low" to lowRiskyPostCodeIndicator,
        "medium" to mediumRiskyPostCodeIndicator,
        "high" to highRiskyPostCodeIndicator
    )

    private val riskLevelsWithPolicyData = mapOf(
        "low" to lowRiskyPostCodeIndicator,
        "medium" to mediumRiskyPostCodeIndicatorWithPolicyData,
        "high" to highRiskyPostCodeIndicator
    )

    private val fetchRiskyPostCodeDistributionResponse = RiskyPostCodeDistributionResponse(
        postDistricts = mapOf(
            "A1" to "high",
            "CM1" to "high",
            "BE3" to "medium",
            "SE1" to "low",
            "AL1" to "neutral",
            "AL2" to "green"
        ),
        localAuthorities = mapOf(
            "E07000240" to "medium",
            "E07000241" to "low",
            "E07000242" to "high"
        ),
        riskLevels = riskLevels
    )

    private val localAuthorityPostCodes = LocalAuthorityPostCodes(
        postcodes = mapOf("SE1" to listOf("E07000241"), "BE3" to listOf("E07000240"), "A1" to listOf("E07000242")),
        localAuthorities = mapOf(
            "E07000241" to LocalAuthority(name = "ASd1", country = "Idnaodsna"),
            "E07000240" to LocalAuthority(name = "ASd0", country = "Idnaodsna"),
            "E07000242" to LocalAuthority(name = "ASd2", country = "Idnaodsna")
        )
    )

    @Before
    fun setUp() {
        statusScreenIsNotShowing()
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() } returns fetchRiskyPostCodeDistributionResponse
        coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `when RiskyPostCodeApi throws Exception then return failure`() = runBlocking {
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() } throws Exception()

        val result = testSubject()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `when RiskyPostCodeApi response is empty then return failure`() = runBlocking {
        val response = mockk<RiskyPostCodeDistributionResponse>()
        every { response.postDistricts } returns emptyMap()
        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() } returns response

        val result = testSubject()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `when PostCodeProvider does not contain any value then return success`() = runBlocking {
        every { postCodeProvider.value } returns null

        val result = testSubject()

        assertEquals(ListenableWorker.Result.success(), result)

        confirmVerified(localAuthorityProvider)
    }

    @Test
    fun `area risk changed from low to high and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasLow()
        postCodeListContainsMainPostCodeWithHighRiskLevel()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "high",
                    highRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: high"))),
                    riskLevelFromLocalAuthority = false
                )
        }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from medium to high and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasMedium()
        postCodeListContainsMainPostCodeWithHighRiskLevel()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "high",
                    highRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: high"))),
                    riskLevelFromLocalAuthority = false
                )
        }
        verify(exactly = 1) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from high to low and status screen not showing`() = runBlocking {
        mainPostCodeLevelWasHigh()
        postCodeListContainsMainPostCodeWithLowRiskLevel()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "low",
                    lowRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "SE1 is in local COVID area level: low"))),
                    riskLevelFromLocalAuthority = false
                )
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
                RiskIndicatorWrapper(
                    "high",
                    highRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: high"))),
                    riskLevelFromLocalAuthority = false
                )
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
                RiskIndicatorWrapper(
                    "medium",
                    mediumRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "BE3 is in local COVID area level: medium"))),
                    riskLevelFromLocalAuthority = false
                )
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
                RiskIndicatorWrapper(
                    "low",
                    lowRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "SE1 is in local COVID area level: low"))),
                    riskLevelFromLocalAuthority = false
                )
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
                RiskIndicatorWrapper(
                    "low",
                    lowRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "SE1 is in local COVID area level: low"))),
                    riskLevelFromLocalAuthority = false
                )
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
                RiskIndicatorWrapper(
                    "high",
                    highRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: high"))),
                    riskLevelFromLocalAuthority = false
                )
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `does not send notification when changing risk from null`() = runBlocking {
        mainPostCodeLevelWasNull()
        postCodeListContainsMainPostCodeWithHighRiskLevel()

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "high",
                    highRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: high"))),
                    riskLevelFromLocalAuthority = false
                )
        }
        verify(exactly = 0) { notificationProvider.showAreaRiskChangedNotification() }
    }

    @Test
    fun `area risk changed from low to medium based on local authority with no policy data`() = runBlocking {
        mainPostCodeLevelWasLow()

        every { postCodeProvider.value }.returns("A1")
        every { localAuthorityProvider.value } returns "E07000240"

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "medium",
                    mediumRiskyPostCodeIndicator.copy(name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: medium"))),
                    riskLevelFromLocalAuthority = true
                )
        }
    }

    @Test
    fun `area risk changed from low to medium based on local authority with policy data`() = runBlocking {
        mainPostCodeLevelWasLow()

        every { postCodeProvider.value }.returns("A1")
        every { localAuthorityProvider.value } returns "E07000240"

        coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() } returns
            fetchRiskyPostCodeDistributionResponse.copy(riskLevels = riskLevelsWithPolicyData)

        val checkAreaRiskChangedResult = testSubject()

        assert(checkAreaRiskChangedResult == ListenableWorker.Result.success())

        val expected = mediumRiskyPostCodeIndicator.copy(
            name = TranslatableString(mapOf("en" to "A1 is in local COVID area level: medium")),
            policyData = PolicyData(
                heading = TranslatableString(mapOf("en" to "")),
                content = TranslatableString(mapOf("en" to "")),
                footer = TranslatableString(mapOf("en" to "")),
                localAuthorityRiskTitle = TranslatableString(mapOf("en" to "ASd0 (A1)")),
                policies = listOf()
            )
        )

        verify(exactly = 1) {
            riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                RiskIndicatorWrapper(
                    "medium",
                    expected,
                    riskLevelFromLocalAuthority = true
                )
        }
    }

    @Test
    fun `no mapping from local authority to risk level returns risk level using post code without policy data`() =
        runBlocking {
            mainPostCodeLevelWasLow()

            every { postCodeProvider.value }.returns("BE3")
            every { localAuthorityProvider.value } returns "NOMAPPING"

            coEvery { riskyPostCodeApi.fetchRiskyPostCodeDistribution() } returns
                fetchRiskyPostCodeDistributionResponse.copy(riskLevels = riskLevelsWithPolicyData)

            testSubject()

            val expected = mediumRiskyPostCodeIndicatorWithPolicyData.copy(
                name = TranslatableString(mapOf("en" to "BE3 is in local COVID area level: medium")),
                policyData = null
            )

            verify(exactly = 1) {
                riskyPostCodeIndicatorProvider.riskyPostCodeIndicator =
                    RiskIndicatorWrapper(
                        "medium",
                        expected,
                        riskLevelFromLocalAuthority = false
                    )
            }
        }

    private fun postCodeListContainsMainPostCodeWithHighRiskLevel() {
        every { postCodeProvider.value }.returns("A1")
    }

    private fun postCodeListContainsMainPostCodeWithLowRiskLevel() {
        every { postCodeProvider.value }.returns("SE1")
    }

    private fun postCodeListContainsMainPostCodeWithMediumRiskLevel() {
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
