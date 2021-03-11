package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateAnalyticsPayload @Inject constructor(
    private val calculateMissingSubmissionDays: CalculateMissingSubmissionDays,
    private val metadataProvider: MetadataProvider,
    private val updateStatusStorage: UpdateStatusStorage
) {

    operator fun invoke(group: AnalyticsEventsGroup): AnalyticsPayload {
        val missingSubmissionDays = calculateMissingSubmissionDays(group.analyticsWindow)
        val metrics = group.entries.toMetrics(missingSubmissionDays)

        return AnalyticsPayload(
            analyticsWindow = group.analyticsWindow,
            metrics = metrics,
            metadata = metadataProvider.getMetadata(),
            includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
        )
    }
}
