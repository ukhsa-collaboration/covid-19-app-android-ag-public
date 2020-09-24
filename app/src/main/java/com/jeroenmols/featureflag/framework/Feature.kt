package com.jeroenmols.featureflag.framework

/**
 * A Feature uniquely identifies a part of the app code that can either be enabled or disabled.
 * Features only have two states by design to simplify the implementation
 *
 * @property key unique value that identifies a test setting (for "Remote Config tool" flags this is shared across Android/iOS)
 */
interface Feature {
    val key: String
    val title: String
    val explanation: String
    val defaultValue: Boolean
}

/**
 * A feature flag is something that disappears over time (hence it is a tool to simplify development)
 * e.g we develop a feature, test it, release it, then we remove it and the feature remain in the app
 *
 * Note that this has nothing to do with being available as a remote feature flag or not. Some features
 * will be deployed using our feature flag tool, some will not.
 *
 * [key] Shared between Android and iOS featureflag backend
 */
enum class FeatureFlag(
    override val key: String,
    override val title: String,
    override val explanation: String,
    override val defaultValue: Boolean = true
) : Feature {
    HIGH_RISK_POST_DISTRICTS(
        "feature.highRiskPostDistricts",
        "High risk post districts",
        "Enabled high risk post districts",
        defaultValue = true
    ),
    HIGH_RISK_VENUES(
        "feature.highRiskVenues",
        "High-risk venues",
        "Enabled high-risk venues",
        defaultValue = true
    ),
    SELF_DIAGNOSIS(
        "feature.selfDiagnosis",
        "Self-diagnosis",
        "Enabled self-diagnosis",
        defaultValue = true
    ),
    ISOLATION_COMPANION(
        "feature.isolationCompanion",
        "Isolation companion",
        "Enabled isolation companion",
        defaultValue = true
    ),
    TEST_ORDERING(
        "feature.testOrdering",
        "Test ordering",
        "Enable test ordering",
        defaultValue = true
    ),
    SIGNATURE_VALIDATION(
        "feature.signatureValidation",
        "Signature validation",
        "Enable signature validation",
        defaultValue = true
    ),
    IN_APP_REVIEW(
        "feature.inAppReview",
        "In-app Review",
        "Enable in-app reviews",
        defaultValue = true
    )
}

/**
 * A test setting is something that stays in our app forever (hence it is a tool to simplify testing)
 * e.g. it is a hook into our app to allow something that a production app shouldn’t allow. (enable logging, bypass software update,…)
 *
 * Test settings must never be exposed via our remote feature flag tool.
 */
enum class TestSetting(
    override val key: String,
    override val title: String,
    override val explanation: String,
    override val defaultValue: Boolean = false
) : Feature {
    STRICT_MODE(
        "testsetting.strictmode",
        "Enable strict mode",
        "Detect IO operations accidentally performed on the main Thread",
        defaultValue = true
    ),
    DEBUG_ANALYTICS(
        "testsetting.debuganalytics",
        "Submit analytics every 15 min",
        "WorkManager will schedule the task every 15 min instead of once a day",
        defaultValue = false
    ),
    USE_WEB_VIEW_FOR_INTERNAL_BROWSER(
        "testsetting.usewebview",
        "Use WebView for internal browser",
        "Appium tests has problem connecting to CustomTabs tabs, so we need to provide alternative option",
        defaultValue = false
    )
}
