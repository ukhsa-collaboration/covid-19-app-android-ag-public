package uk.nhs.nhsx.covid19.android.app.report

import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind
import uk.nhs.nhsx.covid19.android.app.report.config.FontScale
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.config.Theme
import uk.nhs.nhsx.covid19.android.app.report.output.Report
import uk.nhs.nhsx.covid19.android.app.report.output.Screenshot
import uk.nhs.nhsx.covid19.android.app.report.output.Step
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.takeScreenshot
import java.io.File

interface Reporter {
    fun step(stepName: String, stepDescription: String)

    enum class Kind(val description: String) {
        FLOW("Flow"), SCREEN("Screen")
    }
}

class AndroidReporter internal constructor(
    private val testAppContext: TestApplicationContext,
    private val scenario: String,
    private val name: String,
    private val description: String,
    private val kind: Kind,
    private val testConfigurations: List<TestConfiguration>
) : Reporter {
    private val screenshotFolderName = "${kind.description} - $scenario - $name"
    private val steps = linkedMapOf<String, Step>()
    private val reportAdapter = Moshi.Builder().build().adapter(Report::class.java).indent("\t")
    private lateinit var currentTestConfiguration: TestConfiguration

    fun runWithConfigurations(builderAction: Reporter.() -> Unit) {
        testConfigurations.forEach { configuration ->
            applyConfiguration(configuration)
            builderAction()
        }
        save()
        resetSystemFontScaling()
    }

    private fun applyConfiguration(testConfiguration: TestConfiguration) {
        UiDevice.getInstance(getInstrumentation()).pressBack()

        this.currentTestConfiguration = testConfiguration
        setScreenOrientation(testConfiguration.orientation)
        adjustFontScale(testConfiguration.fontScale)
        applyTheme(testConfiguration.theme)
        testAppContext.reset()
    }

    private fun setScreenOrientation(orientation: Orientation) {
        val device = UiDevice.getInstance(getInstrumentation())
        when (orientation) {
            Orientation.LANDSCAPE -> device.setOrientationLeft()
            Orientation.PORTRAIT -> device.setOrientationNatural()
        }
    }

    private fun adjustFontScale(fontScale: FontScale) =
        UiDevice.getInstance(getInstrumentation()).executeShellCommand(
            "settings put system font_scale ${fontScale.scale}"
        )

    private fun applyTheme(theme: Theme) {
        UiThreadStatement.runOnUiThread {
            when (theme) {
                Theme.LIGHT -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                Theme.DARK -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
            }
        }
    }

    private fun resetSystemFontScaling() = adjustFontScale(FontScale.DEFAULT)

    override fun step(stepName: String, stepDescription: String) {
        val orientationName = currentTestConfiguration.orientation.exportName
        val fontScaleName = currentTestConfiguration.fontScale.exportName
        val themeName = currentTestConfiguration.theme.exportName
        val filename = "$orientationName-$fontScaleName-$themeName-$stepName"

        val outputFilename = takeScreenshot(filename, screenshotFolderName)
        if (outputFilename != null) {
            val tags = listOf(orientationName, fontScaleName, themeName)
            val screenshot = Screenshot("$screenshotFolderName/$outputFilename", tags)

            createOrUpdateStep(stepName, stepDescription, screenshot)
        }
    }

    private fun createOrUpdateStep(
        stepName: String,
        stepDescription: String,
        screenshot: Screenshot
    ) {
        val savedStep = steps[stepName]
        if (savedStep == null) {
            steps[stepName] = Step(
                stepName,
                stepDescription,
                screenshots = listOf(screenshot)
            )
        } else {
            val updatedScreenshots = mutableListOf<Screenshot>().apply {
                addAll(savedStep.screenshots)
                add(screenshot)
            }
            steps[stepName] = savedStep.copy(screenshots = updatedScreenshots)
        }
    }

    @Suppress("DEPRECATION")
    private fun save() {
        val report = Report(
            description = description,
            kind = kind,
            name = name,
            scenario = scenario,
            steps = steps.values.toList()
        )
        val outputFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "covid19"
        )
        val text = reportAdapter.toJson(report)
        val file = File(outputFolder, "$screenshotFolderName.json")
        file.writeText(text)
        val exists = file.exists()
        Log.d("Screenshots", "Exists = $exists")
    }
}

class DummyReporter : Reporter {
    override fun step(stepName: String, stepDescription: String) {
    }
}

fun EspressoTest.reporter(
    scenario: String,
    title: String,
    description: String,
    kind: Kind,
    builderAction: Reporter.() -> Unit
) {
    val takeScreenshots =
        InstrumentationRegistry.getArguments().getString("takeScreenshots")?.toBoolean()
            ?: false
    if (takeScreenshots) {
        val configurations = listOf(
            TestConfiguration(Orientation.PORTRAIT, FontScale.DEFAULT, Theme.LIGHT),
            TestConfiguration(Orientation.PORTRAIT, FontScale.SMALL, Theme.LIGHT),
            TestConfiguration(Orientation.PORTRAIT, FontScale.LARGEST, Theme.LIGHT),
            TestConfiguration(Orientation.PORTRAIT, FontScale.DEFAULT, Theme.DARK)
        )

        AndroidReporter(
            testAppContext,
            scenario,
            title,
            description,
            kind,
            configurations
        ).runWithConfigurations(
            builderAction
        )
    } else {
        DummyReporter().apply(builderAction)
    }
}

fun notReported(
    action: () -> Unit
) {
    val takeScreenshots =
        InstrumentationRegistry.getArguments().getString("takeScreenshots")?.toBoolean()
            ?: false
    if (!takeScreenshots) {
        action()
    }
}
