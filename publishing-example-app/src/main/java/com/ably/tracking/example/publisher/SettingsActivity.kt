package com.ably.tracking.example.publisher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.ably.tracking.Accuracy

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, SettingsFragment())
            .commit()
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_preferences)
        setupLocationSourcePreference()
        setupRoutingProfilePreference()
        setupVehicleProfilePreference()
        setupDefaultResolutionPreferences()
        setupConstantLocationEngineResolutionPreferences()
        loadS3Preferences()
    }

    private fun setupS3Preference(
        filenamesWithSizes: List<CharSequence>,
        filenames: List<CharSequence>
    ) {
        val appPreferences = AppPreferences.getInstance(requireContext())
        (findPreference(getString(R.string.preferences_s3_file_key)) as ListPreference?)?.let { s3Preference ->
            preferenceScreen.removePreference(s3Preference)
            s3Preference.entries = filenamesWithSizes.toTypedArray()
            s3Preference.entryValues = filenames.toTypedArray()
            s3Preference.value = appPreferences.getS3File()
            s3Preference.parent?.addPreference(s3Preference)
        }
    }

    private fun loadS3Preferences() {
        (findPreference(getString(R.string.preferences_s3_file_key)) as ListPreference?)?.let { s3Preference ->
            /* Please note that this is not an ideal solution.
              It would be better if UI would be uninteractable when in this state
              I tried isEnabled and isSelectable with no success so I'm setting them to emptyArrays*/
            s3Preference.entryValues = emptyArray()
            s3Preference.entries = emptyArray()
        }
        S3Helper.fetchLocationHistoryFilenames(
            onListLoaded = { filenamesWithSizes, filenames ->
                setupS3Preference(filenamesWithSizes, filenames)
            },
            onUninitialized = {
                requireContext().showShortToast(R.string.error_s3_not_initialized_history_data_fetch_failed)
            }
        )
    }

    private fun setupLocationSourcePreference() {
        val appPreferences = AppPreferences.getInstance(requireContext())
        (findPreference(getString(R.string.preferences_location_source_key)) as ListPreference?)?.apply {
            entries = LocationSourceType.values()
                .map { getString(it.displayNameResourceId) }
                .toTypedArray()
            entryValues = LocationSourceType.values().map { it.name }.toTypedArray()
            value = appPreferences.getLocationSource().name
        }
    }

    private fun setupRoutingProfilePreference() {
        val appPreferences = AppPreferences.getInstance(requireContext())
        (findPreference(getString(R.string.preferences_routing_profile_key)) as ListPreference?)?.apply {
            entries = RoutingProfileType.values()
                .map { getString(it.displayNameResourceId) }
                .toTypedArray()
            entryValues = RoutingProfileType.values().map { it.name }.toTypedArray()
            value = appPreferences.getRoutingProfile().name
        }
    }

    private fun setupVehicleProfilePreference() {
        val appPreferences = AppPreferences.getInstance(requireContext())
        (findPreference(getString(R.string.preferences_vehicle_profile_key)) as ListPreference?)?.apply {
            entries = VehicleProfileType.values()
                .map { getString(it.displayNameResourceId) }
                .toTypedArray()
            entryValues = VehicleProfileType.values().map { it.name }.toTypedArray()
            value = appPreferences.getVehicleProfile().name
        }
    }

    private fun setupDefaultResolutionPreferences() {
        val appPreferences = AppPreferences.getInstance(requireContext())
        (findPreference(getString(R.string.preferences_resolution_accuracy_key)) as ListPreference?)?.apply {
            entries = Accuracy.values()
                .map { accuracy -> accuracy.name.lowercase().replaceFirstChar { it.uppercase() } }
                .toTypedArray()
            entryValues = Accuracy.values().map { it.name }.toTypedArray()
            value = appPreferences.getResolutionAccuracy().name
        }
        (findPreference(getString(R.string.preferences_resolution_desired_interval_key)) as EditTextPreference?)?.apply {
            text = appPreferences.getResolutionDesiredInterval().toString()
            setIntNumberInputType()
        }
        (findPreference(getString(R.string.preferences_resolution_minimum_displacement_key)) as EditTextPreference?)?.apply {
            text = appPreferences.getResolutionMinimumDisplacement().toString()
            setFloatNumberInputType()
        }
    }

    private fun setupConstantLocationEngineResolutionPreferences() {
        val appPreferences = AppPreferences.getInstance(requireContext())
        val constantLocationEngineResolution = appPreferences.getConstantLocationEngineResolution()
        (findPreference(getString(R.string.preferences_constant_resolution_accuracy_key)) as ListPreference?)?.apply {
            entries = Accuracy.values()
                .map { accuracy -> accuracy.name.lowercase().replaceFirstChar { it.uppercase() } }
                .toTypedArray()
            entryValues = Accuracy.values().map { it.name }.toTypedArray()
            value = constantLocationEngineResolution.accuracy.name
        }
        (findPreference(getString(R.string.preferences_constant_resolution_desired_interval_key)) as EditTextPreference?)?.apply {
            text = constantLocationEngineResolution.desiredInterval.toString()
            setIntNumberInputType()
        }
        (findPreference(getString(R.string.preferences_constant_resolution_minimum_displacement_key)) as EditTextPreference?)?.apply {
            text = constantLocationEngineResolution.minimumDisplacement.toString()
            setFloatNumberInputType()
        }
    }
}
