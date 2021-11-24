package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.viewutils.overriddenResources
import uk.nhs.nhsx.covid19.android.app.util.viewutils.updateBaseContextLocale
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity {

    constructor(contentView: Int) : super(contentView)
    constructor() : super()

    @Inject
    lateinit var applicationLocaleProvider: ApplicationLocaleProvider
    private var currentLanguageCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        currentLanguageCode = applicationLocaleProvider.languageCode
    }

    override fun attachBaseContext(baseContext: Context) {
        baseContext.applicationContext.appComponent.inject(this)
        super.attachBaseContext(updateBaseContextLocale(baseContext))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode: Int = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onResume() {
        super.onResume()
        val updatedLanguage = applicationLocaleProvider.languageCode

        if (currentLanguageCode != updatedLanguage) {
            finish()
            startActivity(intent)
        }
    }

    protected fun setAccessibilityTitle(@StringRes resId: Int) {
        setAccessibilityTitle(getString(resId))
    }

    protected fun setAccessibilityTitle(text: String) {
        title = text
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val locale = applicationLocaleProvider.getLocale()
        Timber.d("updateBaseContextLocale to $locale")
        return context.updateBaseContextLocale(locale)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            val menuItem: MenuItem? =
                menu.findItem(R.id.menuEditAction) // explicitly mentioned nullability because it will crash on some systems otherwise
            menuItem?.title = overriddenResources.getString(R.string.edit)
        }
        return super.onPrepareOptionsMenu(menu)
    }
}
