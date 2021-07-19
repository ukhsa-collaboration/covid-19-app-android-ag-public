package uk.nhs.nhsx.covid19.android.app

import androidx.annotation.StringRes

enum class SupportedLanguage(
    @StringRes val languageName: Int,
    val nativeLanguageName: String,
    val code: String
) {
    ENGLISH(R.string.english, "English (UK)", "en"),
    BENGALI(R.string.bengali, "বাংলা", "bn"),
    URDU(R.string.urdu, "اردو", "ur"),
    PUNJABI(R.string.punjabi, "ਪੰਜਾਬੀ", "pa"),
    GUJARATI(R.string.gujarati, "ગુજરાતી", "gu"),
    WELSH(R.string.welsh, "Cymraeg", "cy"),
    ARABIC(R.string.arabic, "العربية", "ar"),
    CHINESE(R.string.chinese, "中文（简体）", "zh"),
    ROMANIAN(R.string.romanian, "Română", "ro"),
    TURKISH(R.string.turkish, "Türkçe", "tr"),
    POLISH(R.string.polish, "Polski", "pl"),
    SOMALI(R.string.somali, "Soomaali", "so"),
}

data class SupportedLanguageItem(
    @StringRes val nameResId: Int,
    val code: String?
)

fun SupportedLanguage?.toSupportedLanguageItem() =
    if (this == null) {
        SupportedLanguageItem(nameResId = R.string.default_language, code = null)
    } else {
        SupportedLanguageItem(nameResId = this.languageName, code = this.code)
    }
