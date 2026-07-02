/*
 * VoidTerm - SettingsActivity
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.asotn.voidterm.R;
import com.asotn.voidterm.utils.AppPreferences;
import com.asotn.voidterm.utils.EnvironmentManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // -------------------------------------------------------------------------
    // Settings Fragment
    // -------------------------------------------------------------------------
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            AppPreferences prefs = AppPreferences.get(requireContext());

            // Font size
            SeekBarPreference fontSizePref = findPreference("pref_font_size");
            if (fontSizePref != null) {
                fontSizePref.setValue(prefs.getFontSize());
                fontSizePref.setOnPreferenceChangeListener((p, v) -> {
                    prefs.setFontSize((Integer) v);
                    return true;
                });
            }

            // Keep screen on
            SwitchPreferenceCompat keepOn = findPreference("pref_keep_screen_on");
            if (keepOn != null) {
                keepOn.setChecked(prefs.keepScreenOn());
                keepOn.setOnPreferenceChangeListener((p, v) -> {
                    prefs.setKeepScreenOn((Boolean) v);
                    return true;
                });
            }

            // Kali mirror
            EditTextPreference mirror = findPreference("pref_kali_mirror");
            if (mirror != null) {
                mirror.setText(prefs.getKaliMirror());
                mirror.setSummary(prefs.getKaliMirror());
                mirror.setOnPreferenceChangeListener((p, v) -> {
                    String val = (String) v;
                    // Only HTTPS mirrors are allowed. A plaintext HTTP mirror
                    // can be tampered with in transit before package
                    // signatures are ever checked, so we refuse to save it.
                    if (val == null || !val.startsWith("https://")) {
                        android.widget.Toast.makeText(getContext(),
                            "Mirror must use https://", android.widget.Toast.LENGTH_LONG).show();
                        return false;
                    }
                    prefs.setKaliMirror(val);
                    mirror.setSummary(val);
                    return true;
                });
            }

            // History size
            Preference histPref = findPreference("pref_history_size");
            if (histPref != null) {
                histPref.setSummary(prefs.getHistorySize() + " lines");
            }

            // Reset bootstrap
            Preference resetPref = findPreference("pref_reset_bootstrap");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(p -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Reset Kali Environment")
                        .setMessage("This will delete the Kali rootfs and require re-downloading (~500 MB). Continue?")
                        .setPositiveButton("Reset", (d, w) -> {
                            deleteRecursive(new java.io.File(EnvironmentManager.KALI_ROOTFS_DIR));
                            Toast.makeText(requireContext(),
                                "Kali environment reset. Restart the app.", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                    return true;
                });
            }

            // About
            Preference aboutPref = findPreference("pref_about");
            if (aboutPref != null) {
                aboutPref.setSummary("v26.2 - github.com/Asotn");
            }
        }

        private void deleteRecursive(java.io.File f) {
            if (f.isDirectory()) {
                java.io.File[] children = f.listFiles();
                if (children != null) {
                    for (java.io.File child : children) deleteRecursive(child);
                }
            }
            f.delete();
        }
    }
}
