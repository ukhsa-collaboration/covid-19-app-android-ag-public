package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import uk.nhs.nhsx.covid19.android.app.ExposureApplication
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.plurals
import uk.nhs.nhsx.covid19.android.app.databinding.DialogExposureNotificationReminderBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.Duration

class ExposureNotificationReminderDialog(
    context: Context,
    private val scheduleExposureNotification: (duration: Duration) -> Unit
) : BottomSheetDialog(context) {

    private lateinit var binding: DialogExposureNotificationReminderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogExposureNotificationReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeView()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun initializeView() = with(binding) {
        minute1.isVisible = ExposureApplication.isTestBuild
        minute1.setOnSingleClickListener {
            onRadioButtonClick(Duration.ofMinutes(1))
        }

        updateResumeRadioButton(hours4, 4)
        updateResumeRadioButton(hours8, 8)
        updateResumeRadioButton(hours12, 12)

        cancel.setOnSingleClickListener { dismiss() }
    }

    private fun updateResumeRadioButton(
        radioButton: RadioButton,
        hours: Int
    ) {
        radioButton.text =
            context.resources.getQuantityString(plurals.resume_contact_tracing_hours, hours, hours)
        radioButton.setOnSingleClickListener {
            onRadioButtonClick(Duration.ofHours(hours.toLong()))
        }
    }

    private fun onRadioButtonClick(duration: Duration) {
        scheduleExposureNotification(duration)
        showNotificationReminderConfirmationDialog(duration)
        dismiss()
    }

    private fun showNotificationReminderConfirmationDialog(duration: Duration) {
        val builder = AlertDialog.Builder(context)
        val hours = duration.toHours().toInt().toString()

        builder.setTitle(
            context.getString(
                R.string.contact_tracing_notification_reminder_dialog_title,
                hours
            )
        )
        builder.setMessage(context.getString(R.string.contact_tracing_notification_reminder_dialog_message))
        builder.setPositiveButton(
            context.getString(R.string.okay)
        ) { _, _ ->
        }

        builder.show()
    }
}
