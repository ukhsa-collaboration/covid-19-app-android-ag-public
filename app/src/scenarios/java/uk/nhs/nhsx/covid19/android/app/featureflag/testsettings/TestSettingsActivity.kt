package uk.nhs.nhsx.covid19.android.app.featureflag.testsettings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestSettingsBinding

private const val TAG_TEST_SETTING = "TAG_TEST_SETTING"

class TestSettingsActivity : AppCompatActivity(), TestSettingsFragment.TestSettingsListener {

    private lateinit var binding: ActivityTestSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag(TAG_TEST_SETTING) == null) {
            val settingsFragment =
                TestSettingsFragment().apply { testSettingListener = this@TestSettingsActivity }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, settingsFragment).commit()
        } else {
            (supportFragmentManager.findFragmentByTag(TAG_TEST_SETTING) as TestSettingsFragment).testSettingListener =
                this
        }
    }

    override fun onFeatureToggleClicked() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                FeatureSelectFragment.getInstance(false)
            )
            .addToBackStack(null).commit()
    }

    override fun onTestSettingClicked() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                FeatureSelectFragment.getInstance(true)
            )
            .addToBackStack(null).commit()
    }
}
