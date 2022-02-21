package uk.nhs.nhsx.covid19.android.app.about

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryViewModel.VenueHistoryState
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityVenueHistoryBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setMultilineTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class VenueHistoryActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<VenueHistoryViewModel>

    private val viewModel: VenueHistoryViewModel by viewModels { factory }

    private lateinit var binding: ActivityVenueHistoryBinding

    private lateinit var venueVisitsViewAdapter: VenueVisitsViewAdapter

    private var editButton: MenuItem? = null

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityVenueHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.title_venue_history,
                upIndicator = R.drawable.ic_arrow_back_white
            )

            setupToolbarPadding()

            venueHistoryList.addItemDecoration(
                DividerItemDecoration(
                    this@VenueHistoryActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        setupViewModelListeners()
    }

    private fun setupToolbarPadding() = with(binding.primaryToolbar.toolbar) {
        setPaddingRelative(
            paddingStart,
            paddingTop,
            16.dpToPx.toInt(),
            paddingBottom
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        editButton = menu?.findItem(R.id.menuEditAction)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menuEditAction -> {
            viewModel.onEditVenueVisitClicked()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (binding.venueHistoryList.isVisible) {
            editButton?.visible()
        } else {
            editButton?.gone()
        }
        return true
    }

    private fun setupViewModelListeners() {
        viewModel.venueHistoryState().observe(this) {
            renderViewState(it)
        }

        viewModel.venueVisitsEditModeChanged().observe(this) { isInEditMode ->
            onVenueVisitsEditModeChanged(isInEditMode)
        }
    }

    private fun renderViewState(viewState: VenueHistoryState) {
        updateVenueVisitsContainer(viewState.venueVisitEntries, viewState.isInEditMode)

        if (viewState.confirmDeleteVenueVisit != null) {
            showDeleteVenueVisitConfirmationDialog(viewState.confirmDeleteVenueVisit.venueVisit)
        }
    }

    private fun updateVenueVisitsContainer(venueVisitEntries: List<VenueVisitListItem>, isInEditMode: Boolean) =
        with(binding) {
            if (venueVisitEntries.isNullOrEmpty()) {
                editButton?.gone()
                venueHistoryList.gone()
                venueHistoryEmpty.visible()
            } else {
                editButton?.visible()
                venueHistoryList.visible()
                venueHistoryEmpty.gone()

                setUpVenueVisitsAdapter(venueVisitEntries, isInEditMode)

                editButton?.title = getEditButtonText(isInEditMode)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    editButton?.contentDescription = getEditVenueVisitsContentDescription(isInEditMode)
                }
            }
        }

    private fun onVenueVisitsEditModeChanged(isInEditMode: Boolean) {
        val announcement = getEditVenueVisitsContentDescription(isInEditMode)
        editButton?.actionView?.announceForAccessibility(announcement)
    }

    private fun getEditButtonText(isInEditMode: Boolean): String =
        if (isInEditMode) getString(R.string.done_button_text)
        else getString(R.string.edit)

    private fun getEditVenueVisitsContentDescription(isInEditMode: Boolean): String =
        if (isInEditMode) getString(R.string.venue_history_editing_done)
        else getString(R.string.venue_history_edit)

    private fun setUpVenueVisitsAdapter(
        venueVisitEntries: List<VenueVisitListItem>,
        showDeleteIcon: Boolean
    ) = with(binding) {
        venueVisitsViewAdapter = VenueVisitsViewAdapter(venueVisitEntries, showDeleteIcon) { venueVisit ->
            viewModel.onDeleteVenueVisitDataClicked(venueVisit)
        }
        venueHistoryList.layoutManager = LinearLayoutManager(this@VenueHistoryActivity)
        venueHistoryList.adapter = venueVisitsViewAdapter
    }

    private fun showDeleteVenueVisitConfirmationDialog(venueVisit: VenueVisit) {
        val builder = AlertDialog.Builder(this)
        builder.setMultilineTitle(getString(R.string.delete_single_venue_visit_title))
        builder.setMessage(R.string.delete_single_venue_visit_text)
        builder.setPositiveButton(
            R.string.confirm
        ) { dialog, _ ->
            viewModel.deleteVenueVisit(venueVisit)
            dialog.dismiss()
        }

        builder.setNegativeButton(
            R.string.cancel
        ) { dialog, _ ->
            dialog.dismiss()
        }

        builder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = builder.show()
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, VenueHistoryActivity::class.java)
    }
}

private fun MenuItem.visible() {
    isVisible = true
}

private fun MenuItem.gone() {
    isVisible = false
}
