package uk.nhs.nhsx.covid19.android.app.status

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowInAppReviewTest {

    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val lastAppRatingStartedDateProvider =
        mockk<LastAppRatingStartedDateProvider>(relaxed = true)

    private val testSubject =
        ShouldShowInAppReview(visitedVenuesStorage, lastAppRatingStartedDateProvider)

    private val venue = Venue("test", "test")

    private val visitsOnTwoDays = listOf(
        VenueVisit(
            venue = venue,
            from = Instant.parse("2020-09-09T10:00:00Z"),
            to = Instant.parse("2020-09-09T12:00:00Z")
        ),
        VenueVisit(
            venue = venue,
            from = Instant.parse("2020-09-10T10:00:00Z"),
            to = Instant.parse("2020-09-10T12:00:00Z")
        )
    )

    @Before
    fun setUp() {
        coEvery { visitedVenuesStorage.getVisits() } returns visitsOnTwoDays
        every { lastAppRatingStartedDateProvider.value } returns null
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `no venue visits returns false`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf()

        assertFalse(testSubject())
    }

    @Test
    fun `multiple visits on two days returns true`() = runBlocking {
        assertTrue(testSubject())
    }

    @Test
    fun `multiple venue visits on same day returns false`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(
            VenueVisit(
                venue = venue,
                from = Instant.parse("2020-09-09T10:00:00Z"),
                to = Instant.parse("2020-09-09T12:00:00Z")
            ),
            VenueVisit(
                venue = venue,
                from = Instant.parse("2020-09-09T13:00:00Z"),
                to = Instant.parse("2020-09-09T14:00:00Z")
            ),
            VenueVisit(
                venue = venue,
                from = Instant.parse("2020-09-09T23:59:59Z"),
                to = Instant.parse("2020-09-10T03:30:00Z")
            )
        )

        assertFalse(testSubject())
    }

    @Test
    fun `last app rating started already set returns false`() = runBlocking {
        every { lastAppRatingStartedDateProvider.value } returns Instant.parse("2020-09-09T20:00:00Z")
            .toEpochMilli()

        assertFalse(testSubject())
    }
}
