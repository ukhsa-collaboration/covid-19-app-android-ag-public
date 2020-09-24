package com.jeroenmols.featureflag.framework

import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_POST_DISTRICTS
import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_VENUES
import com.jeroenmols.featureflag.framework.FeatureFlag.IN_APP_REVIEW
import com.jeroenmols.featureflag.framework.FeatureFlag.ISOLATION_COMPANION
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_DIAGNOSIS
import com.jeroenmols.featureflag.framework.FeatureFlag.SIGNATURE_VALIDATION
import com.jeroenmols.featureflag.framework.FeatureFlag.TEST_ORDERING

class StoreFeatureFlagProvider : FeatureFlagProvider {

    override val priority = MIN_PRIORITY

    @Suppress("ComplexMethod")
    override fun isFeatureEnabled(feature: Feature): Boolean {
        return if (feature is FeatureFlag) {
            when (feature) {
                HIGH_RISK_POST_DISTRICTS -> true
                HIGH_RISK_VENUES -> true
                SELF_DIAGNOSIS -> true
                ISOLATION_COMPANION -> true
                TEST_ORDERING -> true
                SIGNATURE_VALIDATION -> true
                IN_APP_REVIEW -> true
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
