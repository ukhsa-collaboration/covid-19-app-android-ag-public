package uk.nhs.nhsx.covid19.android.app.common

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.appComponent
import java.util.Locale
import javax.inject.Inject

abstract class BaseActivity(contentView: Int) : AppCompatActivity(contentView) {

    @Inject
    lateinit var applicationLocaleProvider: ApplicationLocaleProvider

    override fun attachBaseContext(baseContext: Context) {
        baseContext.applicationContext.appComponent.inject(this)
        if (BuildConfig.FLAVOR == "scenarios") {
            super.attachBaseContext(updateBaseContextLocale(baseContext))
        } else {
            super.attachBaseContext(baseContext)
        }
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val locale = applicationLocaleProvider.getLocale()
        Locale.setDefault(locale)
        return if (VERSION.SDK_INT > VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else {
            updateResourcesLocaleLegacy(context, locale)
        }
    }

    @TargetApi(VERSION_CODES.N_MR1)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    @SuppressWarnings("deprecation")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val config = context.resources.configuration
        config.locale = locale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context
    }
}
