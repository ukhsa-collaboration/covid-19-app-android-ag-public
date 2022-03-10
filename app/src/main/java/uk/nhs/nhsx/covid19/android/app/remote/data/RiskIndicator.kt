package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.BLACK
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import javax.inject.Inject

@Parcelize
@JsonClass(generateAdapter = true)
data class RiskIndicator(
    val colorScheme: ColorScheme,
    val colorSchemeV2: ColorScheme? = null,
    val name: TranslatableString,
    val heading: TranslatableString,
    val content: TranslatableString,
    val linkTitle: TranslatableString,
    val linkUrl: TranslatableString,
    val policyData: PolicyData?
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class PolicyData(
    val heading: TranslatableString,
    val content: TranslatableString,
    val footer: TranslatableString,
    val localAuthorityRiskTitle: TranslatableString,
    val policies: List<Policy>
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class Policy(
    val policyIcon: PolicyIcon,
    val policyHeading: TranslatableString,
    val policyContent: TranslatableString
) : Parcelable

@Parcelize
enum class PolicyIcon(
    val jsonName: String
) : Parcelable {
    DEFAULT("default-icon"),

    MEETING_PEOPLE("meeting-people"),

    BARS_AND_PUBS("bars-and-pubs"),

    WORSHIP("worship"),

    OVERNIGHT_STAYS("overnight-stays"),

    EDUCATION("education"),

    TRAVELLING("travelling"),

    EXERCISE("exercise"),

    WEDDINGS_AND_FUNERALS("weddings-and-funerals"),

    BUSINESSES("businesses"),

    RETAIL("retail"),

    ENTERTAINMENT("entertainment"),

    PERSONAL_CARE("personal-care"),

    LARGE_EVENTS("large-events"),

    CLINICALLY_EXTREMELY_VULNERABLE("clinically-extremely-vulnerable"),

    SOCIAL_DISTANCING("social-distancing"),

    FACE_COVERINGS("face-coverings"),

    MEETING_OUTDOORS("meeting-outdoors"),

    MEETING_INDOORS("meeting-indoors"),

    WORK("work"),

    INTERNATIONAL_TRAVEL("international-travel"),
}

@Parcelize
@JsonClass(generateAdapter = true)
data class RiskIndicatorWrapper(
    val riskLevel: String? = null,
    val riskIndicator: RiskIndicator? = null,
    val riskLevelFromLocalAuthority: Boolean = false
) : Parcelable

@Parcelize
enum class ColorScheme(
    val jsonName: String
) : Parcelable {
    NEUTRAL(jsonName = "neutral"),

    GREEN(jsonName = "green"),

    YELLOW(jsonName = "yellow"),

    AMBER(jsonName = "amber"),

    RED(jsonName = "red"),

    MAROON(jsonName = "maroon"),

    BLACK(jsonName = "black"),
}

class ColorSchemeToImageResource @Inject constructor() {

    @DrawableRes
    operator fun invoke(scheme: ColorScheme): Int {
        return when (scheme) {
            NEUTRAL -> R.drawable.ic_map_risk_neutral
            GREEN -> R.drawable.ic_map_risk_green
            YELLOW -> R.drawable.ic_map_risk_yellow
            AMBER -> R.drawable.ic_map_risk_amber
            RED -> R.drawable.ic_map_risk_red
            MAROON -> R.drawable.ic_map_risk_maroon
            BLACK -> R.drawable.ic_map_risk_black
        }
    }
}
