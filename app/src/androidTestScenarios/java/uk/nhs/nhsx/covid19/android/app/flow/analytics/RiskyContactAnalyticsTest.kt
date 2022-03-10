package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class RiskyContactAnalyticsTest : AnalyticsTest(), IsolationSetupHelper {
    override val isolationHelper: IsolationHelper = IsolationHelper(testAppContext.clock)

    private val riskyContact = RiskyContact(this)

    private fun assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
        acknowledgement: () -> Unit,
        hasOptedOutOfContactIsolation: Boolean = false
    ) {
        triggerRiskyContactAndAcknowledge(acknowledgement)

        assertOnFields(implicitlyAssertNotPresent = false) {
            assertEquals(1, Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
            if (hasOptedOutOfContactIsolation) {
                assertEquals(1, Metrics::optedOutForContactIsolation)
                assertPresent(Metrics::optedOutForContactIsolationBackgroundTick)
            }
        }

        if (hasOptedOutOfContactIsolation) {
            while (remembersIsolation()) {
                assertOnFields(implicitlyAssertNotPresent = false) {
                    assertPresent(Metrics::optedOutForContactIsolationBackgroundTick)
                }
            }

            assertAnalyticsPacketIsNormal()
        }
    }

    private fun triggerRiskyContactAndAcknowledge(acknowledgement: () -> Unit) {
        startTestActivity<MainActivity>()

        riskyContact.triggerViaCircuitBreaker(this::advanceToNextBackgroundTaskExecution)
        acknowledgement()
    }

    private fun remembersIsolation() = testAppContext.getIsolationStateMachine().readLogicalState() is PossiblyIsolating

    @Test
    fun notIsolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation() {
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = riskyContact::acknowledgeIsolationViaOptOutMinor,
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun notIsolating_riskyContact_declareFullyVaccinated_isolationAcknowledged_andOptedOutOfContactIsolation() {
        givenLocalAuthorityIsInWales()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = riskyContact::acknowledgeIsolationViaOptOutFullyVaccinatedForContactQuestionnaireJourney,
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun notIsolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forEngland() {
        givenLocalAuthorityIsInEngland()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = riskyContact::acknowledgeNoIsolationForNewAdviceJourney,
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun isolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forEngland() {
        givenLocalAuthorityIsInEngland()
        givenSelfAssessmentIsolation()

        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = riskyContact::acknowledgeContinueIsolationForNewAdviceJourney,
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun notIsolating_riskyContact_declareNotFullyVaccinated_isolationAcknowledged() {
        givenLocalAuthorityIsInWales()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(acknowledgement = {
            riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(country = WALES)
        })
    }

    @Test
    fun isolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation() {
        givenSelfAssessmentIsolation()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = {
                riskyContact.acknowledgeIsolationViaOptOutMinor(alreadyIsolating = true)
            },
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun isolating_riskyContact_declareFullyVaccinated_isolationAcknowledged_andOptedOutOfContactIsolation() {
        givenLocalAuthorityIsInWales()
        givenSelfAssessmentIsolation()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = {
                riskyContact.acknowledgeIsolationViaOptOutFullyVaccinatedForContactQuestionnaireJourney(alreadyIsolating = true)
            },
            hasOptedOutOfContactIsolation = true
        )
    }

    @Test
    fun isolating_riskyContact_declareNotFullyVaccinated_isolationAcknowledged() {
        givenLocalAuthorityIsInWales()
        givenSelfAssessmentIsolation()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContact(
            acknowledgement = {
                riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(alreadyIsolating = true, country = WALES)
            }
        )
    }
}
