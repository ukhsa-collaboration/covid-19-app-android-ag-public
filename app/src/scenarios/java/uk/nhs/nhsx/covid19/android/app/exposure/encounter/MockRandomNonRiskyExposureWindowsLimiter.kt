package uk.nhs.nhsx.covid19.android.app.exposure.encounter

class MockRandomNonRiskyExposureWindowsLimiter : RandomNonRiskyExposureWindowsLimiter {

    override fun isAllowed(): Boolean = true
}
