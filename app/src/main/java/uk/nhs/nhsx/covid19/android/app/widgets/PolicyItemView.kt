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
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon

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
            PolicyIcon.DEFAULT -> R.drawable.ic_policy_default_icon
            PolicyIcon.MEETING_PEOPLE -> R.drawable.ic_policy_meeting_people
            PolicyIcon.BARS_AND_PUBS -> R.drawable.ic_policy_bars_and_pubs
            PolicyIcon.WORSHIP -> R.drawable.ic_policy_worship
            PolicyIcon.OVERNIGHT_STAYS -> R.drawable.ic_policy_overnight_stays
            PolicyIcon.EDUCATION -> R.drawable.ic_policy_education
            PolicyIcon.TRAVELLING -> R.drawable.ic_policy_travelling
            PolicyIcon.EXERCISE -> R.drawable.ic_policy_exercise
            PolicyIcon.WEDDINGS_AND_FUNERALS -> R.drawable.ic_policy_weddings_and_funerals
        }
        val heading = policy.policyHeading.translate()
        val content = policy.policyContent.translate()

        policyIcon.setImageResource(iconRes)
        policyItemHeading.text = heading
        policyItemContent.text = content
    }
}
