package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateStorage @Inject constructor(
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private var storedState: IsolationStateJson? by storage(ISOLATION_STATE_KEY)

    var state: IsolationState
        get() = storedState?.toMigratedState() ?: IsolationState(isolationConfigurationProvider.durationDays)
        set(newState) {
            storedState = newState.toStateJson()
        }

    companion object {
        const val ISOLATION_STATE_KEY = "ISOLATION_STATE_KEY"
    }
}

private const val LATEST_ISOLATION_STATE_JSON_VERSION = 1

@JsonClass(generateAdapter = true)
data class IsolationStateJson(
    val configuration: DurationDays,
    val contact: ContactCase? = null,
    val testResult: AcknowledgedTestResult? = null,
    val symptomatic: SymptomaticCase? = null,
    val indexExpiryDate: LocalDate? = null, // TODO@splitIndexCase: remove
    val hasAcknowledgedEndOfIsolation: Boolean = false,
    val version: Int = LATEST_ISOLATION_STATE_JSON_VERSION
)

@JsonClass(generateAdapter = true)
data class SymptomaticCase(
    val selfDiagnosisDate: LocalDate,
    val onsetDate: LocalDate? = null
)

private fun IsolationState.toStateJson(): IsolationStateJson =
    IsolationStateJson(
        configuration = isolationConfiguration,
        contact = contactCase,
        testResult = indexInfo?.testResult,
        symptomatic = indexInfo?.toSymptomaticCase(),
        indexExpiryDate = (indexInfo as? IndexCase)?.expiryDate,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

private fun IndexInfo.toSymptomaticCase(): SymptomaticCase? =
    if (this is IndexCase && isolationTrigger is SelfAssessment)
        SymptomaticCase(
            selfDiagnosisDate = isolationTrigger.selfAssessmentDate,
            onsetDate = isolationTrigger.onsetDate
        )
    else null

private fun IsolationStateJson.toMigratedState(): IsolationState {
    val migratedTestResult = if (testResult?.confirmatoryTestCompletionStatus == null && testResult?.confirmedDate != null) {
        testResult.copy(confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED)
    } else {
        testResult
    }
    return isolationState(migratedTestResult)
}

private fun IsolationStateJson.isolationState(
    testResult: AcknowledgedTestResult?
): IsolationState {
    val indexIsolationTrigger =
        if (symptomatic != null)
            SelfAssessment(
                selfAssessmentDate = symptomatic.selfDiagnosisDate,
                onsetDate = symptomatic.onsetDate
            )
        else if (testResult != null && testResult.testResult == POSITIVE)
            PositiveTestResult(testEndDate = testResult.testEndDate)
        else null

    val indexInfo =
        if (indexIsolationTrigger != null && indexExpiryDate != null /*TODO@splitIndexCase: remove indexExpiryDate != null*/)
            IndexCase(
                isolationTrigger = indexIsolationTrigger,
                testResult = testResult,
                expiryDate = indexExpiryDate
            )
        else if (testResult?.testResult == NEGATIVE)
            NegativeTest(testResult)
        else null

    return IsolationState(
        isolationConfiguration = configuration,
        indexInfo = indexInfo,
        contactCase = contact,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )
}
