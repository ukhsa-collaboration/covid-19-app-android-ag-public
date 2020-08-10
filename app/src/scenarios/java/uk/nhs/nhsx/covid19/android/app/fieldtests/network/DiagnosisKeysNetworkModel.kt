package uk.nhs.nhsx.covid19.android.app.fieldtests.network

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.exposure.FieldTestConfiguration

@JsonClass(generateAdapter = true)
data class ExperimentInfo(
    val iosAppVersion: String = "",
    val androidAppVersion: String = "",
    val lead: DeviceInfo? = null,
    val participants: List<DeviceInfo> = listOf(),
    val experimentId: String = "",
    val experimentName: String = "",
    val automaticDetectionFrequency: Int = 0,
    val requestedConfigurations: List<FieldTestConfiguration> = listOf()
)

@JsonClass(generateAdapter = true)
data class DeviceInfo(
    val deviceName: String,
    val googlePlayServicesVersion: String = "",
    val temporaryTracingKeys: List<TemporaryTracingKey>,
    val results: List<Any>?
)

@JsonClass(generateAdapter = true)
data class Results(
    val timestamp: String
)
@JsonClass(generateAdapter = true)
data class TemporaryTracingKey(
    val key: String,
    val intervalNumber: Int,
    val intervalCount: Int
)

@JsonClass(generateAdapter = true)
data class MatchingResult(
    val timestamp: String,
    val configuration: FieldTestConfiguration,
    val counterparts: List<ExposureEvent>
)

@JsonClass(generateAdapter = true)
data class ExposureEvent(
    val deviceName: String,
    val summary: Summary,
    val exposureInfos: List<Exposure>
)

@JsonClass(generateAdapter = true)
data class Summary(
    val attenuationDurations: List<Int>,
    val daysSinceLastExposure: Int,
    val matchedKeyCount: Int,
    val maximumRiskScore: Int,
    val summationRiskScore: Int
)

@JsonClass(generateAdapter = true)
data class Exposure(
    val attenuationDurations: List<Int>,
    val attenuationValue: Int,
    val date: String,
    val duration: Int,
    val totalRiskScore: Int,
    val transmissionRiskLevel: Int
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(val error: ActualError)

@JsonClass(generateAdapter = true)
data class ActualError(val message: String)
