package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.StateStorage.Companion.ISOLATION_STATE_KEY
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.LocalDate

class StateStorageTest : ProviderTest<StateStorage, IsolationState>() {

    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()

    override val getTestSubject: (Moshi, SharedPreferences) -> StateStorage = { moshi, sharedPreferences -> StateStorage(isolationConfigurationProvider, moshi, sharedPreferences) }
    override val property = StateStorage::state
    override val key = ISOLATION_STATE_KEY
    override val defaultValue by lazy { IsolationState(isolationConfigurationProvider.durationDays) }
    override val expectations: List<ProviderTestExpectation<IsolationState>> = generateParameters()

    private val durationDays = DurationDays()

    @BeforeEach
    fun setUpMock() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    data class StateRepresentation(
        val name: String,
        val json: String?,
        val state: IsolationState
    )

    companion object {
        private const val CONFIGURATION =
            """{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11,"testResultPollingTokenRetentionPeriod":28}"""

        private const val CONTACT_EXPOSURE_DATE = "2020-01-09"
        private const val CONTACT_NOTIFICATION_DATE = "2020-01-08"
        private const val CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE = "2020-01-07"
        private const val CONTACT_EXPIRY_DATE = "2020-01-06"
        private const val CONTACT_WITH_OPT_OUT_OF_CONTACT_ISOLATION_AND_EXPIRY_DATE =
            """{"exposureDate":"$CONTACT_EXPOSURE_DATE","notificationDate":"$CONTACT_NOTIFICATION_DATE","optOutOfContactIsolation":{"date":"$CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE"},"expiryDate":"$CONTACT_EXPIRY_DATE"}"""
        private const val CONTACT_WITH_OPT_OUT_OF_CONTACT_ISOLATION =
            """{"exposureDate":"$CONTACT_EXPOSURE_DATE","notificationDate":"$CONTACT_NOTIFICATION_DATE","optOutOfContactIsolation":{"date":"$CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE","reason":"QUESTIONNAIRE"}}"""
        private const val CONTACT_WITHOUT_OPT_OUT_OF_CONTACT_ISOLATION =
            """{"exposureDate":"$CONTACT_EXPOSURE_DATE","notificationDate":"$CONTACT_NOTIFICATION_DATE"}"""

        private const val TEST_END_DATE = "2020-01-05"
        private const val TEST_ACKNOWLEDGED_DATE = "2020-01-04"
        private const val TEST_CONFIRMED_DATE = "2020-01-03"
        private const val POSITIVE_TEST_RESULT =
            """{"testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$TEST_ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$TEST_CONFIRMED_DATE","confirmatoryTestCompletionStatus":"COMPLETED_AND_CONFIRMED"}"""

        private const val POSITIVE_TEST_RESULT_V1 =
            """{"testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$TEST_ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$TEST_CONFIRMED_DATE"}"""

        private const val NEGATIVE_TEST_RESULT =
            """{"testEndDate":"$TEST_END_DATE","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$TEST_ACKNOWLEDGED_DATE","requiresConfirmatoryTest":false}"""

        private val positiveTestResult = AcknowledgedTestResult(
            testEndDate = LocalDate.parse(TEST_END_DATE),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = LocalDate.parse(TEST_ACKNOWLEDGED_DATE),
            requiresConfirmatoryTest = true,
            confirmedDate = LocalDate.parse(TEST_CONFIRMED_DATE),
            confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
        )
        private val negativeTestResult = AcknowledgedTestResult(
            testEndDate = LocalDate.parse(TEST_END_DATE),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = LocalDate.parse(TEST_ACKNOWLEDGED_DATE),
            requiresConfirmatoryTest = false
        )

        private const val SYMPTOMATIC_SELF_DIAGNOSIS_DATE = "2020-01-02"
        private const val SYMPTOMATIC_ONSET_DATE = "2020-01-01"
        private const val SYMPTOMATIC_WITH_ONSET =
            """{"selfDiagnosisDate":"$SYMPTOMATIC_SELF_DIAGNOSIS_DATE","onsetDate":"$SYMPTOMATIC_ONSET_DATE"}"""
        private const val SYMPTOMATIC_WITHOUT_ONSET =
            """{"selfDiagnosisDate":"$SYMPTOMATIC_SELF_DIAGNOSIS_DATE"}"""

        private const val INDEX_EXPIRY_DATE = "2020-01-01"

        // State representations that are fully bi-directional: we can read them and write them and they end up with the
        // same representation (no error or migration involved)
        private val readWriteRepresentations = listOf(
            StateRepresentation(
                name = "never isolating",
                json =
                    """{"configuration":$CONFIGURATION,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "never isolating and negative test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$NEGATIVE_TEST_RESULT,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    testResult = negativeTestResult
                )
            ),
            StateRepresentation(
                name = "isolating with contact case with contact isolation opt-out",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_OPT_OUT_OF_CONTACT_ISOLATION,"hasAcknowledgedEndOfIsolation":true,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contact = Contact(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.parse(CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE), reason = QUESTIONNAIRE),
                    ),
                    hasAcknowledgedEndOfIsolation = true
                )
            ),
            StateRepresentation(
                name = "isolating with contact case without contact isolation opt-out",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITHOUT_OPT_OUT_OF_CONTACT_ISOLATION,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contact = Contact(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment with onset date",
                json =
                    """{"configuration":$CONFIGURATION,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(SYMPTOMATIC_SELF_DIAGNOSIS_DATE),
                        onsetDate = LocalDate.parse(SYMPTOMATIC_ONSET_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment without onset date",
                json =
                    """{"configuration":$CONFIGURATION,"symptomatic":$SYMPTOMATIC_WITHOUT_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(SYMPTOMATIC_SELF_DIAGNOSIS_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment and positive test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(
                            SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                        ),
                        onsetDate = LocalDate.parse(SYMPTOMATIC_ONSET_DATE)
                    ),
                    testResult = positiveTestResult,
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment and negative test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$NEGATIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(
                            SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                        ),
                        onsetDate = LocalDate.parse(
                            SYMPTOMATIC_ONSET_DATE
                        )
                    ),
                    testResult = negativeTestResult,
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with positive test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    testResult = positiveTestResult,
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating for all reasons",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_OPT_OUT_OF_CONTACT_ISOLATION,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":true,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contact = Contact(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.parse(CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE)),
                    ),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(
                            SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                        ),
                        onsetDate = LocalDate.parse(
                            SYMPTOMATIC_ONSET_DATE
                        )
                    ),
                    testResult = positiveTestResult,
                    hasAcknowledgedEndOfIsolation = true
                )
            )
        )

        // State representations that can only be read. Use these to test error handling and migration, to verify that invalid
        // or outdated states are handled properly
        private val readOnlyStateRepresentations = listOf(
            // Copies completedDate from confirmedDate
            StateRepresentation(
                name = "isolating with positive test from v1 to v2",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT_V1,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    testResult = positiveTestResult,
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            // Discard expiry dates
            StateRepresentation(
                name = "isolating for all reasons with expiry dates",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_OPT_OUT_OF_CONTACT_ISOLATION,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":true,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contact = Contact(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        optOutOfContactIsolation = OptOutOfContactIsolation(LocalDate.parse(CONTACT_OPT_OUT_OF_CONTACT_ISOLATION_DATE))
                    ),
                    selfAssessment = SelfAssessment(
                        selfAssessmentDate = LocalDate.parse(
                            SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                        ),
                        onsetDate = LocalDate.parse(
                            SYMPTOMATIC_ONSET_DATE
                        )
                    ),
                    testResult = positiveTestResult,
                    hasAcknowledgedEndOfIsolation = true
                )
            ),
            StateRepresentation(
                name = "null as never isolating",
                json = null,
                state = IsolationState(
                    isolationConfiguration = DurationDays()
                )
            ),
            StateRepresentation(
                name = "invalid data as never isolating",
                json =
                    """{"type":"UnknownCase","testDate":1594733801229,"expiryDate":1595338601229,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays()
                )
            )
        )

        fun generateParameters(): List<ProviderTestExpectation<IsolationState>> =
            readWriteRepresentations.map { stateRepresentation ->
                ProviderTestExpectation(json = stateRepresentation.json, objectValue = stateRepresentation.state)
            } + readOnlyStateRepresentations.map { stateRepresentation ->
                ProviderTestExpectation(json = stateRepresentation.json, objectValue = stateRepresentation.state, direction = JSON_TO_OBJECT)
            }
    }
}
