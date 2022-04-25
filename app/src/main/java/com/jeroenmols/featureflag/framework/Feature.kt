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
 * e.g we develop a feature, test it, release it, then we remove it and the feature remains in the app
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
    BATTERY_OPTIMIZATION(
        "feature.batteryOptimization",
        "Battery optimization",
        "Enable in-app battery optimization request",
        defaultValue = false
    ),
    SUBMIT_ANALYTICS_VIA_ALARM_MANAGER(
        "feature.submitAnalyticsViaAlarmManager",
        "Submit analytics via alarm manager",
        "Analytics submission is triggered by the AlarmManager instead of WorkManager",
        defaultValue = true
    ),
    REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS(
        "feature.remoteServiceExceptionCrashAnalytics",
        "Enable RemoteServiceException crash analytics",
        "Store and send RemoteServiceException crash analytics data",
        defaultValue = false
    ),
    NEW_NO_SYMPTOMS_SCREEN(
        "feature.NewNoSymptomsScreen",
        "New no symptoms screen",
        "Show new no symptoms screen",
        defaultValue = false
    ),
    LOCAL_COVID_STATS(
        "feature.LocalCovidStats",
        "Local Covid Stats page",
        "Show Local Covid Stats page",
        defaultValue = false
    ),
    VENUE_CHECK_IN_BUTTON(
        "feature.VenueCheckIn",
        "Venue check-in Home Screen button",
        "Show Venue check-in Home Screen button",
        defaultValue = false
    ),
    OLD_ENGLAND_CONTACT_CASE_FLOW(
        "feature.ContactCaseNoQuestionnaireJourneyEngland",
        "Old England contact case flow",
        "Show new contact case journey for England (automatic opt-out)",
        defaultValue = false
    ),
    OLD_WALES_CONTACT_CASE_FLOW(
        "feature.ContactCaseNoQuestionnaireJourneyWales",
        "Old Wales contact case flow",
        "Show new contact case journey for Wales (automatic opt-out)",
        defaultValue = false
    ),
    TESTING_FOR_COVID19_HOME_SCREEN_BUTTON(
        "feature.TestingCOVIDHub",
        "Testing for COVID-19 Home Screen button",
        "Show Testing for COVID-19 Home Screen button",
        defaultValue = false
    ),
    SELF_ISOLATION_HOME_SCREEN_BUTTON_ENGLAND(
        "feature.SelfIsolationHubEngland",
        "Self Isolation Home Screen button England",
        "Show Self Isolation Home Screen button for England",
        defaultValue = false
    ),
    SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES(
        "feature.SelfIsolationHubWales",
        "Self Isolation Home Screen button Wales",
        "Show Self Isolation Home Screen button for Wales",
        defaultValue = false
    ),
    COVID19_GUIDANCE_HOME_SCREEN_BUTTON_ENGLAND(
        "feature.CovidGuidanceHubEngland",
        "COVID-19 Guidance Home Screen button England",
        "Show COVID-19 Guidance Home Screen button for England",
        defaultValue = true
    ),
    COVID19_GUIDANCE_HOME_SCREEN_BUTTON_WALES(
        "feature.CovidGuidanceHubWales",
        "COVID-19 Guidance Home Screen button Wales",
        "Show COVID-19 Guidance Home Screen button for Wales",
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
    USE_WEB_VIEW_FOR_INTERNAL_BROWSER(
        "testsetting.usewebview",
        "Use WebView for internal browser",
        "Appium tests has problem connecting to CustomTabs tabs, so we need to provide alternative option",
        defaultValue = false
    ),
    USE_WEB_VIEW_FOR_EXTERNAL_BROWSER(
        "testsetting.usewebview_for_external_browser",
        "Use WebView for external browser",
        "We are seeing some problems with UI tests running on emulator, that test flag should help with that",
        defaultValue = false
    )
}
