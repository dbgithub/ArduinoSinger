package es.deusto.arduinosinger;

import android.app.Activity;
import android.os.Bundle;

/**
 * This activity represents a Settings/Preferences activity with customizable items or options
 */
public class MySettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the activity is launched, the main content is replaced with an instantiation of MySettingsFragment class,
        // which contains the layout for Settings/Preferences page.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MySettingsFragment()).commit();
    }
}
