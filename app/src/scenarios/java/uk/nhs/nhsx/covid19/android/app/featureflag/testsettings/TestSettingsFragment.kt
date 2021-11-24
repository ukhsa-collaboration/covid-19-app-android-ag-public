package uk.nhs.nhsx.covid19.android.app.featureflag.testsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.FragmentTestSettingsBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class TestSettingsFragment : Fragment(R.layout.fragment_test_settings) {

    private var _binding: FragmentTestSettingsBinding? = null
    private val binding get() = _binding!!

    interface TestSettingsListener {
        fun onFeatureToggleClicked()
        fun onTestSettingClicked()
    }

    var testSettingListener: TestSettingsListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            textViewTestSettingsFeatureToggle.setOnSingleClickListener { testSettingListener?.onFeatureToggleClicked() }
            textViewTestSettingsTestSettings.setOnSingleClickListener { testSettingListener?.onTestSettingClicked() }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = "Test Settings"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
