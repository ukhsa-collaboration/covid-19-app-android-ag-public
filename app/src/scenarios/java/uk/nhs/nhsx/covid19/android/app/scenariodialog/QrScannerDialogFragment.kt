package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.widget.AppCompatSpinner
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.addVenue
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.autoLoop
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.deleteSelected
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.qrScanVenueId
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.qrScanVenueName
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.qrScanVenuePostcode
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.scanList
import kotlinx.android.synthetic.scenarios.dialog_qr_scanner.view.uploadCsv
import uk.nhs.nhsx.covid19.android.app.DebugActivity
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockQrScannerViewModel
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.io.BufferedInputStream

class QrScannerDialogFragment(positiveAction: (() -> Unit), dismissAction: (() -> Unit)) :
    ScenarioDialogFragment(positiveAction, dismissAction) {
    override val title: String = "Qr Scanner Config"
    override val layoutId = R.layout.dialog_qr_scanner

    override fun setUp(view: View): Unit = with(view) {
        addVenue.setOnSingleClickListener {
            if (qrScanVenueName.text.toString().isNotBlank() &&
                qrScanVenuePostcode.text.toString().isNotBlank() &&
                qrScanVenueId.text.toString().isNotBlank()
            ) {
                val venue = Venue(
                    id = qrScanVenueId.text.toString(),
                    organizationPartName = qrScanVenueName.text.toString(),
                    postCode = qrScanVenuePostcode.text.toString()
                )
                MockQrScannerViewModel.currentOptions.venueList.add(venue)

                qrScanVenueId.setText("")
                qrScanVenueName.setText("")
                qrScanVenuePostcode.setText("")

                scanList.updateAdapter()
            } else {
                Snackbar.make(qrScanVenueId, "One required field is invalid", Snackbar.LENGTH_SHORT).show()
            }
        }
        autoLoop.setOnCheckedChangeListener { _, isChecked ->
            MockQrScannerViewModel.currentOptions = MockQrScannerViewModel.currentOptions.copy(
                loop = isChecked
            )
        }

        deleteSelected.setOnSingleClickListener {
            if (MockQrScannerViewModel.currentOptions.venueList.isEmpty()) {
                Snackbar.make(deleteSelected, "Nothing has been deleted!", Snackbar.LENGTH_SHORT).show()
            } else {
                MockQrScannerViewModel.currentOptions.venueList.removeAt(scanList.selectedItemPosition)
                scanList.updateAdapter()
            }
        }

        uploadCsv.setOnSingleClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            try {
                requireActivity().startActivityForResult(
                    Intent.createChooser(intent, "Select a file"),
                    DebugActivity.FILE_SELECT_CODE
                )
            } catch (ex: ActivityNotFoundException) {
                Snackbar.make(
                    this, "Please install a file manager.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        scanList.updateAdapter()
    }

    private fun AppCompatSpinner.updateAdapter() {
        val stringList =
            MockQrScannerViewModel.currentOptions.venueList.map { "${it.organizationPartName}, ${it.postCode} [${it.id}]" }
        adapter = SimpleSpinnerAdapter(
            context,
            stringList
        )
        this.setSelection(stringList.size - 1)
    }

    private fun processCsvLine(line: List<String>) {
        require(line.size == 3) { "File not in the correct format" }
        if (line.containsAll(listOf("id", "name", "postcode"))) {
            return
        }
        val venue = Venue(line[0], line[1], line[2])
        MockQrScannerViewModel.currentOptions.venueList.add(venue)
    }

    fun processCsv(path: Uri) {
        with(requireActivity().contentResolver.openInputStream(path)!!) {
            BufferedInputStream(this).bufferedReader().forEachLine {
                processCsvLine(it.split(","))
            }
            close()
        }
        inflatedView?.scanList?.updateAdapter()
    }
}
