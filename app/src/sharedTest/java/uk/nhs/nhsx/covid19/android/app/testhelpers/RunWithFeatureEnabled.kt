package uk.nhs.nhsx.covid19.android.app.testhelpers

import com.jeroenmols.featureflag.framework.Feature
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.RuntimeBehavior

fun runWithFeatureEnabled(feature: Feature, clearFeatureFlags: Boolean = false, action: () -> Unit) {
    if (clearFeatureFlags) {
        FeatureFlagTestHelper.clearFeatureFlags()
    }
    val wasFeatureEnabledInitially = RuntimeBehavior.isFeatureEnabled(feature)
    if (!wasFeatureEnabledInitially) {
        FeatureFlagTestHelper.enableFeatureFlag(feature)
    }
    try {
        action()
    } finally {
        if (!wasFeatureEnabledInitially) {
            FeatureFlagTestHelper.disableFeatureFlag(feature)
        }
    }
}

fun runWithFeature(feature: Feature, enabled: Boolean, action: () -> Unit) {
    setupFeature(feature, enabled)
    try {
        action()
    } finally {
        FeatureFlagTestHelper.clearFeatureFlags()
    }
}

fun runWithFeatures(featureList: List<Feature>, enabled: Boolean, action: () -> Unit) {
    for (feature in featureList) {
        setupFeature(feature, enabled)
    }
    try {
        action()
    } finally {
        FeatureFlagTestHelper.clearFeatureFlags()
    }
}

suspend fun coRunWithFeature(
    feature: Feature,
    enabled: Boolean,
    clearFeatureFlags: Boolean = false,
    action: suspend () -> Unit
) {
    if (clearFeatureFlags) {
        FeatureFlagTestHelper.clearFeatureFlags()
    }
    setupFeature(feature, enabled)
    try {
        action()
    } finally {
        FeatureFlagTestHelper.clearFeatureFlags()
    }
}

private fun setupFeature(feature: Feature, enabled: Boolean) {
    if (enabled) {
        FeatureFlagTestHelper.enableFeatureFlag(feature)
    } else {
        FeatureFlagTestHelper.disableFeatureFlag(feature)
    }
}
