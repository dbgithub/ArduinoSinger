package es.deusto.arduinosinger;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class ListPlacesActivity extends AppCompatActivity implements DialogInterface.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    // Declaring a ArrayList of Place entity:
    public ArrayList<Place> arraylPlaces = new ArrayList<>();
    public static ArrayList<Place> arraylPlaces_backup = new ArrayList<>(); // This list will be used to restore the previous items after a search (like a backup)
    private ArrayAdapter<Place> arrayadapPlaces;
    public static final int CREATE_PLACE = 0; // ID for CreatePlace Intent
    public static final int PLACE_DETAILS = 1; // ID for PlaceDetails Intent
    private static final int MY_PERMISSIONS_FINE_LOCATION = 3; // 3 doesn't mean anything, it's just an ID.
    private int edited_place_index;
    private ActionMode mActionMode = null; // Action mode for CAB (Contextual Action Bar)
    public static GoogleApiClient myGoogleApiClient; // For accessing Google Play Services
    public Location location = null; // For storing current location
    private int sortType = -1;
    public static int sortingDistance; // Sorting distance to sort out the Places list.
    private Intent serviceIntent = null; // An Intent that works as a Service. If NULL, then it means that the service has not started, if not NULL, then the service is active.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_list_places);
        Toolbar toolbar = (Toolbar) findViewById(es.deusto.arduinosinger.R.id.toolbar);
        setSupportActionBar(toolbar);
        PreferenceManager.setDefaultValues(this, es.deusto.arduinosinger.R.xml.settings_preferences, false); // Loads DEFAULT values for any item in Settings/Preferences page.
        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(this); // Binds a callback listener for changes in preferences/settings page:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sortingDistance = Integer.valueOf(sharedPref.getString("coverDistance", "50000")); // 50000m = 50KM. Within 50KM around (a la redonda). Sorting distance to sort out the Places list.

        FloatingActionButton fab = (FloatingActionButton) findViewById(es.deusto.arduinosinger.R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                        // Ejemplo de un snackbar como mensaje emergente: Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Intent createPlacelIntent = new Intent(getBaseContext(), CreateEditPlaceActivity.class);
                startActivityForResult(createPlacelIntent, CREATE_PLACE);
            }
        });

        loadDummyPlaces(); // loads the data from internal storage (if any)
        arrayadapPlaces = new ArrayAdapter<Place>(this, es.deusto.arduinosinger.R.layout.list_item_place, es.deusto.arduinosinger.R.id.list_item_place_tv_title, arraylPlaces) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView name = (TextView) view.findViewById(es.deusto.arduinosinger.R.id.list_item_place_tv_title);
                TextView location = (TextView) view.findViewById(es.deusto.arduinosinger.R.id.list_item_place_tv_location);
                name.setText(arraylPlaces.get(position).getName());
                location.setText(arraylPlaces.get(position).getNeighborhood());
                // TODO the image should also be retrieved
                return view;
            }

            // It is necessary to override the getFilter method in order to implement the search feature:
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    /**
                     * After a search, this method return a list of found items (if any)
                     * @param constraint the word or phrase being filtered
                     * @param results a set of elements containing the searched string
                     */
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        arraylPlaces.clear();
                        arraylPlaces.addAll((ArrayList<Place>) results.values);
                        notifyDataSetChanged();
                    }

                    /**
                     * When the user searches something, this method filters the ListView according to the query string
                     * @param constraint the word or phrase being filtered
                     * @return a set of elements containing the searched string
                     */
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        ArrayList<Place> FilteredArrayNames = new ArrayList<>();

                        // Now, we look for the word we are interested in
                        constraint = constraint.toString().toLowerCase();
                        for (int i = 0; i < arraylPlaces.size(); i++) {
                            Place p = arraylPlaces.get(i);
                            if (p.getName().toLowerCase().contains(constraint.toString()))  {
                                FilteredArrayNames.add(p);
                            }
                        }
                        results.count = FilteredArrayNames.size();
                        results.values = FilteredArrayNames;
                        return results;
                    }
                };
            }
        };
        // LIST VIEW listing places, if any:
        final ListView lv_places = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
        lv_places.setAdapter(arrayadapPlaces);
        lv_places.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                edited_place_index = position;
                Intent itemDetailIntent = new Intent(getBaseContext(), PlaceDetailsActivity.class);
                itemDetailIntent.putExtra(PlaceDetailsActivity.PLACE_DETAILS, arraylPlaces.get(position));
                startActivityForResult(itemDetailIntent, PLACE_DETAILS);
            }
        });
        // We make sure Contextual Action Bar is configured as needed:
        lv_places.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv_places.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }
                // Important! mark the editing row as activated:
                lv_places.setItemChecked(position,true);
                edited_place_index = position; // save the row position of the item to make changes later on, if needed.
                // Start CAB referencing a callback defined later in the code:
                mActionMode = ListPlacesActivity.this.startSupportActionMode(myActionModeCallBack); // Watch out! If we weren't using AppCompatibilityActivity, then we would have to use: ".this.startActionMode(..)"
                return true;
            }
        });

        // If this activity is launched due to an Intent (in this case, ACTION_SEARCH), handleIntent will execute the corresponding code.
        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(es.deusto.arduinosinger.R.menu.menu_list_places, menu);

        // Associate searchable configuration with the SearchView:
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(es.deusto.arduinosinger.R.id.app_bar_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Remember that the ShareActionProvider is implemented in the ContextualActionBar's (CAB) callback
        return true;
    }

    /**
     * Callback used to response to the actions taken from CAB (Contextual Action Bar).
     * It's like CAB's own life cycle where you can define and implement specific behavior for CAB.
     * REMEMBER! if you want the CAB overlay the standard bar, then change your styles.xml adding "<item name="windowActionModeOverlay">true</item>"
     * IMPORTANT: Watch out when implementing ShareActionProvider when your activity is backwards compatible. ActionMode Callbacks should be imported with the compatibility library (..support.v7.view..).
     */
    private android.support.v7.view.ActionMode.Callback myActionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(es.deusto.arduinosinger.R.menu.menu_cab_list_places, menu);
            // WATCH OUT: To integrate ShareActionProvider with backwards compatibility follow the steps at: https://developer.android.com/reference/android/support/v7/widget/ShareActionProvider.html
            // WATCH OUT: To integrate ShareActionProvider with a normal Activity (without compatibility) follow the steps at: https://developer.android.com/reference/android/widget/ShareActionProvider.html
            // WATCH OUT: More help if needed: http://stackoverflow.com/questions/24219842/getactionprovider-item-does-not-implement-supportmenuitem
            // Bind share icon with share functionality:
            MenuItem mnuShare = menu.findItem(es.deusto.arduinosinger.R.id.mnu_cab_share_place);
            ShareActionProvider shareProv = (ShareActionProvider) MenuItemCompat.getActionProvider(mnuShare); // If we were not using AppCompatActivity, we would had to use: (ShareActionProvider) mnuShare.getActionProvider();
            shareProv.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Did you realize about this place? '"+arraylPlaces.get(edited_place_index).getName()+"', located at "+
                    arraylPlaces.get(edited_place_index).getNeighborhood() +"! Check it out! (OnTheStreep app)");
            shareProv.setShareIntent(shareIntent);
            return true;
        }

        // Called when the user enters the action mode (that is, CAB is displayed)
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ListView lv_places = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
            lv_places.setEnabled(false);
            return false;
        }

        /**
         * Implements the business logic related to what happens when the user clicks on an item on the CAB.
         * @param mode the Contextual Action Bar itself
         * @param item an item on which the user has interacted (tapped or similar)
         * @return boolean
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case es.deusto.arduinosinger.R.id.mnu_cab_delete_place:
                    arraylPlaces.remove(edited_place_index);
                    arraylPlaces_backup.remove(edited_place_index);
                    arrayadapPlaces.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Place deleted :)", Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user EXITS the action mode (that is, when CAB fades away)
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Re-enable the list after edition:
            ListView lv_places = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
            lv_places.setEnabled(true);
            // Reset the CAB:
            mActionMode = null;
        }
    };

    /**
     * Overrides the behaviour of what happens when a new Intent is launched from within this Activity.
     * This current activity is prepared to receive just ACTION_SEARCH intents so as to implement the search feature
     * @param intent the Intent the Activity is capturing when it launches an Intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Executes the filtering on the ListView based on the query string.
     * @param intent the Intent the Activity is capturing
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            arrayadapPlaces.getFilter().filter(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case es.deusto.arduinosinger.R.id.app_remove_search_filter:
                resetListView();
                break;
            case es.deusto.arduinosinger.R.id.action_settings:
                startActivity(new Intent(this, MySettingsActivity.class));
                break;
            case es.deusto.arduinosinger.R.id.app_sort_by:
                launchSortByDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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
                    acquiredLocation();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_PLACE){ // If the Intent code was "CREATE_PLACE", then add the new item and update the list
            if(resultCode == Activity.RESULT_OK){
                // Normally you'd do: "data.getStringExtra" or similar. But in this case we are retrieving a Serializable object
                arraylPlaces.add((Place) data.getSerializableExtra("place"));
                arraylPlaces_backup.add((Place) data.getSerializableExtra("place"));
                arrayadapPlaces.notifyDataSetChanged();
                if (arraylPlaces.size() >= 1) {startNearestPlaceService();}
            }
        } else if (requestCode == PLACE_DETAILS) { // This will happen when the user goes from the 'details' Activity back to the main Activity.
            if(resultCode == Activity.RESULT_OK) {
                arraylPlaces.set(edited_place_index, (Place) data.getSerializableExtra("place"));
                arraylPlaces_backup.set(edited_place_index, (Place) data.getSerializableExtra("place"));
                arrayadapPlaces.notifyDataSetChanged();
            }
        }
    }

    /**
     * Creates and shows a dialog box with multiple choice radio buttons in order to sort the places.
     */
    private void launchSortByDialog() {
        // A Dialog is launched in order to offer a choice of ordering:
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(ListPlacesActivity.this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle("Sort by...")
                .setSingleChoiceItems(es.deusto.arduinosinger.R.array.sortingpreferences, sortType, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Sort by nearest places
                                sortType = 0;
                                break;
                            case 1:
                                // Revert back to default sorting. Show all.
                                sortType = 1;
                                break;
                            default:
                                break;
                        }
                    }
                });
                //.setMessage("Heeelloooooooooooooooooo") // setMessage is not compatible when you set multiple items to display
        // 3. Add the buttons. When launching dialogs, you decide whether passing the events back to the Dialog's host or manage it within the dialog implementation
        builder.setPositiveButton("OK", ListPlacesActivity.this);
        builder.setNegativeButton("CANCEL", ListPlacesActivity.this);
                    // Example of managing the event within the dialog implementation:
//                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            // User clicked OK button
//                        }
//                    });
        // 4. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * When launching dialogs, you decide whether passing the events back to the Dialog's host or manage it within the dialog implementation
     * @param dialog
     * @param which
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        // -1 -> OK pressed
        // -2 -> CANCEL pressed
        if (which == -1) {
            if (sortType == 0) {
                // Sort lists by nearest places.
                checkLocationPermission();
            } else {
                // Revert the list back to default view. Show all places.
                resetListView();
            }
        }
    }

    /**
     * Loads persistent dummy places. Since the data has to be persistent, we will proceed by reading a file and loading its content.
     */
    private void loadDummyPlaces() {
        // Non-persistent initialization:
            //this.arraylPlaces.add(new Place("El garito de Joe", "Uribarri", "Mola!"));
            //this.arraylPlaces.add(new Place("Bar los Angeles", "Uribarri", "Mola!"));
            //this.arraylPlaces.add(new Place("Plaza Circular", "Abando", "Es circular!"));
            //this.arraylPlaces.add(new Place("Dummy place1", "Location1", "Aqui se estrelló un avión!"));
            //this.arraylPlaces.add(new Place("Dummy place2", "Location2", "No se que poner"));
            //this.arraylPlaces.add(new Place("Dummy place3", "Location3", "Ultimo dummy creado"));
        // Persistent way of loading data:
        arraylPlaces = new PersistanceManager(getApplicationContext()).loadPlaces();
        arraylPlaces_backup = new PersistanceManager(getApplicationContext()).loadPlaces();
        if (arraylPlaces != null) {
            if (!arraylPlaces.isEmpty()) {
                // If not empty, then, we should start the NearestPlace service to watch the nearest place:
                startNearestPlaceService();
            }
        } else {arraylPlaces = new ArrayList<>();}
        if (arraylPlaces_backup == null) {arraylPlaces_backup = new ArrayList<>();} // if empty, then initialize to empty ArrayList
    }

    /**
     * Starts the NearestPlace service if needed! First, the preference/settings is checked.
     */
    private void startNearestPlaceService() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            Boolean settingsPreference_nearestPlaceSwitch = sharedPref.getBoolean("NearestPlaceNotification", false);
            if (settingsPreference_nearestPlaceSwitch && serviceIntent==null) {
                serviceIntent = new Intent(this, NearestPlaceService.class);
                startService(serviceIntent);
            }
    }

    /**
     * Reset the ListView Place by loading back again the content of the backup ArrayList. In other words: list all items again.
     */
    private void resetListView() {
        arraylPlaces.clear();
        arraylPlaces.addAll(arraylPlaces_backup);
        arrayadapPlaces.notifyDataSetChanged();
        sortType = -1;
    }

    /**
     * In order to proceed, first it is necessary to check for LOCATION permission. "ACCESS_FINE_LOCATION" is a dangerous permissions, so this
     * means that from API 23 onwards it is mandatory to check permission when the functionality is requested (that is, no when you install the app for the first time).
     * Below API 23, there is no need to check for permissions since the latter should have been granted when installing the app by the user.
     * So, if current build version > API 23, THEN => we should check for permission. ELSE, no need to check anything.
     */
    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // Permission DENIED!
            // So, we request him/her to grant the permission for that purpose:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_FINE_LOCATION);
        } else {
            // Permission GRANTED! (or not needed to ask)
            // So, we continue as planned...
            acquiredLocation();
        }
    }

    @SuppressWarnings("MissingPermission")
    /**
     * Acquires/Gets the current location if it is possible. Assign a default callback for future location updates.
     * LOCATION permissions should have already been checked before calling this method.
     */
    private void acquiredLocation() {

        // If the location can be acquired:
        location = LocationServices.FusedLocationApi.getLastLocation(myGoogleApiClient);
        // Now a Location request object is created to request periodically the location at any frequency we want:
        LocationRequest myLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);
        // Now a callback is assigned by default for whenever the location is updated ('onLocationChanged' method):
        LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleApiClient, myLocationRequest, this);

        // Location object is updated:
        if (location != null) {sortOutPlaces();} else  {Log.i("MYLOG", "Not possible to update location. Location object is NULL! :(");}
    }

    private void sortOutPlaces() {
        arraylPlaces.clear();
        for (Place p : arraylPlaces_backup) {
            if (p.getLongitude() != 0 && p.getLatitude() != 0) {
                Location tmp_lo = new Location("");
                tmp_lo.setLongitude(p.getLongitude());
                tmp_lo.setLatitude(p.getLatitude());
                if (location.distanceTo(tmp_lo) <= sortingDistance*1000) {
                    Log.i("MYLOG","Esta dentro de un radio de "+sortingDistance+"KM a la redonda (distancia: "+location.distanceTo(tmp_lo)+")");
                    arraylPlaces.add(p);
                } else {Log.i("MYLOG","Esta fuera de un radio de "+sortingDistance+"KM a la redonda");}
            }
        }
        arrayadapPlaces.notifyDataSetChanged();
    }

    // Life cycle of the activity:

    @Override
    protected void onStart() {
        super.onStart();
        // Start the connection to Google Play Services (a callback with the status of the connection will be received)
        connectToGooglePlayServices();
        // If the NearestPlaceNotification service was not initialized and now is ON, then it is necessary to trigger it.
        startNearestPlaceService();
    }

    /**
     * When the app is finishing and it is about to close and to be destroyed, then we make sure that
     * the data is saved/persisted correctly.
     */
    @Override
    protected void onStop(){
        super.onStop();
        // Clear search results (if any). Going back to initial ListView state.
        resetListView();
        (new PersistanceManager(getApplicationContext())).savePlaces(arraylPlaces);
        // Start the disconnection process from Google Play Services (a callback with the status of the process will be received)
        disconnectFromGooglePlayServices();
    }
    // ------------------

    /**
     * Connects to GooglePlayServices so as to work with it later on.
     * For that, Google Play Services availability is checked (version code), then we proceed to instantiate our local variable and then, connect.
     */
    private void connectToGooglePlayServices() {
        // Check Google Play Services availability:
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS){
            Log.i("MYLOG","Google Play Services version code: " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE); // Just log Google Play Services version code to check errors. If the version installed in the smartphone is lower than the version linked in the app (build.gradle) our app will not work.
        }

        // Create the Google API Client
        myGoogleApiClient =  new GoogleApiClient.Builder(ListPlacesActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // ..and connect!
        myGoogleApiClient.connect();
    }

    /**
     * Removes location update callback reference and disconnects from Google Play Services.
     */
    private void disconnectFromGooglePlayServices() {
        LocationServices.FusedLocationApi.removeLocationUpdates(myGoogleApiClient, this);
        if(myGoogleApiClient.isConnected()){
            myGoogleApiClient.disconnect();
            Log.i("MYLOG", "Disconnected from Google Play Services! :)");
        }
    }

    // Google Play Services and Location CALLBACKS:

    /**
     * GoogleApiClient.OnConnectionCallbacks INTERFACE
     * Callback for whenever the connection to GooglePlay Services is successfully connected
     * @param bundle additional information is provided about the connection
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MYLOG", "Connected to Google Play Services successfully! :)");
    }

    /**
     * GoogleApiClient.OnConnectionCallbacks INTERFACE
     * Callback for whenever the connection to GooglePlay Services is suspended
     * @param i a constant indicating the cause
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.i("MYLOG", "The connection to Google Play Services was suspended!");
    }

    /**
     * GoogleApiClient.OnConnectionFailedListener INTERFACE
     * Callback for whenever the connection to GooglePlay Services fails
     * @param connectionResult information on the connection status
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("MYLOG", "Connection to Google Play Services FAILED!!");
    }

    /**
     * LocationLister INTERFACE
     * Callback executed when location changes and the location is updated.
     * @param location new location of the smart phone
     */
    @Override
    public void onLocationChanged(Location location) {
        // Then, we update our location object:
        this.location = location;
        Log.i("MYLOG", "Location updated! (" + location.toString()+")");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("NearestPlaceNotification")) {
            if (!sharedPreferences.getBoolean(key, false)) {
                Log.i("MYLOG", "Now, the notification every minute should be turned off " + sharedPreferences.getBoolean(key,false));
                if (serviceIntent != null) {stopService(serviceIntent); serviceIntent = null;}
            }
        } else if (key.equals("coverDistance")) {
            sortingDistance = Integer.valueOf(sharedPreferences.getString(key, "50000")); // 50000m = 50KM. Within 50KM around (a la redonda). Sorting distance to sort out the Places list.
        }

    }
}
