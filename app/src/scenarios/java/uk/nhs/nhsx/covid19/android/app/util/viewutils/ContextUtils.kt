package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import androidx.annotation.StyleableRes
import co.lokalise.android.sdk.LokaliseResources
import co.lokalise.android.sdk.LokaliseSDK
import co.lokalise.android.sdk.core.LokaliseContextWrapper
import co.lokalise.android.sdk.core.LokalisePreferences
import uk.nhs.nhsx.covid19.android.app.R
import java.util.Locale

val Context.overriddenResources: Resources
    get() = LokaliseResources(this)

fun Context.updateBaseContextLocale(locale: Locale): Context {
    LokaliseSDK.setLocale(locale.language)
    return LokaliseContextWrapper.wrap(this)
}

fun TypedArray.getString(context: Context, @StyleableRes resourceId: Int): String {
    val textId = getResourceId(resourceId, R.string.empty)
    return context.overriddenResources.getString(textId)
}

fun purgeLokalise(context: Context) =
    LokaliseSDK.getInstance().database.apply {
        val prefs = LokalisePreferences(context)
        prefs.BUNDLE_ID.set(0)
        clearTranslations()
    }
