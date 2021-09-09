package uk.nhs.nhsx.covid19.android.a11y

import org.junit.Test
import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint

class AccessibilityTitleDetectorTest {
    @Test
    fun `Using label on an Activity does not violate rule`() {
        lint()
            .allowMissingSdk()
            .files(
                xml(
                    "AndroidManifest.xml",
                    """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.test.app">
                            <application>
                                <activity
                                    android:name=".MainActivity"
                                    android:label="@string/stringId">
                                </activity>
                            </application>
                        </manifest>
                    """
                ).indented()
            )
            .issues(AccessibilityTitleDetector.ISSUE_MISSING_ACCESSIBILITY_LABEL)
            .run()
            .expectClean()
    }

    @Test
    fun `Not using label on an Activity violates rule`() {
        lint()
            .allowMissingSdk()
            .files(
                xml(
                    "AndroidManifest.xml",
                    """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.test.app">
                            <application>
                                <activity android:name=".MainActivity"/>
                            </application>
                        </manifest>
                    """
                ).indented()
            )
            .issues(AccessibilityTitleDetector.ISSUE_MISSING_ACCESSIBILITY_LABEL)
            .run()
            .expect("""
                |AndroidManifest.xml:3: Error: Ensure accessibility title is announced using non-empty android:label='' attribute [AccessibilityLabel]
                |        <activity android:name=".MainActivity"/>
                |                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                |1 errors, 0 warnings
            """.trimMargin())
    }

    @Test
    fun `Using empty label on an Activity violates rule`() {
        lint()
            .allowMissingSdk()
            .files(
                xml(
                    "AndroidManifest.xml",
                    """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.test.app">
                            <application>
                                <activity
                                    android:name=".MainActivity"
                                    android:label="">
                                </activity>
                            </application>
                        </manifest>
                    """
                ).indented()
            )
            .issues(AccessibilityTitleDetector.ISSUE_MISSING_ACCESSIBILITY_LABEL)
            .run()
            .expect("""
                |AndroidManifest.xml:4: Error: Ensure accessibility title is announced using non-empty android:label='' attribute [AccessibilityLabel]
                |            android:name=".MainActivity"
                |            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                |1 errors, 0 warnings
            """.trimMargin())
    }

    @Test
    fun `Can use null as label when activity sets the title`() {
        lint()
            .allowMissingSdk()
            .files(
                xml(
                    "AndroidManifest.xml",
                    """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.test.app">
                            <application>
                                <activity
                                    android:name=".MainActivity"
                                    android:label="@string/empty_accessibility_announcement">
                                </activity>
                            </application>
                        </manifest>
                    """
                ).indented()
            )
            .issues(AccessibilityTitleDetector.ISSUE_MISSING_ACCESSIBILITY_LABEL)
            .run()
            .expectClean()
    }
}