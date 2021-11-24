package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class ScenarioDialogFragment<VB : ViewBinding>(
    private val positiveAction: (() -> Unit),
    private val dismissAction: (() -> Unit)? = null
) : DialogFragment() {
    protected abstract val title: String

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it).apply {
                setTitle(title)
                _binding = setupBinding(LayoutInflater.from(it))
                setView(binding.root)
                setupView()

                setPositiveButton(android.R.string.ok) { _, _ ->
                    positiveAction.invoke()
                }
                setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dismissAction?.invoke()
                    dialog.dismiss()
                }
            }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    abstract fun setupBinding(inflater: LayoutInflater): VB
    abstract fun setupView()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected fun Boolean.toViewState(): Int =
        if (this) View.VISIBLE else View.INVISIBLE
}
