package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun takeScreenshot(
    screenshotName: String,
    parentFolderPath: String = "",
    isDialog: Boolean = false,
    failure: Boolean = false
): String? {
    Log.d("Screenshots", "Taking screenshot of '$screenshotName' for `$parentFolderPath`")
    getCurrentActivity()?.let { activity ->
        val rootFolderName = if (failure) "covid19/failures" else "covid19"
        return if (isDialog) {
            takeRegularScreenshot(screenshotName, parentFolderPath, rootFolderName)
        } else {
            takeFullLengthScreenshot(activity, screenshotName, parentFolderPath, rootFolderName)
        }
    }
    return "Activity not found"
}

private fun takeRegularScreenshot(
    screenshotName: String,
    parentFolderPath: String,
    rootFolderName: String
): String? {
    val screenCapture = Screenshot.capture()
    val processor = ScreenCaptureProcessorWithSubfolderSupport(parentFolderPath, rootFolderName)
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

private fun takeFullLengthScreenshot(
    activity: Activity,
    screenshotName: String,
    parentFolderPath: String,
    rootFolderName: String
): String? {
    val screenshotNameWithExt = "$screenshotName.png"
    return runBlocking {
        withContext(context = Dispatchers.Main) {
            val bitmap = activity.window.decorView.rootView?.getScreenBitmap()
            bitmap?.let {
                Log.d("Screenshots", "Saving to file $screenshotNameWithExt")
                saveBitmap(bitmap, parentFolderPath, rootFolderName, screenshotNameWithExt)
            }
        }
    }
}

private fun saveBitmap(
    bitmap: Bitmap,
    parentFolderPath: String,
    rootFolderName: String,
    screenshotName: String
): String? {
    val imageFolder = File(
        File(
            getExternalStoragePublicDirectory(DIRECTORY_PICTURES),
            rootFolderName
        ).absolutePath,
        parentFolderPath
    )
    imageFolder.mkdirs()
    val fos: FileOutputStream
    try {
        val file = File(imageFolder, screenshotName)
        fos = FileOutputStream(file)
        bitmap.compress(PNG, 100, fos)
        fos.flush()
        fos.close()
        Log.d("Screenshots", "Saved file ${file.absolutePath}")
        return screenshotName
    } catch (e: FileNotFoundException) {
        Log.e("Screenshots", e.message, e)
    } catch (e: IOException) {
        Log.e("Screenshots", e.localizedMessage, e)
    }
    return null
}

private fun View.getScreenBitmap(): Bitmap? {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    val originalWidth = this.width
    val originalHeight = this.height
    val originalMeasuredWidth = this.measuredWidth
    val originalMeasuredHeight = this.measuredHeight

    this.measure(
        MeasureSpec.makeMeasureSpec(device.displayWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    )
    val height = Math.max(originalHeight, this.measuredHeight)
    this.layout(0, 0, this.measuredWidth, height)
    val b = Bitmap.createBitmap(this.measuredWidth, height, ARGB_8888)
    val c = Canvas(b)

    this.draw(c)

    this.measure(
        MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(originalHeight, MeasureSpec.EXACTLY)
    )
    this.layout(0, 0, originalMeasuredWidth, originalMeasuredHeight)
    return b
}

class ScreenCaptureProcessorWithSubfolderSupport(
    parentFolderPath: String,
    folderName: String
) :
    BasicScreenCaptureProcessor() {
    init {
        val file = File(
            File(
                getExternalStoragePublicDirectory(DIRECTORY_PICTURES),
                folderName
            ).absolutePath,
            parentFolderPath
        )
        this.mDefaultScreenshotPath = file.absoluteFile
    }

    override fun getFilename(prefix: String): String = prefix
}
