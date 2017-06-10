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

public class CreateEditSongActivity extends AppCompatActivity {

    public static final int TAKE_A_PICTURE = 0;
    public static final int SELECT_CONTACT = 1;
    public static final String PLACE_EDIT = "PLACE_EDIT";
    private static final int  MY_PERMISSIONS_CAMERA = 1; // 1 doesn't mean True, it's just an ID.
    private static final int MY_PERMISSIONS_EXTERNAL_STORAGE = 2; // 2 doesn't mean anything, it's just an ID.
    private String currentPicPath;
    // If you want to implement Up Navigation, check this out: https://developer.android.com/training/implementing-navigation/ancestral.html

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_create_song);

        // Now we should check whether ths activity was called because the user wanted to CREATE a new Song or EDIT it.
        // The reason behind this is that to edit the place, "CreateEditSongActivity" activity is also used for the same purpose.
        Song songToEdit = (Song) getIntent().getSerializableExtra(PLACE_EDIT);
        if (songToEdit != null) {
            setTitle("Edit Song");
            EditText et_name = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_name);
            EditText et_lyric = (EditText) findViewById(R.id.et_lyric);
            EditText et_description = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_desc);
            et_name.setText(songToEdit.getName());
            et_lyric.setText(songToEdit.getLyric());
            et_description.setText(songToEdit.getDescription());
            // TODO: retrieve also the picture and show it
        }

        // OnClick listeners:
        ImageButton imgb = (ImageButton) findViewById(es.deusto.arduinosinger.R.id.create_place_img);
        imgb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
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
                    EditText et_lyric = (EditText) findViewById(R.id.et_lyric);
                    EditText et_description = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_desc);
                Intent intentResult = new Intent();
                intentResult.putExtra("place", new Song(et_name.getText().toString(), et_lyric.getText().toString(), et_description.getText().toString()));
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

    // Life cycle of the activity:

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
