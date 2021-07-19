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
    private val configuration: TestConfiguration,
    private val scenario: String,
    private val name: String,
    private val description: String,
    private val kind: Kind,
    private val testAppContext: TestApplicationContext
) : Reporter {
    private val screenshotFolderName = "${kind.description} - $scenario - $name"
    private val steps = mutableListOf<Step>()
    private val reportAdapter = Moshi.Builder().build().adapter(Report::class.java).indent("\t")

    fun runWithConfigurations(builderAction: Reporter.() -> Unit) {
        Log.d("Reporter", "Start run for scenario: $scenario $name")

        Log.d("Reporter", "Use configuration: $configuration for scenario: $scenario $name")
        applyConfiguration(configuration)
        try {
            builderAction()
        } catch (exception: Exception) {
            Log.e(
                "Reporter",
                "Configuration failed: $configuration for scenario: $scenario $name",
                exception
            )
            throw exception
        }

        save()
        resetSystemFontScaling()
        testAppContext.reset()
    }

    private fun applyConfiguration(testConfiguration: TestConfiguration) {
        UiDevice.getInstance(getInstrumentation()).pressBack()

        adjustFontScale(testConfiguration.fontScale)
        applyTheme(testConfiguration.theme)
        testAppContext.setLocale(testConfiguration.languageCode)
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
        val orientationName = configuration.orientation.exportName
        val fontScaleName = configuration.fontScale.exportName
        val themeName = configuration.theme.exportName
        val languageCode = configuration.languageCode
        val filename = "$orientationName-$fontScaleName-$themeName-$stepName-$languageCode"

        val outputFilename = takeScreenshot(filename, screenshotFolderName)
        if (outputFilename != null) {
            val tags = listOf(orientationName, fontScaleName, themeName, languageCode)
            val screenshot = Screenshot("$screenshotFolderName/$outputFilename", tags)

            createStep(stepName, stepDescription, screenshot)
        }
    }

    private fun createStep(
        stepName: String,
        stepDescription: String,
        screenshot: Screenshot
    ) {
        steps.add(
            Step(
                stepName,
                stepDescription,
                screenshots = mutableListOf(screenshot)
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun save() {
        val outputFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "covid19"
        )
        val file = File(outputFolder, "$screenshotFolderName.json")
        val report = if (file.exists()) {
            val existingReport = reportAdapter.fromJson(file.readText())!!
            steps.forEach { step ->
                val existingStep = existingReport.steps.first { it.name == step.name }
                existingStep.screenshots.addAll(step.screenshots)
            }
            existingReport
        } else {
            Report(
                description = description,
                kind = kind.description,
                name = name,
                scenario = scenario,
                steps = steps
            )
        }

        val text = reportAdapter.toJson(report)
        file.writeText(text)
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
    if (isRunningReporterTool()) {
        AndroidReporter(
            configuration!!,
            scenario,
            title,
            description,
            kind,
            testAppContext
        ).runWithConfigurations(
            builderAction
        )
    } else {
        DummyReporter().apply(builderAction)
    }
}

fun isRunningReporterTool() =
    InstrumentationRegistry.getArguments().getString("takeScreenshots")?.toBoolean() ?: false
