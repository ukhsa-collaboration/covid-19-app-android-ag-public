package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_edit_postal_district.postCodeView
import kotlinx.android.synthetic.main.activity_edit_postal_district.savePostCode
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.INVALID_POST_DISTRICT
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.POST_DISTRICT_NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS
import uk.nhs.nhsx.covid19.android.app.util.setNavigateUpToolbar
import javax.inject.Inject

class EditPostalDistrictActivity : BaseActivity(R.layout.activity_edit_postal_district) {

    @Inject
    lateinit var factory: ViewModelFactory<EditPostalDistrictViewModel>

    private val viewModel: EditPostalDistrictViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar as MaterialToolbar,
            R.string.edit_postcode_district_title,
            R.drawable.ic_arrow_back_white
        )

        savePostCode.setOnClickListener {
            viewModel.updatePostCode(postCodeView.postCodeDistrict)
        }

        viewModel.viewState().observe(this) { postCodeViewState ->
            when (postCodeViewState) {
                SUCCESS -> finish()
                INVALID_POST_DISTRICT -> postCodeView.showErrorState()
                POST_DISTRICT_NOT_SUPPORTED -> postCodeView.showPostCodeNotSupportedErrorState()
            }
        }
    }

    companion object {
        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, EditPostalDistrictActivity::class.java)
    }
}
