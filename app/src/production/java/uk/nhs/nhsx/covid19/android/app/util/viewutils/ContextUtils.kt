package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import androidx.annotation.StyleableRes
import java.util.Locale

val Context.overriddenResources: Resources
    get() = resources

fun Context.updateBaseContextLocale(locale: Locale): Context {
    Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    return createConfigurationContext(config)
}

fun TypedArray.getString(context: Context, @StyleableRes resourceId: Int): String =
    this.getString(resourceId) ?: ""
