package uk.nhs.nhsx.covid19.android.app

enum class SupportedLanguage(val displayName: String, val code: String? = null) {
    DEFAULT("Default"),
    ENGLISH("English", "en"),
    BANGLA("Bangla", "bn"),
    URDU("Urdu", "ur"),
    PUNJABI("Punjabi", "pa"),
    GUJARATI("Gujarati", "gu"),
    WELSH("Welsh", "cy"),
    ARABIC("Arabic", "ar"),
    SIMPLIFIED_CHINESE("Chinese (Simplified)", "zh-Hans"),
    TRADITIONAL_CHINESE("Chinese (Traditional)", "zh-Hant"),
    ROMANIAN("Romanian", "ro"),
    TURKISH("Turkish", "tr"),
}
