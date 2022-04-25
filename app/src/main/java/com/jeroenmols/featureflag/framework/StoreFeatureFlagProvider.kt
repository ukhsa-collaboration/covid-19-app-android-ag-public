package com.jeroenmols.featureflag.framework

import com.jeroenmols.featureflag.framework.FeatureFlag.BATTERY_OPTIMIZATION
import com.jeroenmols.featureflag.framework.FeatureFlag.COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_COVID_STATS
import com.jeroenmols.featureflag.framework.FeatureFlag.NEW_NO_SYMPTOMS_SCREEN
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import com.jeroenmols.featureflag.framework.FeatureFlag.TESTING_FOR_COVID19_HOME_SCREEN_BUTTON
import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON

class StoreFeatureFlagProvider : FeatureFlagProvider {

    override val priority = MIN_PRIORITY

    @Suppress("ComplexMethod")
    override fun isFeatureEnabled(feature: Feature): Boolean {
        return if (feature is FeatureFlag) {
            when (feature) {
                BATTERY_OPTIMIZATION -> false
                SUBMIT_ANALYTICS_VIA_ALARM_MANAGER -> true
                REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS -> false
                NEW_NO_SYMPTOMS_SCREEN -> false
                LOCAL_COVID_STATS -> false
                VENUE_CHECK_IN_BUTTON -> false
                OLD_ENGLAND_CONTACT_CASE_FLOW -> false
                OLD_WALES_CONTACT_CASE_FLOW -> false
                TESTING_FOR_COVID19_HOME_SCREEN_BUTTON -> false
                SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND -> false
                SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES -> false
                COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND -> true
                COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES -> true
            }
        } else {
            // TestSettings should never be shipped to users
            when (feature as TestSetting) {
                else -> false
            }
        }
    }

    override fun hasFeature(feature: Feature): Boolean = true
}
