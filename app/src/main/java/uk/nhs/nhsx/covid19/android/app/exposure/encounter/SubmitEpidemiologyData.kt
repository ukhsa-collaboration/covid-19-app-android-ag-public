package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.VisibleForTesting
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventWithType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows

class SubmitEpidemiologyData constructor(
    private val metadataProvider: MetadataProvider,
    private val epidemiologyDataApi: EpidemiologyDataApi,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val submitEpidemiologyDataScope: CoroutineScope,
    private val submitEpidemiologyDataDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(
        metadataProvider: MetadataProvider,
        epidemiologyDataApi: EpidemiologyDataApi,
        submitFakeExposureWindows: SubmitFakeExposureWindows
    ) : this(
        metadataProvider, epidemiologyDataApi, submitFakeExposureWindows, GlobalScope, Dispatchers.IO
    )

    operator fun invoke(
        epidemiologyEventList: List<EpidemiologyEvent>,
        epidemiologyEventType: EpidemiologyEventType
    ) {
        submitEpidemiologyDataScope.launch(submitEpidemiologyDataDispatcher) {
            epidemiologyEventList.forEach { epidemiologyEvent ->
                runSafely {
                    epidemiologyDataApi.submitEpidemiologyData(
                        EpidemiologyRequest(
                            metadata = metadataProvider.getMetadata(),
                            events = listOf(epidemiologyEvent.toEpidemiologyEventWithType(epidemiologyEventType))
                        )
                    )
                }
            }
            submitFakeExposureWindows(epidemiologyEventType.toEmptySubmissionSource(), epidemiologyEventList.size)
        }
    }

    data class ExposureWindowWithRisk(val dayRisk: DayRisk, val exposureWindow: ExposureWindow)
}

@VisibleForTesting
internal fun EpidemiologyEvent.toEpidemiologyEventWithType(type: EpidemiologyEventType): EpidemiologyEventWithType =
    EpidemiologyEventWithType(
        type = type,
        version = this.version,
        payload = this.payload
    )

private fun EpidemiologyEventType.toEmptySubmissionSource(): EmptySubmissionSource =
    when (this) {
        EXPOSURE_WINDOW_POSITIVE_TEST -> EXPOSURE_WINDOW_AFTER_POSITIVE
        EXPOSURE_WINDOW -> EmptySubmissionSource.EXPOSURE_WINDOW
    }
