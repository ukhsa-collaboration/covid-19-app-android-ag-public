package uk.nhs.nhsx.covid19.android.app.exposure.encounter

interface RandomNonRiskyExposureWindowsLimiter {

    fun isAllowed(): Boolean
}
