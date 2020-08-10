package uk.nhs.nhsx.covid19.android.app.state

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.PositiveSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PositiveSymptomsRobot
import java.time.Instant
import java.time.LocalDate

class PositiveSymptomsActivityTest : EspressoTest() {

    private val positiveSymptomsRobot = PositiveSymptomsRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun testOrderButtonIsShowingWhenTestOrderingFeatureFlagIsEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.TEST_ORDERING)

        val onsetDate = LocalDate.parse("2020-05-20")
        testAppContext.setState(
            Isolation(Instant.now(), LocalDate.now().plusDays(5), indexCase = IndexCase(onsetDate))
        )

        startTestActivity<PositiveSymptomsActivity>()

        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.checkTestOrderingButtonIsDisplayed()
    }

    @Test
    fun testOrderButtonIsNotShowingWhenTestOrderingFeatureFlagIsDisabled() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.TEST_ORDERING)

        val onsetDate = LocalDate.parse("2020-05-20")
        testAppContext.setState(
            Isolation(
                Instant.now(),
                LocalDate.now().plusDays(5),
                indexCase = IndexCase(onsetDate)
            )
        )

        startTestActivity<PositiveSymptomsActivity>()

        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.checkTestOrderingButtonIsNotDisplayed()
    }
}
