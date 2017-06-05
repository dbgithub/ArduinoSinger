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
 * Created by aitor on 4/22/17.
 * This class loads the layout of the Setting/Preferences page.
 * Binds a listener for whenever a change is done in the Setting/Preferences page.
 */

public class MySettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MY_PERMISSIONS_FINE_LOCATION = 1; // 1 doesn't mean anything, it's just an ID.
    SwitchPreference nearestLocationSwitch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(es.deusto.arduinosinger.R.xml.settings_preferences);

        // Bind a callback listener for changes in preferences page:
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

        // We attach a ChangeListener so that we can check for permission before the STATE of the preference item changes:
        nearestLocationSwitch = (SwitchPreference) findPreference("NearestPlaceNotification");
        nearestLocationSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            /**
             * Before activating the service, first, it is necessary to check the location permission. This is done here, because
             * within the Service implementation is not possible due to its idiosyncrasy.
             * More info at: https://developer.android.com/reference/android/preference/Preference.OnPreferenceChangeListener.html
             * @param preference
             * @param newValue
             * @return TRUE if you want to update the state of the Preference with the new value, otherwise return FALSE
             */
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (((SwitchPreference)preference).isChecked()) {return true;} else {
                return checkLocationPermission();
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Example:
        // if (key.equals("Theme")) {sharedPreferences.getString(key,"").equals("AppTheme2")}
    }

    /**
     * In order to proceed, first it is necessary to check for LOCATION permission. "ACCESS_FINE_LOCATION" is a dangerous permissions, so this
     * means that from API 23 onwards it is mandatory to check permission when the functionality is requested (that is, no when you install the app for the first time).
     * Below API 23, there is no need to check for permissions since the latter should have been granted when installing the app by the user.
     * So, if current build version > API 23, THEN => we should check for permission. ELSE, no need to check anything.
     */
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // Permission DENIED!
            // So, we request him/her to grant the permission for that purpose:
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_FINE_LOCATION);
            return false;
        } else {
            // Permission GRANTED! (or not needed to ask)
            // So, we continue as planned...
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // So, we continue as planned...
                    nearestLocationSwitch.setChecked(true);
                }
                break;
            default:
                break;
        }
    }
}
