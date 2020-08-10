package uk.nhs.nhsx.covid19.android.app.featureflag.testsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.scenarios.fragment_test_settings.textViewTestSettingsFeatureToggle
import kotlinx.android.synthetic.scenarios.fragment_test_settings.textViewTestSettingsTestSettings
import uk.nhs.nhsx.covid19.android.app.R

class TestSettingsFragment : Fragment(R.layout.fragment_test_settings) {

    interface TestSettingsListener {
        fun onFeatureToggleClicked()
        fun onTestSettingClicked()
    }

    var testSettingListener: TestSettingsListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_test_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewTestSettingsFeatureToggle.setOnClickListener { testSettingListener?.onFeatureToggleClicked() }
        textViewTestSettingsTestSettings.setOnClickListener { testSettingListener?.onTestSettingClicked() }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = "Test Settings"
    }
}
