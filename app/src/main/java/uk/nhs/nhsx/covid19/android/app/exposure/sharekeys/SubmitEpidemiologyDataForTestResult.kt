package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import javax.inject.Inject

class SubmitEpidemiologyDataForTestResult @Inject constructor(
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val submitEpidemiologyData: SubmitEpidemiologyData
) {

    operator fun invoke(testKitType: VirologyTestKitType?, requiresConfirmatoryTest: Boolean) {
        submitEpidemiologyData.submitAfterPositiveTest(
            epidemiologyEventProvider.epidemiologyEvents,
            testKitType,
            requiresConfirmatoryTest
        )
    }
}
