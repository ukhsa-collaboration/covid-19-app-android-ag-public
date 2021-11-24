package uk.nhs.nhsx.covid19.android.app.featureflag.testsettings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jeroenmols.featureflag.framework.Feature
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagProvider
import com.jeroenmols.featureflag.framework.RuntimeFeatureFlagProvider
import com.jeroenmols.featureflag.framework.TestSetting
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.FragmentFeatureFlagBinding
import kotlin.system.exitProcess

internal class FeatureSelectFragment : Fragment() {

    private var _binding: FragmentFeatureFlagBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeatureFlagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        initializeRecyclerView()
        activity?.title = if (useTestSettings()) "Test (automation) settings" else "Feature Flags"
    }

    private fun initializeRecyclerView() {
        val runtimeFeatureFlagProvider = RuntimeFeatureFlagProvider(requireContext())
        val checkedListener = { feature: Feature, enabled: Boolean ->
            runtimeFeatureFlagProvider.setFeatureEnabled(feature, enabled)
            requestRestart()
        }

        with(binding) {
            if (useTestSettings()) {
                recyclerView.adapter =
                    FeatureFlagViewAdapter(
                        TestSetting.values(),
                        runtimeFeatureFlagProvider,
                        checkedListener
                    )
            } else {
                recyclerView.adapter =
                    FeatureFlagViewAdapter(
                        FeatureFlag.values(),
                        runtimeFeatureFlagProvider,
                        checkedListener
                    )
            }
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    private fun useTestSettings() = arguments?.getBoolean(USE_TEST_SETTINGS, false) == true

    companion object {
        fun getInstance(showTestSettings: Boolean): FeatureSelectFragment {
            val args = Bundle().apply { putBoolean(USE_TEST_SETTINGS, showTestSettings) }
            return FeatureSelectFragment().apply { arguments = args }
        }

        private const val USE_TEST_SETTINGS = "useTestSettings"
    }

    private fun requestRestart() {
        val msg = "In order for changes to reflect please restart the app via settings"
        val snackbar = Snackbar.make(requireView(), msg, Snackbar.LENGTH_INDEFINITE)
            .setActionTextColor(Color.RED)
            .setAction("Force Stop") {
                exitProcess(-1)
            }
        snackbar.view.setBackgroundColor(Color.BLACK)
        snackbar.show()
    }
}

private class FeatureFlagViewAdapter<T : Feature>(
    val items: Array<T>,
    val provider: FeatureFlagProvider,
    val checkedListener: Function2<Feature, Boolean, Unit>
) : RecyclerView.Adapter<FeatureFlagViewHolder<T>>() {

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FeatureFlagViewHolder<T>, position: Int) =
        holder.bind(items[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureFlagViewHolder<T> {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_featureflag, parent, false)
        return FeatureFlagViewHolder(itemView, provider, checkedListener)
    }
}

private class FeatureFlagViewHolder<T : Feature>(
    view: View,
    val provider: FeatureFlagProvider,
    val checkedListener: Function2<Feature, Boolean, Unit>
) : RecyclerView.ViewHolder(view) {

    fun bind(feature: T) {
        itemView.findViewById<TextView>(R.id.textViewFeatureFlagTitle).text = feature.title
        itemView.findViewById<TextView>(R.id.textViewFeatureFlagDescription).text =
            feature.explanation
        itemView.findViewById<SwitchCompat>(R.id.switchFeatureFlag).isChecked =
            provider.isFeatureEnabled(feature)
        itemView.findViewById<SwitchCompat>(R.id.switchFeatureFlag)
            .setOnCheckedChangeListener { switch, isChecked ->
                if (switch.isPressed) checkedListener.invoke(feature, isChecked)
            }
    }
}
