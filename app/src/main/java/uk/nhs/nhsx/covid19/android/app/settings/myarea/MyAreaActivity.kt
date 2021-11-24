package uk.nhs.nhsx.covid19.android.app.settings.myarea

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityMyAreaBinding
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import javax.inject.Inject

class MyAreaActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<MyAreaViewModel>
    private val viewModel: MyAreaViewModel by viewModels { factory }

    private lateinit var binding: ActivityMyAreaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityMyAreaBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)

            with(primaryToolbar.toolbar) {

                setNavigateUpToolbar(
                    this,
                    R.string.settings_my_area_title,
                    upIndicator = R.drawable.ic_arrow_back_white
                )

                setPaddingRelative(
                    paddingStart,
                    paddingTop,
                    12.dpToPx.toInt(),
                    paddingBottom
                )
            }
        }

        viewModel.viewState.observe(this) {
            renderViewState(it)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun renderViewState(viewState: ViewState) = with(binding) {
        if (viewState.postCode != null) {
            postCodeDistrictOption.subtitle = viewState.postCode
        }
        if (viewState.localAuthority != null) {
            localAuthorityOption.subtitle = viewState.localAuthority
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuEditAction -> {
                EditPostalDistrictActivity.start(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
