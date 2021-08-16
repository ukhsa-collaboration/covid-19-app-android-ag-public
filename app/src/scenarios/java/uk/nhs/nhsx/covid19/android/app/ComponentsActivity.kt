package uk.nhs.nhsx.covid19.android.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import kotlinx.android.synthetic.scenarios.include_test_complex_components.button1
import kotlinx.android.synthetic.scenarios.include_test_complex_components.button2
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class ComponentsActivity : AppCompatActivity(R.layout.activity_component_test) {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setNavigateUpToolbar(toolbar, R.string.component_test_screen_title, upIndicator = R.drawable.ic_close_white)

        button1.setOnSingleClickListener {
            startActivity<VenueHistoryActivity>()
        }

        button2.setOnSingleClickListener {
            Snackbar.make(button2, "Test snackbar message", Snackbar.LENGTH_SHORT).show()
        }
    }
}
