package uk.nhs.nhsx.covid19.android.app.state

import com.jeroenmols.featureflag.framework.FeatureFlag
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
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.TEST_ORDERING)

        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkStateInfoViewForPositiveSymptoms()
    }

    @Test
    fun testScreenWithNegativeSymptoms() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.TEST_ORDERING)

        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, false)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkStateInfoViewForNegativeSymptoms()
    }

    @Test
    fun testOrderButtonIsShowingWhenTestOrderingFeatureFlagIsEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.TEST_ORDERING)

        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkBottomActionButtonIsDisplayed()
    }

    @Test
    fun testOrderButtonIsNotShowingWhenTestOrderingFeatureFlagIsDisabled() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.TEST_ORDERING)

        startTestActivity<SymptomsAdviceIsolateActivity>() {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkBottomActionButtonIsNotDisplayed()
    }
}
