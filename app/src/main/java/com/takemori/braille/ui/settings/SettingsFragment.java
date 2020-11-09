package com.takemori.braille.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.takemori.braille.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     * Subclasses are expected to call {@link #setPreferenceScreen(PreferenceScreen)} either
     * directly or via helper methods such as {@link #addPreferencesFromResource(int)}.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *                           this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     *                           {@link androidx.preference.PreferenceScreen} with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);

        // Find the preference about the privacy statement and launch a dialog onClick.
        Preference privacyPreference = findPreference("privacy");
        privacyPreference.setOnPreferenceClickListener(preference -> {
            privacyLaunchDialog();
            return true;
        });
    }


    // Generate the onClick functions of options settings after the preference fragment is inflated
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public AlertDialog privacyNoticeDialogBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.privacy_policy_title);
        builder.setMessage(R.string.privacy_policy_V1_0);
        return builder.create();
    }

    public void privacyLaunchDialog() {
        AlertDialog privacyAlert = privacyNoticeDialogBuilder();
        privacyAlert.show();
    }


}