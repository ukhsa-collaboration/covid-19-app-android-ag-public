package com.jeroenmols.featureflag.framework

import com.jeroenmols.featureflag.framework.FeatureFlag.BATTERY_OPTIMIZATION
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER

class StoreFeatureFlagProvider : FeatureFlagProvider {

    override val priority = MIN_PRIORITY

    @Suppress("ComplexMethod")
    override fun isFeatureEnabled(feature: Feature): Boolean {
        return if (feature is FeatureFlag) {
            when (feature) {
                BATTERY_OPTIMIZATION -> false
                DAILY_CONTACT_TESTING -> false
                SUBMIT_ANALYTICS_VIA_ALARM_MANAGER -> true
                REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS -> false
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
