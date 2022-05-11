package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class RiskyContactAnalyticsTest : AnalyticsTest(), IsolationSetupHelper {
    override val isolationHelper: IsolationHelper = IsolationHelper(testAppContext.clock)

    private val riskyContact = RiskyContact(this)

    private fun assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
        acknowledgement: () -> Unit,
        hasOptedOutOfContactIsolation: Boolean = false,
        usesOldContactCaseFlow: Boolean = true
    ) {
        triggerRiskyContactAndAcknowledge(acknowledgement)

        assertOnFields(implicitlyAssertNotPresent = false) {
            if (usesOldContactCaseFlow)
                assertEquals(1, Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
            else
                assertNull(Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
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
    fun notIsolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation_Wales() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = riskyContact::acknowledgeIsolationViaOptOutMinor,
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun notIsolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation_England() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInEngland()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = riskyContact::acknowledgeIsolationViaOptOutMinor,
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun notIsolating_riskyContact_declareFullyVaccinated_isolationAcknowledged_andOptedOutOfContactIsolation() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = riskyContact::acknowledgeIsolationViaOptOutFullyVaccinatedForContactQuestionnaireJourney,
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun notIsolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forEngland() {
        givenLocalAuthorityIsInEngland()
        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
            acknowledgement = riskyContact::acknowledgeNoIsolationForNewAdviceJourney,
            hasOptedOutOfContactIsolation = true,
            usesOldContactCaseFlow = false
        )
    }

    @Test
    fun isolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forEngland() {
        givenLocalAuthorityIsInEngland()
        givenSelfAssessmentIsolation()

        assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
            acknowledgement = riskyContact::acknowledgeContinueIsolationForNewAdviceJourney,
            hasOptedOutOfContactIsolation = true,
            usesOldContactCaseFlow = false
        )
    }

    @Test
    fun notIsolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forWales() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = riskyContact::acknowledgeNoIsolationForNewAdviceJourney,
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = false
            )
        }
    }

    @Test
    fun isolating_riskyContact_newAdvice_andOptedOutOfContactIsolation_forWales() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()

            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = riskyContact::acknowledgeContinueIsolationForNewAdviceJourney,
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = false
            )
        }
    }

    @Test
    fun notIsolating_riskyContact_declareNotFullyVaccinated_isolationAcknowledged() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(acknowledgement = {
                riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(country = WALES)
            })
        }
    }

    @Test
    fun isolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation_Wales() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = {
                    riskyContact.acknowledgeIsolationViaOptOutMinor(alreadyIsolating = true)
                },
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun isolating_riskyContact_declareMinor_isolationAcknowledged_andOptedOutOfContactIsolation_England() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInEngland()
            givenSelfAssessmentIsolation()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = {
                    riskyContact.acknowledgeIsolationViaOptOutMinor(alreadyIsolating = true)
                },
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun isolating_riskyContact_declareFullyVaccinated_isolationAcknowledged_andOptedOutOfContactIsolation() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = {
                    riskyContact.acknowledgeIsolationViaOptOutFullyVaccinatedForContactQuestionnaireJourney(
                        alreadyIsolating = true
                    )
                },
                hasOptedOutOfContactIsolation = true,
                usesOldContactCaseFlow = true
            )
        }
    }

    @Test
    fun isolating_riskyContact_declareNotFullyVaccinated_isolationAcknowledged() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()
            assertAcknowledgingRiskyContactIncrementsAcknowledgedStartOfIsolationDueToRiskyContactWhenOldContactCaseFlow(
                acknowledgement = {
                    riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(
                        alreadyIsolating = true,
                        country = WALES
                    )
                }
            )
        }
    }
}
