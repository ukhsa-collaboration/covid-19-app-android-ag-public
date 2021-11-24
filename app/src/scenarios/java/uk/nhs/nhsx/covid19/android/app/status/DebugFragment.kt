package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkManager
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.PERIODIC_TASKS
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.FragmentDebugBinding
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.ExportToFileResult.Error
import uk.nhs.nhsx.covid19.android.app.status.ExportToFileResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.status.ExportToFileResult.Success
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.widgets.OffsetDaysView.OnOffsetDaysChangedListener
import java.io.File
import javax.inject.Inject

class DebugFragment : Fragment() {

    @Inject
    lateinit var debugViewModelFactory: ViewModelFactory<DebugViewModel>

    @Inject
    lateinit var exposureNotificationApi: ExposureNotificationApi

    private val debugViewModel: DebugViewModel by viewModels { debugViewModelFactory }

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showDebugOptions()

        debugViewModel.exposureKeysResult.observe(
            this,
            {
                when (it) {
                    is ResolutionRequired -> {
                        startIntentSenderForResult(
                            it.status.resolution?.intentSender,
                            REQUEST_CODE_KEY_HISTORY,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    }
                    is Success -> {
                        share(it.file)
                    }
                    is Error -> Toast.makeText(
                        requireContext(),
                        it.exception.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        observeDownloadTaskStatus()
    }

    private fun showDebugOptions() = with(binding) {
        offsetDaysView.setListener(object : OnOffsetDaysChangedListener {
            override fun offsetChanged(offsetDays: Long) {
                debugViewModel.onOffsetDaysChanged(offsetDays)
            }
        })

        offsetDaysView.setDateChangeReceiver(debugViewModel.getDateChangeReceiver())

        submitKeys.setOnSingleClickListener {
            activity?.startActivity<ShareKeysInformationActivity> {}
        }

        exportKeys.setOnSingleClickListener {
            debugViewModel.exportKeys(requireContext())
        }

        importKeys.setOnSingleClickListener {
            val keysIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            keysIntent.type = "*/*"
            startActivityForResult(keysIntent, IMPORT_KEYS)
        }

        startDownloadTask.setOnSingleClickListener {
            debugViewModel.startDownloadTask()
        }

        sendPositiveTestResult.setOnSingleClickListener {
            debugViewModel.sendPositiveConfirmedTestResult(requireContext())
        }

        sendNegativeTestResult.setOnSingleClickListener {
            debugViewModel.sendNegativeConfirmedTestResult(requireContext())
        }

        sendVoidTestResult.setOnSingleClickListener {
            debugViewModel.sendVoidConfirmedTestResult(requireContext())
        }

        sendPlodTestResult.setOnSingleClickListener {
            debugViewModel.sendPlodConfirmedTestResult(requireContext())
        }

        sendUnconfirmedPositiveTestResult.setOnSingleClickListener {
            debugViewModel.sendPositiveUnconfirmedTestResult()
        }

        sendUnknownTestResult.setOnSingleClickListener {
            debugViewModel.sendUnknownTestResult()
        }

        defaultState.setOnSingleClickListener {
            debugViewModel.setDefaultState()
        }

        indexState.setOnSingleClickListener {
            debugViewModel.setIndexState()
        }

        contactState.setOnSingleClickListener {
            debugViewModel.setContactState(contactDayOffset.text.toString().toLong())
        }

        sendExposureNotification.setOnSingleClickListener {
            if (exposureNotificationApi is MockExposureNotificationApi) {
                debugViewModel.sendExposureNotification(exposureDayOffset.text.toString().toLong())
            } else {
                Toast.makeText(context, "Use Mock Exposure Notifications", Toast.LENGTH_LONG).show()
            }
        }

        riskyVenueM1.setOnSingleClickListener {
            debugViewModel.setRiskyVenue(INFORM)
        }

        riskyVenueM2.setOnSingleClickListener {
            debugViewModel.setRiskyVenue(BOOK_TEST)
        }

        riskyPostCode.setOnSingleClickListener {
            debugViewModel.setRiskyPostCode()
        }

        submitAnalyticsUsingAlarmManager.setOnSingleClickListener {
            debugViewModel.submitAnalyticsUsingAlarmManager()
        }
    }

    private fun observeDownloadTaskStatus() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(PERIODIC_TASKS.workName)
            .observe(viewLifecycleOwner) {
                val startDownloadTask = binding.startDownloadTask
                it?.let { workInfos ->
                    if (workInfos.any { workInfo -> workInfo.state == ENQUEUED }) {
                        startDownloadTask.isEnabled = true
                    }
                    if (workInfos.any { workInfo -> workInfo.state == RUNNING }) {
                        startDownloadTask.isEnabled = false
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_KEYS && resultCode == RESULT_OK && data != null) {
            val uri = data.data!!
            val file = saveToFile(requireContext(), uri)
            Timber.d("File exists: ${file.exists()} ${file.length()}")
            debugViewModel.importKeys(file)
        } else if (requestCode == REQUEST_CODE_KEY_HISTORY && resultCode == RESULT_OK) {
            debugViewModel.exportKeys(requireContext())
        }
    }

    private fun saveToFile(context: Context, uri: Uri): File {
        context.openFileOutput("temp.zip", Context.MODE_PRIVATE).use { fileOutput ->
            context.contentResolver.openInputStream(uri).use { inputStream ->
                inputStream!!.copyTo(fileOutput)
            }
        }
        return File(context.filesDir, "temp.zip")
    }

    private fun share(file: File) {
        val fileUri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "uk.nhs.covid19.scenarios.fileprovider",
            file
        )
        val intent = IntentBuilder.from(requireActivity())
            .setStream(fileUri) // uri from FileProvider
            .setType("application/zip")
            .intent
            .setAction(Intent.ACTION_SEND) // Change if needed
            .setDataAndType(fileUri, "application/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        requireContext().startActivity(intent)
    }

    companion object {
        const val IMPORT_KEYS = 100
        const val REQUEST_CODE_KEY_HISTORY = 1337
    }
}
