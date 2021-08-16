package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.HasTestAppContext

interface LocalAuthoritySetupHelper : HasTestAppContext {
    fun givenLocalAuthorityIsInEngland() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
    }

    fun givenLocalAuthorityIsInWales() {
        testAppContext.setLocalAuthority(TestApplicationContext.WELSH_LOCAL_AUTHORITY)
    }
}
