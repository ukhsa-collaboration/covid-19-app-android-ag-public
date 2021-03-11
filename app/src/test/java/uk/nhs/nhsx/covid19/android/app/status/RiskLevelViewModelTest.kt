package uk.nhs.nhsx.covid19.android.app.status

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.SOCIAL_DISTANCING
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk

class RiskLevelViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private val testSubject = RiskLevelViewModel(localAuthorityPostCodeProvider)

    private val showMassTestingObserver = mockk<Observer<Boolean>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.showMassTesting().observeForever(showMassTestingObserver)
    }

    @Test
    fun `return true when post district is in England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onHandleRiskLevel(risk)

        verify { showMassTestingObserver.onChanged(true) }
    }

    @Test
    fun `return false when post district is not in England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onHandleRiskLevel(risk)

        verify { showMassTestingObserver.onChanged(false) }
    }

    @Test
    fun `return false when PostalDistrictProviderWrapper returns null`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns null

        testSubject.onHandleRiskLevel(risk)

        verify { showMassTestingObserver.onChanged(false) }
    }

    @Test
    fun `return false when post district is in England and policy data is missing`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onHandleRiskLevel(risk.copy(riskIndicator = risk.riskIndicator.copy(policyData = null)))

        verify { showMassTestingObserver.onChanged(false) }
    }

    private val socialDistancingPolicy = Policy(
        policyIcon = SOCIAL_DISTANCING,
        policyHeading = Translatable(mapOf("en" to "Social distancing")),
        policyContent = Translatable(mapOf("en" to "Please keep a safe distance of at least 2 meters people not living in your household."))
    )

    private val risk = Risk(
        "SE1",
        RiskIndicator(
            colorScheme = RED,
            colorSchemeV2 = MAROON,
            name = Translatable(mapOf("en" to "SE1 is in Local Alert Level 4")),
            heading = Translatable(mapOf("en" to "Heading high")),
            content = Translatable(mapOf("en" to "Content high")),
            linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
            linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
            policyData = PolicyData(
                heading = Translatable(mapOf("en" to "Coronavirus cases are very high in your area")),
                content = Translatable(mapOf("en" to "Local Authority content high")),
                footer = Translatable(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                policies = listOf(socialDistancingPolicy),
                localAuthorityRiskTitle = Translatable(mapOf("en" to "SE1 is in local COVID alert level: high"))
            )
        ),
        riskLevelFromLocalAuthority = true
    )
}
