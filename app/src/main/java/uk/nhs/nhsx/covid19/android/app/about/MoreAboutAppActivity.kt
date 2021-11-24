package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityMoreAboutAppBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar

class MoreAboutAppActivity : BaseActivity() {

    private lateinit var binding: ActivityMoreAboutAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.about_this_app_title,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            textSoftwareName.setText(R.string.app_name)
            textSoftwareVersion.text = BuildConfig.VERSION_NAME
            textSoftwareDateOfRelease.text = BuildConfig.RELEASE_DATE
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, MoreAboutAppActivity::class.java)
    }
}
