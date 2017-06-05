package es.deusto.arduinosinger;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CreateEditPlaceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    public static final int TAKE_A_PICTURE = 0;
    public static final int SELECT_CONTACT = 1;
    public static final String PLACE_EDIT = "PLACE_EDIT";
    private static final int  MY_PERMISSIONS_CAMERA = 1; // 1 doesn't mean True, it's just an ID.
    private static final int MY_PERMISSIONS_EXTERNAL_STORAGE = 2; // 2 doesn't mean anything, it's just an ID.
    private static final int MY_PERMISSIONS_FINE_LOCATION = 3; // 3 doesn't mean anything, it's just an ID.
    private String currentPicPath;
    private ArrayList<Contact> arraylContacts = new ArrayList<>();
    private ArrayAdapter<Contact> arrayadapContacts;
    private GoogleApiClient myGoogleApiClient; // For accessing Google Play Services
    private Location location = null; // For storing current location
    // If you want to implement Up Navigation, check this out: https://developer.android.com/training/implementing-navigation/ancestral.html

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_create_place);
        myGoogleApiClient = ListPlacesActivity.myGoogleApiClient;

        // Now we should check whether ths activity was called because the user wanted to CREATE a new Place or EDIT it.
        // The reason behind this is that to edit the place, "CreatePaceActivity" activity is also used for the same purpose.
        Place placeToEdit = (Place) getIntent().getSerializableExtra(PLACE_EDIT);
        if (placeToEdit != null) {
            setTitle("Edit Place");
            EditText et_name = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_name);
            EditText et_neighborhood = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_neighborhood);
            EditText et_description = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_desc);
            et_name.setText(placeToEdit.getName());
            et_neighborhood.setText(placeToEdit.getNeighborhood());
            et_description.setText(placeToEdit.getDescription());
            updateLongitudeLatitude(placeToEdit.getLongitude(), placeToEdit.getLatitude());
            arraylContacts = placeToEdit.getlContacts();
            // TODO: retrieve also the picture and show it
        } else {
            // Depending on an Settings option, the coordinates fields will either be filled automatically or not.
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            Boolean settingsPreference_automaticFill = sharedPref.getBoolean("AutomaticFillUp", false);
            if (settingsPreference_automaticFill) {checkLocationPermission();}
        }
        arrayadapContacts = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_2, android.R.id.text1, arraylContacts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(arraylContacts.get(position).getName());
                text2.setText(arraylContacts.get(position).getPhoneNumber());
                return view;
            }
        };
        ListView lv_associatedContacts = (ListView) findViewById(es.deusto.arduinosinger.R.id.listView);
        lv_associatedContacts.setAdapter(arrayadapContacts);

        // OnClick listeners:
        ImageButton imgb = (ImageButton) findViewById(es.deusto.arduinosinger.R.id.create_place_img);
        imgb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });

        Button btn_gps = (Button) findViewById(es.deusto.arduinosinger.R.id.btn_gps);
        btn_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermission();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(es.deusto.arduinosinger.R.menu.menu_create_place, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case es.deusto.arduinosinger.R.id.mnu_save:
                // We will create a new Intent which will be passed back to the parent activity.
                // Is is necessary to set the result code.
                    // Retrieving the text from text fields:
                    EditText et_name = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_name);
                    EditText et_neighborhood = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_neighborhood);
                    EditText et_description = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_desc);
                    EditText et_longitude = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_longitude);
                    EditText et_latitude = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_latitude);
                Intent intentResult = new Intent();
                intentResult.putExtra("place", new Place(et_name.getText().toString(), et_neighborhood.getText().toString(), et_description.getText().toString(), arraylContacts, (et_longitude.getText().toString().isEmpty()) ? 0.0 : Double.valueOf(et_longitude.getText().toString()), (et_latitude.getText().toString().isEmpty()) ? 0.0 : Double.valueOf(et_latitude.getText().toString())));
                setResult(Activity.RESULT_OK, intentResult);
                finish();
                break;
            case es.deusto.arduinosinger.R.id.mnu_cancel:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
            case es.deusto.arduinosinger.R.id.mnu_addContact:
                Intent intent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, SELECT_CONTACT); // El Select_contact es una variable que nosotros hemos definido
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_A_PICTURE){
            if(resultCode == Activity.RESULT_OK){
                galleryAddPic();
                // TODO show the image in the ImageView!
            }
        } else if (requestCode == SELECT_CONTACT) {
            if(resultCode == Activity.RESULT_OK){
                Cursor cursor = getContentResolver().query(data.getData(),
                        new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                        null,
                        null,
                        null);
                if(cursor.moveToNext()) {
                    Log.i("Contact was picked up:", data.getDataString());
                    arraylContacts.add(new Contact(cursor.getString(0), cursor.getString(1)));
                    arrayadapContacts.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_CAMERA:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Now we check another permission:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        // Permission DENIED!
                        // So, we request him/her to grant the permission for that purpose:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_EXTERNAL_STORAGE);
                    } else {
                        // Permission GRANTED! (or not needed to ask)
                        // Here goes the implementation of the camera functionality and pictures:
                        takePicture();
                    }
                }
                break;
            case MY_PERMISSIONS_EXTERNAL_STORAGE:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Here goes the implementation of the camera functionality and pictures:
                    takePicture();
                }
                break;
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

    /**
     * In order to launch the camera, it is mandatory to check whether the permission was granted or not.
     * The CAMERA permissions is a dangerous one from API 23 onwards. Below API 23, there is no need to check
     * for permissions since the latter should have been granted when installing the app by the user.
     * So, if current build version > API 23, THEN => we should check for permission. ELSE, no need to check anything.
     * After checking for CAMERA permission, it is mandatory also to check WRITE PERMISSIONS.
     */
    private void launchCamera() {
        // More info about capturing a picture using phone's hardware camera: https://developer.android.com/training/camera/photobasics.html
        // Detailed information about requesting the required permissions when needed: https://developer.android.com/training/permissions/requesting.html

        // Firstly, check whether the user has granted the permission requested:
        if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // Permission DENIED!
            // So, we request him/her to grant the permission for that purpose:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_CAMERA);
        } else {
            // Permission GRANTED! (or not needed to ask)

            // Now we check another permission (write permission):
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                // Permission DENIED!
                // So, we request him/her to grant the permission for that purpose:
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_EXTERNAL_STORAGE);
            } else {
                // Permission GRANTED! (or not needed to ask)

                // So, we continue as planned...
                takePicture();
            }
        }
    }

    /**
     * After having checked current API version as well as either the permission was granted or not, it's time now
     * to take the picture accordingly.
     */
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) { // StartActivityForResult is protected under a condition to check whether there exist a hardware camera that will handle the Intent (this IF is for that purpose).
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Create the File where the photo should go
            } catch (IOException ex) {
                Log.e("ERROR", "Error while creating the File for the picture!");
            }
            if (photoFile != null) {
                Uri photoURI;
                // For more recent apps targeting Android 7.0 (API level 24) and higher, calling 'getUriForFile' causes a FileUriExposedException because it returns " file:// URI" instead of "content:// URI", FileProvider is needed!
                if (Build.VERSION.SDK_INT > 24) {
                        photoURI = FileProvider.getUriForFile(this, "es.deusto.arduinosinger.fileprovider", photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, TAKE_A_PICTURE);
            }
        }
    }

    /**
     * Creates a File in the External File Directory with a given name based on a date-time stamp.
     * In summary, this is the location where the picture will go to.
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "place_" + timeStamp + "_";
        //File storageDir = getFilesDir();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.i("MYLOG","EL STORAGE DIR ES: " + storageDir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // We capture the path for later use
        currentPicPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        /* This way doesn't really work, I don't know why:
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPicPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        */
        // This way, you store the desired metada you want in the picture, afterwards, the image is saved in the public gallery.
        Log.i("MYLOG","El image.getAbsolutePath: " + currentPicPath);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put("_data",currentPicPath) ;
        ContentResolver c = getContentResolver();
        c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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

        // Now, we assign values to Longitude and Latitude fields in the GUI:
        if (location != null) {updateLongitudeLatitude(location.getLongitude(), location.getLatitude());} else {Log.i("MYLOG", "Not possible to update location. Location object is NULL! :(");}
    }

    /**
     * Updates Longitude and Latitude text fields in the UI:
     */
    private void updateLongitudeLatitude(double lon, double lat) {
        EditText et_longitude = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_longitude);
        EditText et_latitude = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_latitude);
        et_longitude.setText(Double.toString(lon));
        et_latitude.setText(Double.toString(lat));
    }

    // Life cycle of the activity:

    @Override
    protected void onStart() {
        super.onStart();
        // Start the connection to Google Play Services (a callback with the status of the connection will be received)
        connectToGooglePlayServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        myGoogleApiClient =  new GoogleApiClient.Builder(CreateEditPlaceActivity.this)
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
}
