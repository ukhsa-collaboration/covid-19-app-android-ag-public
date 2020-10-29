package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.EncryptedFileInfo
import uk.nhs.nhsx.covid19.android.app.util.getNextLocalMidnightTime
import uk.nhs.nhsx.covid19.android.app.util.readText
import uk.nhs.nhsx.covid19.android.app.util.roundDownToNearestQuarter
import uk.nhs.nhsx.covid19.android.app.util.roundUpToNearestQuarter
import uk.nhs.nhsx.covid19.android.app.util.writeText
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.concurrent.Executors
import javax.inject.Inject

class VisitedVenuesStorage @Inject constructor(
    moshi: Moshi,
    encryptedFileInfo: EncryptedFileInfo
) {
    private val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val venuesFile = encryptedFileInfo.file
    private val encryptedFile = encryptedFileInfo.encryptedFile

    private val type = Types.newParameterizedType(
        List::class.java,
        VenueVisit::class.java
    )
    private val adapter: JsonAdapter<List<VenueVisit>> = moshi.adapter(type)

    suspend fun finishLastVisitAndAddNewVenue(venue: Venue, clock: Clock = Clock.systemDefaultZone()) =
        withContext(context) {
            val now = Instant.now(clock)
            val visitedVenues = getVisitedVenuesMutable()

            val lastVisit = visitedVenues.lastOrNull()
            lastVisit?.let {
                val nowForCheckOut = now.roundUpToNearestQuarter()
                if (nowForCheckOut.isBefore(lastVisit.to)) {
                    visitedVenues[visitedVenues.lastIndex] = lastVisit.copy(to = nowForCheckOut)
                }
            }

            val nowForCheckIn = now.roundDownToNearestQuarter()
            val visitedVenue = VenueVisit(
                venue,
                from = nowForCheckIn,
                to = nowForCheckIn.getNextLocalMidnightTime(clock)
            )
            visitedVenues.add(visitedVenue)

            setVisits(visitedVenues)
        }

    suspend fun getVisits(): List<VenueVisit> = withContext(context) {
        getVisitedVenuesMutable()
    }

    suspend fun getVisitByVenueId(venueId: String?): VenueVisit? {
        val visits = getVisits()

        return visits.firstOrNull { it.venue.id == venueId }
    }

    suspend fun setVisits(venueVisits: List<VenueVisit>) = withContext(context) {
        val updatedVisitedVenues = adapter.toJson(venueVisits)

        venuesFile.delete()

        encryptedFile.writeText(updatedVisitedVenues)
    }

    fun removeAllVenueVisits() {
        venuesFile.delete()
    }

    suspend fun removeVenueVisit(position: Int) {
        val visitedVenues = getVisitedVenuesMutable()

        visitedVenues.removeAt(position)

        setVisits(visitedVenues)
    }

    suspend fun markAsWasInRiskyList(venueIds: List<String>) = withContext(context) {
        val visits = getVisitedVenuesMutable()
        setVisits(visits.map { it.copy(wasInRiskyList = it.venue.id in venueIds) })
    }

    private fun getVisitedVenuesMutable(): MutableList<VenueVisit> {
        return try {
            val fileContents = encryptedFile.readText()
            adapter.fromJson(fileContents).orEmpty().toMutableList()
        } catch (exception: IOException) {
            mutableListOf()
        }
    }

    suspend fun removeLastVisit() = withContext(context) {
        val visitedVenues = getVisitedVenuesMutable()

        if (!visitedVenues.isNullOrEmpty()) {
            visitedVenues.removeAt(visitedVenues.size - 1)
            setVisits(visitedVenues)
        }
    }
}
