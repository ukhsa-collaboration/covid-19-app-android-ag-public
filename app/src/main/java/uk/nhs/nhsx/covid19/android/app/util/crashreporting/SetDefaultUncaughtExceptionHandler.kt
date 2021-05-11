package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import java.lang.Thread.UncaughtExceptionHandler
import javax.inject.Inject

class SetDefaultUncaughtExceptionHandler @Inject constructor() {
    operator fun invoke(uncaughtExceptionHandler: UncaughtExceptionHandler) {
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
    }
}
