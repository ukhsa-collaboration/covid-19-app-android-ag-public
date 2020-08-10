package uk.nhs.nhsx.covid19.android.app.fieldtests.ui.main

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.RequestCodes
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.fieldtests.storage.ExperimentSettingsProvider
import androidx.core.widget.addTextChangedListener
import timber.log.Timber

class JoinFragment : Fragment(R.layout.fragment_join) {

    private val viewModel: FieldTestViewModel by activityViewModels()
    private lateinit var experimentSettingsProvider: ExperimentSettingsProvider

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        experimentSettingsProvider = ExperimentSettingsProvider(requireContext())

        val teamNameEditText = requireView().findViewById<EditText>(R.id.teamNameEditText)
        teamNameEditText.setText(experimentSettingsProvider.teamId)

        val deviceNameEditText = requireView().findViewById<EditText>(R.id.deviceNameEditText)
        deviceNameEditText.setText(experimentSettingsProvider.deviceName)

        teamNameEditText.addTextChangedListener {
            experimentSettingsProvider.teamId = it.toString()
        }

        deviceNameEditText.addTextChangedListener {
            experimentSettingsProvider.deviceName = it.toString()
        }

        val joinExperiment = requireView().findViewById<Button>(R.id.joinExperiment)
        joinExperiment.setOnClickListener {
            viewModel.joinExperiment()
        }

        viewModel
            .resolutionRequiredLiveEvent
            .observe(
                viewLifecycleOwner,
                Observer { event ->
                    Timber.d("startResolutionForResult")
                    try {
                        startIntentSenderForResult(
                            event.apiException.status.resolution?.intentSender,
                            event.requestCode,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Timber.e(event.apiException, "Error calling startResolutionForResult")
                    }
                }
            )

        experimentSettingsProvider.getExperimentIdLiveData()
            .observe(
                viewLifecycleOwner,
                Observer { experimentId ->
                    if (experimentId.isNotEmpty()) {
                        val fragment = MainFragment()
                        val ft = parentFragmentManager.beginTransaction()
                        ft.replace(R.id.container, fragment)
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.commit()
                    }
                }
            )

        viewModel
            .displaySnackbarLiveEvent
            .observe(
                viewLifecycleOwner,
                Observer { errorMessage ->
                    Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show()
                }
            )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onResolutionComplete(requestCode, resultCode)
    }

    /**
     * Called when opt-in resolution is completed by user.
     *
     *
     * Modeled after `Activity#onActivityResult` as that's how the API sends callback to
     * apps.
     */
    private fun onResolutionComplete(requestCode: Int, resultCode: Int) {
        when (requestCode) {
            RequestCodes.REQUEST_CODE_START_EXPOSURE_NOTIFICATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.resolutionResultOk(joinExperiment = true)
                }
            }
            RequestCodes.REQUEST_CODE_GET_TEMP_EXPOSURE_KEY_HISTORY -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.resolutionResultOk(joinExperiment = true)
                }
            }
        }
    }
}
