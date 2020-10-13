package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import java.util.Locale
import javax.inject.Inject

abstract class BaseActivity(contentView: Int) : AppCompatActivity(contentView) {

    @Inject
    lateinit var applicationLocaleProvider: ApplicationLocaleProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(baseContext: Context) {
        baseContext.applicationContext.appComponent.inject(this)
        if (BuildConfig.FLAVOR == "scenarios") {
            super.attachBaseContext(updateBaseContextLocale(baseContext))
        } else {
            super.attachBaseContext(baseContext)
        }
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode: Int = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val locale = applicationLocaleProvider.getLocale()
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
