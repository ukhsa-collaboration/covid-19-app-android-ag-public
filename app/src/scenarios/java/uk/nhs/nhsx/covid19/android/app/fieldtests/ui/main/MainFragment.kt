package uk.nhs.nhsx.covid19.android.app.fieldtests.ui.main

import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.fieldtests.ui.participants.ParticipantListActivity
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.RequestCodes
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.fieldtests.storage.ExperimentSettingsProvider

class MainFragment : Fragment(R.layout.fragment_main) {

    private val viewModel: FieldTestViewModel by activityViewModels()
    private lateinit var experimentSettingsProvider: ExperimentSettingsProvider

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        experimentSettingsProvider =
            ExperimentSettingsProvider(
                requireContext()
            )

        applicationVersionTextView().text =
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        googlePlayServiceVersionTextView().text =
            getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)

        teamNameTextView().text = experimentSettingsProvider.teamId
        deviceNameTextView().text = experimentSettingsProvider.deviceName

        toggleExposureNotificationsCta().setOnClickListener {
            viewModel.toggleExposureNotifications()
        }

        createNewExperimentCta().setOnClickListener {
            val googlePlayServicesVersion =
                getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
            viewModel.createNewExperiment(googlePlayServicesVersion)
        }

        seeExperimentDetailsCta().setOnClickListener {
            viewModel.seeExperimentDetails()
        }

        val processAndPostResultsButton =
            requireView().findViewById<Button>(R.id.processAndPostResultsButton)
        processAndPostResultsButton.setOnClickListener {
            viewModel.processAndPostResults()
        }

        setupViewModelListeners()
    }

    private fun seeExperimentDetailsCta() =
        requireView().findViewById<Button>(R.id.seeExperimentDetailsCta)

    private fun createNewExperimentCta() =
        requireView().findViewById<Button>(R.id.createNewExperimentCta)

    private fun deviceNameTextView() =
        requireView().findViewById<TextView>(R.id.deviceNameTextView)

    private fun teamNameTextView() =
        requireView().findViewById<TextView>(R.id.teamNameTextView)

    private fun googlePlayServiceVersionTextView(): TextView =
        requireView().findViewById(R.id.googlePlayServiceVersionTextView)

    private fun applicationVersionTextView(): TextView =
        requireView().findViewById(R.id.applicationVersionTextView)

    private fun toggleExposureNotificationsCta(): Button =
        requireView().findViewById(R.id.toggleExposureNotifications)

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupViewModelListeners() {
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
                    } catch (e: SendIntentException) {
                        Timber.e(event.apiException, "Error calling startResolutionForResult")
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

        viewModel
            .hasAccessToKeyHistoryData
            .observe(
                viewLifecycleOwner,
                Observer { hasGrantedAccess ->
                    val toggleExposureNotifications = toggleExposureNotificationsCta()
                    if (hasGrantedAccess) {
                        toggleExposureNotifications.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        toggleExposureNotifications.text = "Stop Exposure Notifications"
                    } else {
                        toggleExposureNotifications.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_green_dark
                            )
                        )
                        toggleExposureNotifications.text = "Start Exposure Notifications"
                    }
                    val actionsContainer =
                        requireView().findViewById<LinearLayout>(R.id.actionsContainer)
                    actionsContainer.isVisible = hasGrantedAccess
                }
            )

        viewModel
            .displayParticipantsLiveEvent
            .observe(
                viewLifecycleOwner,
                Observer { participants ->
                    startActivity(
                        ParticipantListActivity.newInstance(
                            requireActivity(),
                            participants
                        )
                    )
                }
            )

        experimentSettingsProvider.getExperimentIdLiveData()
            .observe(
                viewLifecycleOwner,
                Observer { experimentId ->
                    val experimentName = experimentSettingsProvider.experimentName
                    val experimentIdTextView =
                        requireView().findViewById<TextView>(R.id.experimentIdTextView)
                    experimentIdTextView.text = "$experimentName ($experimentId)"
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
                    viewModel.resolutionResultOk()
                }
            }
            RequestCodes.REQUEST_CODE_GET_TEMP_EXPOSURE_KEY_HISTORY -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.resolutionResultOk()
                }
            }
        }
    }

    private fun getVersionNameForPackage(packageName: String): String {
        try {
            return requireContext().packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
        }
        return "Not available"
    }

    companion object {
        val TAG = MainFragment::class.simpleName
        fun newInstance() = MainFragment()
    }
}
