package uk.nhs.nhsx.covid19.android.app.util

import timber.log.Timber
import java.io.File

object FileFactory {
    fun createFile(parent: File, child: String): File = File(parent, child)
}

fun File.tryDelete() {
    try {
        delete()
    } catch (exception: Exception) {
        Timber.d(exception, "Can't delete file: $this")
    }
}
