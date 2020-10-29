package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_VENUES
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class DownloadAndProcessRiskyVenuesTest {

    private val riskyVenuesApi = mockk<RiskyVenuesApi>()
    private val venueMatchFinder = mockk<VenueMatchFinder>(relaxed = true)
    private val visitedVenueStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val filterOutdatedVisits = mockk<FilterOutdatedVisits>()
    private val riskyVenuesCircuitBreakerPolling = mockk<RiskyVenuesCircuitBreakerPolling>()
    private val riskyVenueCircuitBreakerConfigurationProvider = mockk<RiskyVenueCircuitBreakerConfigurationProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-10-07T00:05:00.00Z"), ZoneOffset.UTC)

    private val testSubject = DownloadAndProcessRiskyVenues(
        riskyVenuesApi,
        venueMatchFinder,
        visitedVenueStorage,
        filterOutdatedVisits,
        riskyVenuesCircuitBreakerPolling,
        riskyVenueCircuitBreakerConfigurationProvider,
        fixedClock
    )

    private val riskyVenues = listOf(
        RiskyVenue(
            "1",
            RiskyWindow(
                from = Instant.parse("2020-07-08T10:00:00.00Z"),
                to = Instant.parse("2020-07-08T12:00:00.00Z")
            )
        )
    )

    private val venueVisits = listOf(
        VenueVisit(
            venue = Venue(
                "1",
                "Venue1"
            ),
            from = Instant.parse("2020-07-08T10:00:00.00Z"),
            to = Instant.parse("2020-07-08T12:00:00.00Z"),
            wasInRiskyList = false
        )
    )

    @Before
    fun setUp() {
        every { filterOutdatedVisits.invoke(any()) } returns listOf()
        coEvery { visitedVenueStorage.getVisits() } returns listOf()
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `calls api endpoint`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(venues = listOf())

        testSubject()

        coVerify { riskyVenuesApi.getListOfRiskyVenues() }
    }

    @Test
    fun `high-risk venues feature toggled off when work is invoked`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(HIGH_RISK_VENUES)

        testSubject()

        coVerify(exactly = 0) { visitedVenueStorage.getVisits() }
        verify(exactly = 0) { filterOutdatedVisits.invoke(any()) }
        coVerify(exactly = 0) { visitedVenueStorage.setVisits(any()) }
        coVerify(exactly = 0) { riskyVenuesApi.getListOfRiskyVenues() }
    }

    @Test
    fun `does not call findMatches if risky venues are empty`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(venues = listOf())

        testSubject()

        coVerify(exactly = 0) { venueMatchFinder.findMatches(any()) }
    }

    @Test
    fun `when no matches found circuit breaker initialization is not scheduled`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(venues = riskyVenues)
        coEvery { venueMatchFinder.findMatches(any()) } returns emptyList()

        testSubject()

        coVerify(exactly = 0) { visitedVenueStorage.markAsWasInRiskyList(emptyList()) }
        coVerify(exactly = 0) { riskyVenueCircuitBreakerConfigurationProvider.addAll(any()) }
    }

    @Test
    fun `when matches found circuit breaker initialization is scheduled`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(venues = riskyVenues)
        coEvery { venueMatchFinder.findMatches(any()) } returns listOf(riskyVenues[0].id)

        testSubject()

        coVerify(exactly = 1) { visitedVenueStorage.markAsWasInRiskyList(listOf(riskyVenues[0].id)) }
        val slot = slot<List<RiskyVenueCircuitBreakerConfiguration>>()
        coVerify(exactly = 1) { riskyVenueCircuitBreakerConfigurationProvider.addAll(capture(slot)) }

        val expected = RiskyVenueCircuitBreakerConfiguration(
            startedAt = Instant.now(fixedClock),
            venueId = riskyVenues[0].id,
            approvalToken = null,
            isPolling = false
        )

        assertEquals(1, slot.captured.size)
        assertEquals(expected, slot.captured[0])
    }
}
