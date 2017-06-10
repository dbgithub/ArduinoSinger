package es.deusto.arduinosinger;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateEditSongActivity extends AppCompatActivity {

    public static final int TAKE_A_PICTURE = 0;
    public static final int SELECT_CONTACT = 1;
    public static final String SONG_EDIT = "SONG_EDIT";
    private static final int  MY_PERMISSIONS_CAMERA = 1; // 1 doesn't mean True, it's just an ID.
    private static final int MY_PERMISSIONS_EXTERNAL_STORAGE = 2; // 2 doesn't mean anything, it's just an ID.
    private String currentPicPath;
    private String imgName;
    // If you want to implement Up Navigation, check this out: https://developer.android.com/training/implementing-navigation/ancestral.html

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_create_song);

        // Now we should check whether ths activity was called because the user wanted to CREATE a new Song or EDIT it.
        // The reason behind this is that to edit the place, "CreateEditSongActivity" activity is also used for the same purpose.
        Song songToEdit = (Song) getIntent().getSerializableExtra(SONG_EDIT);
        if (songToEdit != null) {
            setTitle("Edit Song");
            EditText et_name = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_name);
            EditText et_lyric = (EditText) findViewById(R.id.et_lyric);
            EditText et_description = (EditText) findViewById(es.deusto.arduinosinger.R.id.et_desc);
            et_name.setText(songToEdit.getName());
            et_lyric.setText(songToEdit.getLyric());
            et_description.setText(songToEdit.getDescription());
            imgName = songToEdit.getImageName();
            // Retrieve and show image:
            ImageView imgView = (ImageView) findViewById(R.id.create_place_img);
            String uri = "@drawable/" + imgName; // Building the URI for every image.
            int imageResourceID = getResources().getIdentifier(uri, null, getPackageName()); // Get the ID of the image resource given the URI

            // Loading (large) bitmaps efficiently (more info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html):
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(),imageResourceID, options);
            imgView.setImageBitmap(decodeSampledBitmapFromResource(getResources(), imageResourceID, 85, 85));
        }

        // OnClick listeners:
        ImageButton imgb = (ImageButton) findViewById(es.deusto.arduinosinger.R.id.create_place_img2);
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
                intentResult.putExtra("place", new Song(et_name.getText().toString(), et_lyric.getText().toString(), et_description.getText().toString(), imgName));
                setResult(Activity.RESULT_OK, intentResult);
                finish();
                break;
            case es.deusto.arduinosinger.R.id.mnu_cancel:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
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

    /**
     * Returns the largest sample size that the picture can be given the height and width.
     * More info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Returns the bitmap after resizing it
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

}
