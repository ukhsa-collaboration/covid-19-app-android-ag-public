package uk.nhs.nhsx.covid19.android.app.localstats

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthorityData
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Parcelize
data class LocalStats(
    val postCodeDistrict: PostCodeDistrict,
    val localAuthorityName: String,
    val lastFetch: Instant,
    val countryNewCasesBySpecimenDateRollingRateLastUpdate: LocalDate,
    val localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate: LocalDate,
    val localAuthorityNewCasesByPublishDateLastUpdate: LocalDate,
    val countryNewCasesBySpecimenDateRollingRate: BigDecimal?,
    val localAuthorityData: LocalAuthorityData
) : Parcelable
