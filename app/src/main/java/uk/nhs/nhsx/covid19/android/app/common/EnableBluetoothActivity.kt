/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.Bundle
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseContainer
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.BLUETOOTH_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.packagemanager.PackageManager
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

class EnableBluetoothActivity : BaseActivity(R.layout.activity_edge_case) {

    @Inject
    @Named(BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    @Inject
    lateinit var packageManager: PackageManager

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
        val bluetoothSettingsIntent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)

        val canDeviceResolveBluetoothSettingsIntent =
            packageManager.resolveActivity(
                this,
                bluetoothSettingsIntent,
                MATCH_DEFAULT_ONLY
            ) != null

        if (canDeviceResolveBluetoothSettingsIntent) {
            try {
                startActivity(bluetoothSettingsIntent)
            } catch (e: Exception) {
                showBluetoothSettingsNavigationFailedMessage()
            }
        } else {
            showBluetoothSettingsNavigationFailedMessage()
        }
    }

    private fun showBluetoothSettingsNavigationFailedMessage() {
        Snackbar.make(edgeCaseContainer, R.string.enable_bluetooth_error_hint, Snackbar.LENGTH_LONG).show()
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
