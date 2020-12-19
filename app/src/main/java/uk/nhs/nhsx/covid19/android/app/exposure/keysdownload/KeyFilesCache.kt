package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import android.content.Context
import okhttp3.ResponseBody
import uk.nhs.nhsx.covid19.android.app.util.lastDateFormatter
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

class KeyFilesCache @Inject constructor(
    private val applicationContext: Context,
    private val clock: Clock
) {

    fun createFile(timestamp: String, responseBody: ResponseBody): File {
        val filePath = getFilePath(timestamp)
        responseBody.byteStream().saveToFile(filePath)

        return File(filePath)
    }

    fun getFile(timestamp: String): File? {
        val filePath = getFilePath(timestamp)

        val file = File(filePath)
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    private fun getFilePath(timestamp: String): String {
        val cacheDirPath = createDirPath()
        return listOf(cacheDirPath, "$timestamp.zip").joinToString(File.separator)
    }

    private fun InputStream.saveToFile(file: String) = use { input ->
        File(file).outputStream().use { output ->
            input.copyTo(output)
        }
    }

    fun clearOutdatedFiles() {
        val threshold = LocalDateTime.now(clock).minusDays(1)

        val cacheDir = File(createDirPath())

        cacheDir.list()?.toList()?.forEach {
            runCatching {
                val timestamp = it.substring(0, it.indexOf('.'))
                val test = LocalDateTime.from(lastDateFormatter.parse(timestamp))
                if (test.isBefore(threshold)) File(cacheDir, it).delete()
            }
        }
    }

    private fun createDirPath(): String {
        val cacheDirPath = listOf(
            applicationContext.filesDir,
            "keys_cache"
        ).joinToString(File.separator)

        File(cacheDirPath).mkdir()

        return cacheDirPath
    }
}
