package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
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
    val contact: Contact? = null,
    val testResult: AcknowledgedTestResult? = null,
    val symptomatic: SymptomaticCase? = null,
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
        contact = contact,
        testResult = testResult,
        symptomatic = selfAssessment?.toSymptomaticCase(),
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )

private fun SelfAssessment.toSymptomaticCase(): SymptomaticCase =
    SymptomaticCase(
        selfDiagnosisDate = selfAssessmentDate,
        onsetDate = onsetDate
    )

private fun SymptomaticCase.toSelfAssessment(): SelfAssessment =
    SelfAssessment(
        selfAssessmentDate = selfDiagnosisDate,
        onsetDate = onsetDate
    )

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
): IsolationState =
    IsolationState(
        isolationConfiguration = configuration,
        selfAssessment = symptomatic?.toSelfAssessment(),
        testResult = testResult,
        contact = contact,
        hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation
    )
