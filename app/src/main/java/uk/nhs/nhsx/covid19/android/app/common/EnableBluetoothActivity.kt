/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.BLUETOOTH_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

class EnableBluetoothActivity : AppCompatActivity(R.layout.activity_edge_case) {

    @Inject
    @Named(BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        edgeCaseTitle.text = getString(R.string.enable_bluetooth_title)
        edgeCaseText.text = getString(R.string.enable_bluetooth_rationale)

        takeActionButton.text = getString(R.string.allow_bluetooth)
        takeActionButton.setOnClickListener {
            navigateToBluetoothSettings()
        }

        bluetoothStateProvider.availabilityState.observe(
            this,
            Observer { state ->
                if (state == ENABLED) {
                    finish()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        bluetoothStateProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        bluetoothStateProvider.stop(this)
    }

    override fun onBackPressed() {
    }

    private fun navigateToBluetoothSettings() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.component = ComponentName(
            "com.android.settings",
            "com.android.settings.bluetooth.BluetoothSettings"
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(
                getIntent(
                    context
                )
            )

        private fun getIntent(context: Context) =
            Intent(context, EnableBluetoothActivity::class.java)
    }
}
