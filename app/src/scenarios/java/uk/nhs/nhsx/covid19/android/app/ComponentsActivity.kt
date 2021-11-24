package uk.nhs.nhsx.covid19.android.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryActivity
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityComponentTestBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class ComponentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComponentTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityComponentTestBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)
            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.component_test_screen_title,
                upIndicator = R.drawable.ic_close_white
            )

            hasIdAsWell.findViewById<MaterialButton>(R.id.button1).setOnSingleClickListener {
                startActivity<VenueHistoryActivity>()
            }

            with(hasIdAsWell.findViewById<MaterialButton>(R.id.button2)) {
                setOnSingleClickListener {
                    Snackbar.make(this, "Test snackbar message", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
