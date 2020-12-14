package uk.nhs.nhsx.covid19.android.app.util

import android.os.Environment
import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import radiography.Radiography
import uk.nhs.nhsx.covid19.android.app.testhelpers.takeScreenshot
import java.io.File

@Suppress("DEPRECATION")
class ScreenshotTakingRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        takeScreenshot(
            parentFolderPath = description.className,
            screenshotName = description.methodName,
            failure = true
        )
        reportViewHierarchy(description)
    }

    private fun reportViewHierarchy(description: Description) {
        val viewHierarchy = Radiography.scan()

        try {
            val outputFolder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "covid19/failures/${description.className}"
            )
            val file = File(outputFolder, "${description.methodName}.txt")

            file.writeText(viewHierarchy)
        } catch (e: Exception) {
            Log.e("ScreenshotTakingRule", "Unable to store view hierarchy", e)
        }
    }
}
