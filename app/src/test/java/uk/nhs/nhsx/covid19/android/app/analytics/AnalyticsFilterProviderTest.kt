package uk.nhs.nhsx.covid19.android.app.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.IS_ISOLATING_FOR_SELF_DIAGNOSED_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature

class AnalyticsFilterProviderTest {
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val testSubject = AnalyticsFilterProvider(localAuthorityPostCodeProvider)

    @Test
    fun `when risky contact feature flag disabled for wales, should filter events`() = runBlocking {
        val expectedValue = true
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        coRunWithFeature(feature = OLD_WALES_CONTACT_CASE_FLOW, enabled = false) {
            assert(testSubject.invoke().shouldFilterRiskyContactInfo == expectedValue)
        }
    }

    @Test
    fun `when risky contact feature flag disabled for england, should filter events`() = runBlocking {
        val expectedValue = true
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = OLD_ENGLAND_CONTACT_CASE_FLOW, enabled = false) {
            assert(testSubject.invoke().shouldFilterRiskyContactInfo == expectedValue)
        }
    }

    @Test
    fun `when risky contact feature flag enabled for wales, should not filter events`() = runBlocking {
        val expectedValue = false
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        coRunWithFeature(feature = OLD_WALES_CONTACT_CASE_FLOW, enabled = true) {
            assert(testSubject.invoke().shouldFilterRiskyContactInfo == expectedValue)
        }
    }

    @Test
    fun `when risky contact feature flag enabled for england, should not filter events`() = runBlocking {
        val expectedValue = false
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = OLD_ENGLAND_CONTACT_CASE_FLOW, enabled = true) {
            assert(testSubject.invoke().shouldFilterRiskyContactInfo == expectedValue)
        }
    }

    @Test
    fun `when self isolation hub feature flag disabled for wales, should filter events`() = runBlocking {
        val expectedValue = true
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        coRunWithFeature(feature = SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES, enabled = false) {
            assert(testSubject.invoke().shouldFilterSelfIsolation == expectedValue)
        }
    }

    @Test
    fun `when self isolation hub feature flag disabled for england, should filter events`() = runBlocking {
        val expectedValue = true
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND, enabled = false) {
            assert(testSubject.invoke().shouldFilterSelfIsolation == expectedValue)
        }
    }

    @Test
    fun `on self isolation hub feature flag enabled for wales, should not filter events`() = runBlocking {
        val expectedValue = false
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        coRunWithFeature(feature = SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES, enabled = true) {
            assert(testSubject.invoke().shouldFilterSelfIsolation == expectedValue)
        }
    }

    @Test
    fun `when self isolation hub flag enabled for england, should not filter events`() = runBlocking {
        val expectedValue = false
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND, enabled = true) {
            assert(testSubject.invoke().shouldFilterSelfIsolation == expectedValue)
        }
    }

    @Test
    fun `when venue check-in feature flag disabled, should filter events`() = runBlocking {
        val expectedValue = true
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = false) {
            assert(testSubject.invoke().shouldFilterVenueCheckIn == expectedValue)
        }
    }

    @Test
    fun `when venue check-in feature flag enabled, should not filter events`() = runBlocking {
        val expectedValue = false
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = true) {
            assert(testSubject.invoke().shouldFilterVenueCheckIn == expectedValue)
        }
    }

    @Test
    fun `filter symptomatic questionnaire events`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        val expectedValue = listOf(
            DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY,
            IS_ISOLATING_FOR_SELF_DIAGNOSED_BACKGROUND_TICK,
            COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
        )

        assert(testSubject.invoke().enabledCustomAnalyticsFilters == expectedValue)
    }
}
