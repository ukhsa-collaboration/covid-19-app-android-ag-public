package uk.nhs.nhsx.covid19.android.app.util

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class DoReToAwareAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun onCreate(bundle: Bundle) {
        bundle.putString(FILTER_BUNDLE_KEY, ReportedTestsFilter::class.java.name)
        super.onCreate(bundle)
    }

    companion object {
        // androidx.test.internal.runner.RunnerArgs looks for this bundle key
        private const val FILTER_BUNDLE_KEY = "filter"
    }
}
