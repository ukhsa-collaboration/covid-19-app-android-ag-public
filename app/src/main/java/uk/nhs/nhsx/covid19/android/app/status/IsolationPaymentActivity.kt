package uk.nhs.nhsx.covid19.android.app.status

import android.os.Bundle
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar

class IsolationPaymentActivity : BaseActivity(R.layout.activity_isolation_payment) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCloseToolbar(toolbar, R.string.isolation_payment)
    }
}
