package uk.nhs.nhsx.covid19.android.app.util

import androidx.annotation.StringRes
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistrictAreaStringProvider @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {

    suspend fun provide(@StringRes stringResId: Int): Int {
        return when (localAuthorityPostCodeProvider.getPostCodeDistrict()) {
            WALES -> welshMapping[stringResId] ?: stringResId
            else -> stringResId
        }
    }

    companion object {
        private val welshMapping = mapOf(
            R.string.url_privacy_notice to R.string.url_privacy_notice_wls,
            R.string.url_terms_of_use to R.string.url_terms_of_use_wls,
            R.string.url_nhs_111_online to R.string.url_nhs_111_online_wls,
            R.string.url_accessibility_statement to R.string.url_accessibility_statement_wls,
            R.string.url_postal_code_risk_more_info to R.string.url_postal_code_risk_more_info_wls,
            R.string.url_how_app_works to R.string.url_how_app_works_wls,
            R.string.url_common_questions to R.string.url_common_questions_wls,
            R.string.url_latest_advice to R.string.url_latest_advice_wls,
            R.string.url_latest_advice_in_isolation to R.string.url_latest_advice_in_isolation_wls,
            R.string.url_order_test_privacy to R.string.url_order_test_privacy_wls,
            R.string.url_order_test_for_someone_else to R.string.url_order_test_for_someone_else_wls,
            R.string.url_nhs_get_tested to R.string.url_nhs_get_tested_wls,
            R.string.url_latest_government_guidance to R.string.url_latest_government_guidance_wls,
            R.string.url_nhs_guidance to R.string.url_nhs_guidance_wls,
            R.string.exposure_notification_vaccination_status_all_doses_question_link_url to R.string.exposure_notification_vaccination_status_all_doses_question_link_url_wls,
        )
    }
}
