package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileHelper @Inject constructor(
    private val applicationContext: Context
) {
    fun provideFile(inputStream: InputStream): File {
        val chunksDirPath = createDirPath()
        val filePath =
            listOf(chunksDirPath, "${System.currentTimeMillis()}.zip").joinToString(File.separator)
        inputStream.saveToFile(filePath)

        return File(filePath)
    }

    private fun InputStream.saveToFile(file: String) = use { input ->
        File(file).outputStream().use { output ->
            input.copyTo(output)
        }
    }

    private fun createDirPath(): String {
        val chunksDirPath = listOf(
            applicationContext.filesDir,
            "chunks"
        ).joinToString(File.separator)
        val chunksDir = File(chunksDirPath)

        chunksDir.apply {
            deleteRecursively()
            mkdir()
        }

        return chunksDirPath
    }
}
