package uk.nhs.nhsx.covid19.android.app.report.config

enum class Orientation(val exportName: String, val shortName: String) {
    LANDSCAPE("landscape-left", "L"), PORTRAIT("portrait", "P")
}

enum class FontScale(val scale: Float, val exportName: String, val shortName: String) {
    SMALL(0.85f, "content-size-S", "S"),
    DEFAULT(1.0f, "content-size-M", "M"),
    LARGE(1.15f, "content-size-L", "L"),
    LARGEST(1.3f, "content-size-XL", "X")
}

enum class Theme(val exportName: String, val shortName: String) {
    LIGHT("light", "L"), DARK("dark", "D")
}

data class TestConfiguration(
    val orientation: Orientation,
    val fontScale: FontScale,
    val theme: Theme,
    val languageCode: String = "en"
) {
    override fun toString(): String {
        return languageCode + orientation.shortName + fontScale.shortName + theme.shortName
    }
}
