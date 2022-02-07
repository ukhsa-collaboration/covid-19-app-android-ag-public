package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

typealias LocalAuthorityId = String

@JsonClass(generateAdapter = true)
data class LocalStatsResponse(
    val lastFetch: Instant,
    val metadata: LocalStatsMetadata,
    val england: CountryData,
    val wales: CountryData,
    @Json(name = "lowerTierLocalAuthorities")
    val localAuthorities: Map<LocalAuthorityId, LocalAuthorityData>
)

@JsonClass(generateAdapter = true)
data class LocalStatsMetadata(
    val england: CountryMetadata,
    val wales: CountryMetadata,
    @Json(name = "lowerTierLocalAuthorities")
    val localAuthorities: LocalAuthoritiesMetadata
)

@JsonClass(generateAdapter = true)
data class CountryData(
    val newCasesBySpecimenDateRollingRate: BigDecimal?
)

@JsonClass(generateAdapter = true)
data class CountryMetadata(
    val newCasesBySpecimenDateRollingRate: LastUpdateDate
)

@JsonClass(generateAdapter = true)
data class LocalAuthoritiesMetadata(
    val newCasesByPublishDate: LastUpdateDate,
    val newCasesBySpecimenDateRollingRate: LastUpdateDate
)

@JsonClass(generateAdapter = true)
@Parcelize
data class LocalAuthorityData(
    val newCasesByPublishDate: Int?,
    val newCasesByPublishDateRollingSum: Int?,
    val newCasesByPublishDateDirection: Direction?,
    val newCasesByPublishDateChange: Int?,
    val newCasesByPublishDateChangePercentage: BigDecimal?,
    val newCasesBySpecimenDateRollingRate: BigDecimal?
) : Parcelable

enum class Direction {
    SAME, UP, DOWN
}

@JsonClass(generateAdapter = true)
data class LastUpdateDate(val lastUpdate: LocalDate)
