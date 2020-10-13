package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostalDistrictProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    private var value: String? by prefs

    fun toPostalDistrict(): PostCodeDistrict? = when (value) {
        ENGLAND.name -> ENGLAND
        WALES.name -> WALES
        SCOTLAND.name -> SCOTLAND
        NORTHERN_IRELAND.name -> NORTHERN_IRELAND
        else -> null
    }

    fun storePostalDistrict(postCodeDistrict: PostCodeDistrict?) {
        value = postCodeDistrict?.let {
            postCodeDistrict.name
        }
    }

    companion object {
        private const val VALUE_KEY = "MAIN_POST_CODE_DISTRICT"
    }
}

enum class PostCodeDistrict(val value: String) {
    ENGLAND("England"),
    WALES("Wales"),
    SCOTLAND("Scotland"),
    NORTHERN_IRELAND("NorthernIreland");

    companion object {
        fun fromString(stringValue: String?): PostCodeDistrict? =
            when (stringValue) {
                ENGLAND.value -> ENGLAND
                WALES.value -> WALES
                SCOTLAND.value -> SCOTLAND
                NORTHERN_IRELAND.value -> NORTHERN_IRELAND
                else -> null
            }
    }
}
