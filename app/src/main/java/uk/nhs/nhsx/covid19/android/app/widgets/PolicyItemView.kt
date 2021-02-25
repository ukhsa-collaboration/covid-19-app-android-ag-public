package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_policy_item.view.policyIcon
import kotlinx.android.synthetic.main.view_policy_item.view.policyItemContent
import kotlinx.android.synthetic.main.view_policy_item.view.policyItemHeading
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.BARS_AND_PUBS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.BUSINESSES
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.CLINICALLY_EXTREMELY_VULNERABLE
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.DEFAULT
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.EDUCATION
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.ENTERTAINMENT
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.EXERCISE
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.FACE_COVERINGS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.INTERNATIONAL_TRAVEL
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.LARGE_EVENTS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.MEETING_INDOORS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.MEETING_OUTDOORS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.MEETING_PEOPLE
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.OVERNIGHT_STAYS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.PERSONAL_CARE
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.RETAIL
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.SOCIAL_DISTANCING
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.TRAVELLING
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.WEDDINGS_AND_FUNERALS
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.WORK
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.WORSHIP

class PolicyItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_policy_item, this)
        configureLayout()
    }

    private fun configureLayout() {
        orientation = HORIZONTAL
    }

    fun setPolicyItem(policy: Policy) {
        val iconRes = when (policy.policyIcon) {
            DEFAULT -> R.drawable.ic_policy_default
            OVERNIGHT_STAYS -> R.drawable.ic_policy_overnight_stays
            TRAVELLING -> R.drawable.ic_policy_travelling
            EXERCISE -> R.drawable.ic_policy_exercise
            MEETING_PEOPLE -> R.drawable.ic_policy_meeting_people
            BARS_AND_PUBS -> R.drawable.ic_policy_bars_and_pubs
            BUSINESSES -> R.drawable.ic_policy_businesses
            EDUCATION -> R.drawable.ic_policy_education
            WORSHIP -> R.drawable.ic_policy_places_of_worship
            WEDDINGS_AND_FUNERALS -> R.drawable.ic_policy_weddings_and_funerals
            RETAIL -> R.drawable.ic_policy_retail
            ENTERTAINMENT -> R.drawable.ic_policy_entertainment
            PERSONAL_CARE -> R.drawable.ic_policy_personal_care
            LARGE_EVENTS -> R.drawable.ic_policy_large_events
            CLINICALLY_EXTREMELY_VULNERABLE -> R.drawable.ic_policy_clinically_extremely_vulnerable
            SOCIAL_DISTANCING -> R.drawable.ic_policy_social_distancing
            FACE_COVERINGS -> R.drawable.ic_policy_face_coverings
            MEETING_OUTDOORS -> R.drawable.ic_policy_meeting_outdoors
            MEETING_INDOORS -> R.drawable.ic_policy_meeting_indoors
            WORK -> R.drawable.ic_policy_work
            INTERNATIONAL_TRAVEL -> R.drawable.ic_policy_international_travel
        }
        val heading = policy.policyHeading.translate()
        val content = policy.policyContent.translate()

        policyIcon.setImageResource(iconRes)
        policyItemHeading.text = heading
        policyItemContent.text = content
    }
}
