package uk.nhs.nhsx.covid19.android.app.status.localmessage

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.UNKNOWN
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalMessageRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import java.time.Instant
import kotlin.test.assertTrue

class LocalMessageActivityTest : EspressoTest() {
    private val localMessageRobot = LocalMessageRobot()
    private val statusRobot = StatusRobot()

    private val head = "A new variant of concern is in your area"
    private val content = listOf(
        ContentBlock(
            type = PARAGRAPH,
            text = "There have been reported cases of a new variant in AL1. Here are some key pieces of information to help you stay safe"
        ),
        ContentBlock(
            type = PARAGRAPH,
            text = "There have been reported cases of a new variant in AL1. Here are some key pieces of information to help you stay safe",
            link = "http://example.com",
            linkText = "Click me"
        ),
        ContentBlock(
            type = UNKNOWN,
            text = "There have been reported cases of a new variant in AL1. Here are some key pieces of information to help you stay safe",
            link = "http://example.com",
            linkText = "Click me"
        ),
        ContentBlock(
            type = PARAGRAPH,
            link = "http://example.com"
        ),
        ContentBlock(type = PARAGRAPH),
        ContentBlock(type = UNKNOWN)
    )

    private val localMessage = LocalMessageTranslation(
        head = head,
        body = "This is the body of the notification",
        content = content
    )

    private val response = LocalMessagesResponse(
        localAuthorities = mapOf(
            "E07000240" to listOf("message1")
        ),
        messages = mapOf(
            "message1" to LocalMessage(
                type = NOTIFICATION,
                updated = Instant.parse("2021-05-19T14:59:13Z"),
                contentVersion = 1,
                translations = TranslatableLocalMessage(
                    mapOf("en" to localMessage)
                )
            )
        )
    )

    @Test
    fun startLocalMessageActivity_displaysContentBlocks() = notReported {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = response

        startTestActivity<LocalMessageActivity>()

        localMessageRobot.checkActivityIsDisplayed()

        localMessageRobot.checkTitleIsDisplayed(head)

        localMessageRobot.checkListSize(4)
        localMessageRobot.checkContentIsDisplayedAtPosition(content[0], 0)
        localMessageRobot.checkContentIsDisplayedAtPosition(content[1], 1)
        localMessageRobot.checkContentIsDisplayedAtPosition(content[2], 2)
        localMessageRobot.checkContentIsDisplayedAtPosition(content[3], 3)
    }

    @Test
    fun startLocalMessageActivity_whenClickLink_opensInBrowser() = notReported {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = response

        startTestActivity<LocalMessageActivity>()

        localMessageRobot.checkActivityIsDisplayed()

        assertBrowserIsOpened("http://example.com") {
            localMessageRobot.clickLink()
        }
    }

    @Test
    fun startLocalMessageActivity_whenClickCloseButton_finishesActivity() = notReported {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = response

        val activity = startTestActivity<LocalMessageActivity>()

        localMessageRobot.checkActivityIsDisplayed()

        localMessageRobot.clickCloseButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun startLocalMessageActivity_whenClickBackToHome_finishesActivity() = notReported {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = response

        val activity = startTestActivity<LocalMessageActivity>()

        localMessageRobot.checkActivityIsDisplayed()

        localMessageRobot.clickBackToHome()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
