package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.getNextLocalMidnightTime
import uk.nhs.nhsx.covid19.android.app.util.roundDownToNearestQuarter
import uk.nhs.nhsx.covid19.android.app.util.roundUpToNearestQuarter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.Clock
import java.time.Instant
import java.util.concurrent.Executors
import javax.inject.Inject

class VisitedVenuesStorage @Inject constructor(context: Context, moshi: Moshi) {

    private val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val venuesFile = File(context.filesDir, "venues")
    private val encryptedFile = EncryptedFile.Builder(
        venuesFile,
        context,
        masterKeyAlias,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()

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

        val writer = OutputStreamWriter(encryptedFile.openFileOutput(), Charsets.UTF_8)
        writer.use { it.write(updatedVisitedVenues) }
    }

    fun removeAllVenueVisits() {
        venuesFile.delete()
    }

    suspend fun markAsWasInRiskyList(venueIds: List<String>) = withContext(context) {
        val visits = getVisitedVenuesMutable()
        setVisits(visits.map { it.copy(wasInRiskyList = it.venue.id in venueIds) })
    }

    private fun getVisitedVenuesMutable(): MutableList<VenueVisit> {
        return try {
            val inputStream = encryptedFile.openFileInput()
            val fileContents = inputStream.bufferedReader().use { it.readText() }
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
