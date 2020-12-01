package com.jeroenmols.featureflag.framework

import com.jeroenmols.featureflag.framework.FeatureFlag.BATTERY_OPTIMIZATION
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_AUTHORITY

class StoreFeatureFlagProvider : FeatureFlagProvider {

    override val priority = MIN_PRIORITY

    @Suppress("ComplexMethod")
    override fun isFeatureEnabled(feature: Feature): Boolean {
        return if (feature is FeatureFlag) {
            when (feature) {
                LOCAL_AUTHORITY -> true
                BATTERY_OPTIMIZATION -> false
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
