/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.nhs.nhsx.covid19.android.app.fieldtests.storage

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.lifecycle.LiveData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import uk.nhs.nhsx.covid19.android.app.exposure.FieldTestConfiguration

class ExperimentSettingsProvider(context: Context) {

    private val moshi = Moshi.Builder().build()
    private val configurationAdapter = moshi.adapter(FieldTestConfiguration::class.java)
    private val configurationListType =
        Types.newParameterizedType(List::class.java, FieldTestConfiguration::class.java)
    private val configurationListAdapter: JsonAdapter<List<FieldTestConfiguration>> =
        moshi.adapter(configurationListType)

    private val sharedPref = context.getSharedPreferences(
        PREFS_FILE_NAME,
        Context.MODE_PRIVATE
    )

    private val experimentId =
        SharedPreferenceStringLiveData(
            sharedPref,
            PREFS_KEY_EXPERIMENT_ID,
            ""
        )

    fun getExperimentIdLiveData(): LiveData<String> {
        return experimentId
    }

    fun getExperimentId(): String {
        return sharedPref.getString(PREFS_KEY_EXPERIMENT_ID, "")!!
    }

    fun setExperimentId(newName: String) {
        sharedPref.edit().putString(PREFS_KEY_EXPERIMENT_ID, newName).apply()
    }

    var deviceName: String
        get() {
            val device = BluetoothAdapter.getDefaultAdapter()
            val defaultName = device.name
            return sharedPref.getString(PREFS_KEY_DEVICE_NAME, defaultName)!!.replace(" ", "_")
        }
        set(newName) {
            val tidy = newName.replace(" ", "_")
            sharedPref.edit().putString(PREFS_KEY_DEVICE_NAME, tidy).apply()
        }

    var teamId: String
        get() {
            val defaultName = ""
            return sharedPref.getString(PREFS_KEY_TEAM_ID, defaultName)!!
        }
        set(newId) {
            sharedPref.edit().putString(PREFS_KEY_TEAM_ID, newId).apply()
        }

    var automaticDetectionFrequency: Int
        get() {
            return sharedPref.getInt(PREFS_KEY_AUTOMATIC_DETECTION_FREQUENCY, 0)
        }
        set(frequency) {
            sharedPref.edit().putInt(PREFS_KEY_AUTOMATIC_DETECTION_FREQUENCY, frequency).apply()
        }

    var experimentName: String
        get() {
            return sharedPref.getString(PREFS_KEY_EXPERIMENT_NAME, "")!!
        }
        set(newName) {
            sharedPref.edit().putString(PREFS_KEY_EXPERIMENT_NAME, newName).apply()
        }

    fun putConfigurations(configurations: List<FieldTestConfiguration>) {
        val configs = configurationListAdapter.toJson(configurations)
        sharedPref.edit().putString(PREFS_KEY_CONFIGS, configs).apply()
    }

    fun getConfigurations(): List<FieldTestConfiguration> {
        return configurationListAdapter.fromJson(sharedPref.getString(PREFS_KEY_CONFIGS, "")!!)
            ?: listOf()
    }

    companion object {
        private const val PREFS_FILE_NAME = "prefs"
        private const val PREFS_KEY_DEVICE_NAME = "device_name"
        private const val PREFS_KEY_EXPERIMENT_ID = "experiment_id"
        private const val PREFS_KEY_EXPERIMENT_NAME = "experiment_name"
        private const val PREFS_KEY_TEAM_ID = "team_id"
        private const val PREFS_KEY_AUTOMATIC_DETECTION_FREQUENCY = "automatic_detection_frequency"
        private const val PREFS_KEY_CONFIGS = "configs"
    }
}
