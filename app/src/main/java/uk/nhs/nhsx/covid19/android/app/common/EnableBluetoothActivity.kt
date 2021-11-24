/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEdgeCaseBinding
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.BLUETOOTH_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.packagemanager.PackageManager
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject
import javax.inject.Named

class EnableBluetoothActivity : BaseActivity() {

    @Inject
    @Named(BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    @Inject
    lateinit var packageManager: PackageManager

    private lateinit var binding: ActivityEdgeCaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityEdgeCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            edgeCaseTitle.text = getString(R.string.enable_bluetooth_title)
            edgeCaseText.text = getString(R.string.enable_bluetooth_rationale)

            takeActionButton.text = getString(R.string.allow_bluetooth)
            takeActionButton.setOnSingleClickListener {
                navigateToBluetoothSettings()
            }
        }

        bluetoothStateProvider.availabilityState.observe(this) { state ->
            if (state == ENABLED) {
                finish()
            }
        }
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
        Snackbar.make(binding.edgeCaseContainer, R.string.enable_bluetooth_error_hint, Snackbar.LENGTH_LONG).show()
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
