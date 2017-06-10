package es.deusto.arduinosinger;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

/**
 * Created by aitor on 10/06/17.
 * This class loads the layout of the Setting/Preferences page.
 * Binds a listener for whenever a change is done in the Setting/Preferences page.
 */

public class MySettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(es.deusto.arduinosinger.R.xml.settings_preferences);

        // Bind a callback listener for changes in preferences page:
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

//        // We attach a ChangeListener so that we can check for permission before the STATE of the preference item changes:
//        nearestLocationSwitch = (SwitchPreference) findPreference("NearestPlaceNotification");
//        nearestLocationSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            /**
//             * Before activating the service, first, it is necessary to check the location permission. This is done here, because
//             * within the Service implementation is not possible due to its idiosyncrasy.
//             * More info at: https://developer.android.com/reference/android/preference/Preference.OnPreferenceChangeListener.html
//             * @param preference
//             * @param newValue
//             * @return TRUE if you want to update the state of the Preference with the new value, otherwise return FALSE
//             */
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if (((SwitchPreference)preference).isChecked()) {return true;} else {
//                return checkLocationPermission();
//                }
//            }
//        }); // TODO: to delete!!
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Example:
        // if (key.equals("Theme")) {sharedPreferences.getString(key,"").equals("AppTheme2")}
    }

}
