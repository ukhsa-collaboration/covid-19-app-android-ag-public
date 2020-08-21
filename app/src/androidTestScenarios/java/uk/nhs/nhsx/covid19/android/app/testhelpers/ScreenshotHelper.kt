package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import java.io.File
import java.io.IOException

fun takeScreenshot(screenshotName: String, parentFolderPath: String = ""): String? {
    Log.d("Screenshots", "Taking screenshot of '$screenshotName'")
    getCurrentActivity()?.let {
        val screenCapture = Screenshot.capture(it)
        val processor = ScreenCaptureProcessorWithSubfolderSupport(parentFolderPath)
        return try {
            screenCapture.name = screenshotName
            val filename = processor.process(screenCapture)
            Log.d("Screenshots", "uk.nhs.nhsx.covid19.android.app.report.output.Screenshot taken")
            filename
        } catch (ex: IOException) {
            Log.e("Screenshots", "Could not take the screenshot", ex)
            null
        }
    }
    return "Activity not found"
}

class ScreenCaptureProcessorWithSubfolderSupport(parentFolderPath: String) :
    BasicScreenCaptureProcessor() {
    init {
        this.mDefaultScreenshotPath = File(
            File(
                getExternalStoragePublicDirectory(DIRECTORY_PICTURES),
                "covid19"
            ).absolutePath,
            parentFolderPath
        )
    }

    override fun getFilename(prefix: String): String = prefix
}
