/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewPrivacyProtectedBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class PrivacyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = ViewPrivacyProtectedBinding.inflate(LayoutInflater.from(context), this, true)

    private var text: String = ""
        set(value) {
            field = value
            binding.privacyTextDescription.text = value
            this.contentDescription = value
        }

    init {
        orientation = VERTICAL
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PrivacyView,
            0,
            0
        ).apply {
            text = getString(context, R.styleable.PrivacyView_privacyText)

            recycle()
        }
    }
}
