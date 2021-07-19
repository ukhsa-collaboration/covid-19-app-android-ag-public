package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import java.time.Instant
import kotlin.test.assertEquals

class DownloadAndProcessRiskyVenuesTest {

    private val riskyVenuesApi = mockk<RiskyVenuesApi>()
    private val venueMatchFinder = mockk<VenueMatchFinder>(relaxUnitFun = true)
    private val visitedVenueStorage = mockk<VisitedVenuesStorage>(relaxUnitFun = true)
    private val filterOutdatedVisits = mockk<FilterOutdatedVisits>()
    private val riskyVenuesCircuitBreakerPolling = mockk<RiskyVenuesCircuitBreakerPolling>(relaxUnitFun = true)
    private val riskyVenueCircuitBreakerConfigurationProvider =
        mockk<RiskyVenueCircuitBreakerConfigurationProvider>(relaxUnitFun = true)

    private val testSubject = DownloadAndProcessRiskyVenues(
        riskyVenuesApi,
        venueMatchFinder,
        visitedVenueStorage,
        filterOutdatedVisits,
        riskyVenuesCircuitBreakerPolling,
        riskyVenueCircuitBreakerConfigurationProvider
    )

    private val riskyVenues = listOf(
        RiskyVenue(
            "1",
            RiskyWindow(
                from = Instant.parse("2020-07-08T10:00:00.00Z"),
                to = Instant.parse("2020-07-09T00:00:00.00Z")
            ),
            messageType = INFORM
        )
    )

    private val venueVisits = listOf(
        VenueVisit(
            venue = Venue(
                "1",
                "Venue1"
            ),
            from = Instant.parse("2020-07-08T10:00:00.00Z"),
            to = Instant.parse("2020-07-09T00:00:00.00Z"),
            wasInRiskyList = false
        )
    )

    private val riskyVenueMatches = mapOf(riskyVenues[0] to venueVisits)

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
    fun `does not call findMatches if risky venues are empty`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(venues = listOf())

        val result = testSubject()

        assertEquals(Result.Success(Unit), result)

        coVerify(exactly = 0) { venueMatchFinder.findMatches(any()) }
    }

    @Test
    fun `when no matches found circuit breaker is not called`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(riskyVenues)
        coEvery { venueMatchFinder.findMatches(any()) } returns emptyMap()

        val result = testSubject()

        assertEquals(Result.Success(Unit), result)

        coVerify(exactly = 0) { visitedVenueStorage.markAsWasInRiskyList(emptyList()) }
        coVerify(exactly = 0) { riskyVenueCircuitBreakerConfigurationProvider.addAll(any()) }
    }

    @Test
    fun `when matches found circuit breaker initialization is scheduled`() = runBlocking {
        coEvery { riskyVenuesApi.getListOfRiskyVenues() } returns RiskyVenuesResponse(riskyVenues)
        coEvery { venueMatchFinder.findMatches(riskyVenues) } returns riskyVenueMatches

        val result = testSubject()

        assertEquals(Result.Success(Unit), result)

        coVerify(exactly = 1) { visitedVenueStorage.markAsWasInRiskyList(venueVisits) }
        val slot = slot<List<RiskyVenueCircuitBreakerConfiguration>>()
        coVerify(exactly = 1) { riskyVenueCircuitBreakerConfigurationProvider.addAll(capture(slot)) }

        val expected = RiskyVenueCircuitBreakerConfiguration(
            startedAt = Instant.parse("2020-07-08T23:59:59.999Z"),
            venueId = riskyVenues[0].id,
            approvalToken = null,
            isPolling = false,
            messageType = INFORM
        )

        assertEquals(1, slot.captured.size)
        assertEquals(expected, slot.captured[0])
    }
}
