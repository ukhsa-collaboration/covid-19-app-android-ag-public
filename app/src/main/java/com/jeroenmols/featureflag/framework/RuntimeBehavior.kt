package com.jeroenmols.featureflag.framework

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Check whether a feature should be enabled or not. Based on the priority of the different providers and if said
 * provider explicitly defines a value for that feature, the value of the flag is returned.
 */
object RuntimeBehavior {

    @VisibleForTesting
    internal val providers = CopyOnWriteArrayList<FeatureFlagProvider>()

    fun initialize(context: Context, isDebugBuild: Boolean) {
        if (isDebugBuild) {
            val runtimeFeatureFlagProvider = RuntimeFeatureFlagProvider(context)
            addProvider(runtimeFeatureFlagProvider)
            addProvider(TestFeatureFlagProvider)
        } else {
            addProvider(StoreFeatureFlagProvider())
        }
    }

    fun isFeatureEnabled(feature: Feature): Boolean {
        return providers.filter { it.hasFeature(feature) }
            .minBy(FeatureFlagProvider::priority)
            ?.isFeatureEnabled(feature)
            ?: feature.defaultValue
    }

    fun refreshFeatureFlags() {
        providers.filter { it is RemoteFeatureFlagProvider }
            .forEach { (it as RemoteFeatureFlagProvider).refreshFeatureFlags() }
    }

    fun addProvider(provider: FeatureFlagProvider) {
        providers.add(provider)
    }

    fun clearFeatureFlagProviders() = providers.clear()

    fun removeAllFeatureFlagProviders(priority: Int) =
        providers.removeAll(providers.filter { it.priority == priority })
}
