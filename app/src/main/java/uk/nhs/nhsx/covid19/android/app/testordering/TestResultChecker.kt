package uk.nhs.nhsx.covid19.android.app.testordering

interface TestResultChecker {
    fun hasTestResultMatching(predicate: (TestResult) -> Boolean): Boolean
}
