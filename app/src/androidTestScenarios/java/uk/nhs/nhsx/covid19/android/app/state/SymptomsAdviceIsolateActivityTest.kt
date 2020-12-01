package uk.nhs.nhsx.covid19.android.app.state

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot

class SymptomsAdviceIsolateActivityTest : EspressoTest() {

    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun testScreenWithPositiveSymptoms() = notReported {
        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkStateInfoViewForPositiveSymptoms()

        symptomsAdviceIsolateRobot.checkExposureLinkIsDisplayed()
    }

    @Test
    fun testScreenWithNegativeSymptoms() = notReported {
        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, false)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkStateInfoViewForNegativeSymptoms()

        symptomsAdviceIsolateRobot.checkExposureLinkIsNotDisplayed()
    }

    @Test
    fun testOrderButtonIsShowingWhenTestOrderingFeatureFlagIsEnabled() = notReported {
        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkBottomActionButtonIsDisplayed()

        symptomsAdviceIsolateRobot.checkExposureLinkIsDisplayed()
    }
}
