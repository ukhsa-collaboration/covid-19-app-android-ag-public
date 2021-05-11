package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.StateStorageTest.Operation.READ
import uk.nhs.nhsx.covid19.android.app.state.StateStorageTest.Operation.WRITE
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class StateStorageTest(private val testParameters: StateRepresentationTest) {

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val statusStringStorage = mockk<StateStringStorage>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()

    private val testSubject =
        StateStorage(
            statusStringStorage,
            isolationConfigurationProvider,
            moshi
        )

    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun test() {
        when (testParameters.operation) {
            READ -> read(testParameters.stateRepresentation)
            WRITE -> write(testParameters.stateRepresentation)
        }
    }

    private fun read(stateRepresentation: StateRepresentation) {
        every { statusStringStorage.prefsValue } returns stateRepresentation.json

        val parsedState = testSubject.state

        assertEquals(stateRepresentation.state, parsedState)
    }

    private fun write(stateRepresentation: StateRepresentation) {
        testSubject.state = stateRepresentation.state

        verify { statusStringStorage setProperty "prefsValue" value stateRepresentation.json }
    }

    data class StateRepresentation(
        val name: String,
        val json: String?,
        val state: IsolationState
    )

    enum class Operation {
        READ,
        WRITE
    }

    data class StateRepresentationTest(
        val stateRepresentation: StateRepresentation,
        val operation: Operation
    ) {
        override fun toString(): String {
            return """$operation: ${stateRepresentation.name}"""
        }
    }

    companion object {
        private const val CONFIGURATION =
            """{"contactCase":11,"indexCaseSinceSelfDiagnosisOnset":11,"indexCaseSinceSelfDiagnosisUnknownOnset":9,"maxIsolation":21,"pendingTasksRetentionPeriod":14,"indexCaseSinceTestResultEndDate":11}"""

        private const val CONTACT_EXPOSURE_DATE = "2020-01-09"
        private const val CONTACT_NOTIFICATION_DATE = "2020-01-08"
        private const val CONTACT_DCT_OPT_IN_DATE = "2020-01-07"
        private const val CONTACT_EXPIRY_DATE = "2020-01-06"
        private const val CONTACT_WITH_DCT =
            """{"exposureDate":"$CONTACT_EXPOSURE_DATE","notificationDate":"$CONTACT_NOTIFICATION_DATE","dailyContactTestingOptInDate":"$CONTACT_DCT_OPT_IN_DATE","expiryDate":"$CONTACT_EXPIRY_DATE"}"""
        private const val CONTACT_WITHOUT_DCT =
            """{"exposureDate":"$CONTACT_EXPOSURE_DATE","notificationDate":"$CONTACT_NOTIFICATION_DATE","expiryDate":"$CONTACT_EXPIRY_DATE"}"""

        private const val TEST_END_DATE = "2020-01-05"
        private const val TEST_ACKNOWLEDGED_DATE = "2020-01-04"
        private const val TEST_CONFIRMED_DATE = "2020-01-03"
        private const val POSITIVE_TEST_RESULT =
            """{"testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$TEST_ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$TEST_CONFIRMED_DATE"}"""
        private const val NEGATIVE_TEST_RESULT =
            """{"testEndDate":"$TEST_END_DATE","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$TEST_ACKNOWLEDGED_DATE","requiresConfirmatoryTest":false}"""

        private val positiveTestResult = AcknowledgedTestResult(
            testEndDate = LocalDate.parse(TEST_END_DATE),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = LocalDate.parse(TEST_ACKNOWLEDGED_DATE),
            requiresConfirmatoryTest = true,
            confirmedDate = LocalDate.parse(TEST_CONFIRMED_DATE)
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
                    indexInfo = NegativeTest(negativeTestResult)
                )
            ),
            StateRepresentation(
                name = "isolating with contact case with DCT",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_DCT,"hasAcknowledgedEndOfIsolation":true,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        dailyContactTestingOptInDate = LocalDate.parse(CONTACT_DCT_OPT_IN_DATE),
                        expiryDate = LocalDate.parse(CONTACT_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = true
                )
            ),
            StateRepresentation(
                name = "isolating with contact case without DCT",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITHOUT_DCT,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        expiryDate = LocalDate.parse(CONTACT_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment with onset date",
                json =
                    """{"configuration":$CONFIGURATION,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = IndexCase(
                        isolationTrigger = SelfAssessment(
                            selfAssessmentDate = LocalDate.parse(SYMPTOMATIC_SELF_DIAGNOSIS_DATE),
                            onsetDate = LocalDate.parse(SYMPTOMATIC_ONSET_DATE)
                        ),
                        expiryDate = LocalDate.parse(INDEX_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment without onset date",
                json =
                    """{"configuration":$CONFIGURATION,"symptomatic":$SYMPTOMATIC_WITHOUT_ONSET,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = IndexCase(
                        isolationTrigger = SelfAssessment(
                            selfAssessmentDate = LocalDate.parse(SYMPTOMATIC_SELF_DIAGNOSIS_DATE)
                        ),
                        expiryDate = LocalDate.parse(INDEX_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment and positive test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = IndexCase(
                        isolationTrigger = SelfAssessment(
                            selfAssessmentDate = LocalDate.parse(
                                SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                            ),
                            onsetDate = LocalDate.parse(SYMPTOMATIC_ONSET_DATE)
                        ),
                        testResult = positiveTestResult,
                        expiryDate = LocalDate.parse(INDEX_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with self-assessment and negative test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$NEGATIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = IndexCase(
                        isolationTrigger = SelfAssessment(
                            selfAssessmentDate = LocalDate.parse(
                                SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                            ),
                            onsetDate = LocalDate.parse(
                                SYMPTOMATIC_ONSET_DATE
                            )
                        ),
                        testResult = negativeTestResult,
                        expiryDate = LocalDate.parse(INDEX_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating with positive test",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = IndexCase(
                        isolationTrigger = PositiveTestResult(
                            positiveTestResult.testEndDate
                        ),
                        testResult = positiveTestResult,
                        expiryDate = LocalDate.parse(
                            INDEX_EXPIRY_DATE
                        )
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            StateRepresentation(
                name = "isolating for all reasons",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_DCT,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"indexExpiryDate":"$INDEX_EXPIRY_DATE","hasAcknowledgedEndOfIsolation":true,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        exposureDate = LocalDate.parse(
                            CONTACT_EXPOSURE_DATE
                        ),
                        notificationDate = LocalDate.parse(
                            CONTACT_NOTIFICATION_DATE
                        ),
                        dailyContactTestingOptInDate = LocalDate.parse(
                            CONTACT_DCT_OPT_IN_DATE
                        ),
                        expiryDate = LocalDate.parse(
                            CONTACT_EXPIRY_DATE
                        )
                    ),
                    indexInfo = IndexCase(
                        isolationTrigger = SelfAssessment(
                            selfAssessmentDate = LocalDate.parse(
                                SYMPTOMATIC_SELF_DIAGNOSIS_DATE
                            ),
                            onsetDate = LocalDate.parse(
                                SYMPTOMATIC_ONSET_DATE
                            )
                        ),
                        testResult = positiveTestResult,
                        expiryDate = LocalDate.parse(
                            INDEX_EXPIRY_DATE
                        )
                    ),
                    hasAcknowledgedEndOfIsolation = true
                )
            )
        )

        // State representations that can only be read. Use these to test error handling and migration, to verify that invalid
        // or outdated states are handled properly
        private val readOnlyStateRepresentations = listOf(
            // We cannot handle this => discard symptoms
            StateRepresentation(
                name = "isolating with self-assessment but index expiry date missing",
                json =
                    """{"configuration":$CONFIGURATION,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            // We cannot handle this => discard symptoms and test
            StateRepresentation(
                name = "isolating with self-assessment and positive test but index expiry date missing",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            // We cannot handle this => discard symptoms, keep test
            StateRepresentation(
                name = "isolating with self-assessment and negative test but index expiry date missing",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$NEGATIVE_TEST_RESULT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = NegativeTest(negativeTestResult),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            // We cannot handle this => discard symptoms
            StateRepresentation(
                name = "isolating with contact case and self-assessment but index expiry date missing",
                json =
                    """{"configuration":$CONFIGURATION,"contact":$CONTACT_WITH_DCT,"symptomatic":$SYMPTOMATIC_WITH_ONSET,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        exposureDate = LocalDate.parse(CONTACT_EXPOSURE_DATE),
                        notificationDate = LocalDate.parse(CONTACT_NOTIFICATION_DATE),
                        dailyContactTestingOptInDate = LocalDate.parse(CONTACT_DCT_OPT_IN_DATE),
                        expiryDate = LocalDate.parse(CONTACT_EXPIRY_DATE)
                    ),
                    hasAcknowledgedEndOfIsolation = false
                )
            ),
            // We cannot handle this => discard test
            StateRepresentation(
                name = "isolating with positive test but index expiry date missing",
                json =
                    """{"configuration":$CONFIGURATION,"testResult":$POSITIVE_TEST_RESULT,"hasAcknowledgedEndOfIsolation":false,"version":1}""",
                state = IsolationState(
                    isolationConfiguration = DurationDays(),
                    hasAcknowledgedEndOfIsolation = false
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

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun generateParameters(): Iterable<StateRepresentationTest> =
            readWriteRepresentations.flatMap { stateRepresentation ->
                listOf(
                    StateRepresentationTest(stateRepresentation, READ),
                    StateRepresentationTest(stateRepresentation, WRITE)
                )
            } + readOnlyStateRepresentations.map { stateRepresentation ->
                StateRepresentationTest(stateRepresentation, READ)
            }
    }
}
