package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RiskyPostCodeDistributionResponse(val postDistricts: Map<String, String>, val localAuthorities: Map<String, String>?, val riskLevels: Map<String, RiskIndicator>)
