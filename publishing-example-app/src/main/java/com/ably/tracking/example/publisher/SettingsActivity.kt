package com.ably.tracking.example.publisher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

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
        (findPreference(getString(R.string.preferences_s3_file_key)) as ListPreference?)?.let { s3Preference ->
            S3Helper.fetchLocationHistoryFilenames(
                onListLoaded = { filenamesWithSizes, filenames ->
                    s3Preference.entries = filenamesWithSizes.toTypedArray()
                    s3Preference.entryValues = filenames.toTypedArray()
                },
                onUninitialized = {
                    Toast.makeText(
                        requireContext(),
                        "S3 not initialized - cannot fetch files",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}
