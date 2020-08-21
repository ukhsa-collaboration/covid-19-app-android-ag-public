package uk.nhs.nhsx.covid19.android.app

enum class SupportedLanguage(val displayName: String, val code: String? = null) {
    DEFAULT("Default"),
    BANGLA("Bangla", "bn"),
    URDU("Urdu", "ur"),
    PUNJABI("Punjabi", "pa"),
    GUJARATI("Gujarati", "gu"),
    WELSH("Welsh", "cy"),
}
