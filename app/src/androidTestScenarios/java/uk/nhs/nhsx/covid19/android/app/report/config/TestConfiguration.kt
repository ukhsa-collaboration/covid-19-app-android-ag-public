package uk.nhs.nhsx.covid19.android.app.report.config

enum class Orientation(val exportName: String) {
    LANDSCAPE("landscape-left"), PORTRAIT("portrait")
}

enum class FontScale(val scale: Float, val exportName: String) {
    SMALL(0.85f, "content-size-S"),
    DEFAULT(1.0f, "content-size-M"),
    LARGE(1.15f, "content-size-L"),
    LARGEST(1.3f, "content-size-XL")
}

enum class Theme(val exportName: String) {
    LIGHT("light"), DARK("dark")
}

data class TestConfiguration(
    val orientation: Orientation,
    val fontScale: FontScale,
    val theme: Theme,
    val languageCode: String? = null
)
