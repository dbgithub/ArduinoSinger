package es.deusto.arduinosinger;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SongDetailsActivity extends AppCompatActivity {

    public static final String SONG_DETAILS = "SONG_DETAILS";
    public static final int EDIT_SONG = 0; // ID for EditPlace Intent
    private Song tmpP;
    private boolean fieldsUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_song_details);
        Toolbar toolbar = (Toolbar) findViewById(es.deusto.arduinosinger.R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(es.deusto.arduinosinger.R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editPlaceIntent = new Intent(getBaseContext(), CreateEditSongActivity.class);
                editPlaceIntent.putExtra(CreateEditSongActivity.PLACE_EDIT, tmpP);
                startActivityForResult(editPlaceIntent, EDIT_SONG);
            }
        });

        tmpP = (Song)getIntent().getSerializableExtra("SONG_DETAILS");
        updateFields();
    }

    @Override
    public void onBackPressed() {
        if (fieldsUpdated) {
            Log.i("INTENT!", "fieldsUpdate is TRUE, onBackPressed executed!");
            Intent myintent = new Intent();
            myintent.putExtra("place", tmpP);
            setResult(Activity.RESULT_OK, myintent);
            fieldsUpdated = false;
        }
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_SONG){ // If the Intent code was "EDIT_SONG", then we update the information on the screen
            if(resultCode == Activity.RESULT_OK){
                // Normally you'd do: "data.getStringExtra" or similar. But in this case we are retrieving a Serializable object
                tmpP = (Song) data.getSerializableExtra("place");
                updateFields();
                fieldsUpdated = true; // We indicate that an update was performed
            }
        }
    }

    /**
     * Populates the Object passed from the list of Songs OR updates the information after the Song was edited.
     * In either way, all fields will be rewritten.
     */
    private void updateFields() {
        Log.i("TRAZA", "updateFields() llamado!!!");
        CollapsingToolbarLayout tb = (CollapsingToolbarLayout)findViewById(es.deusto.arduinosinger.R.id.toolbar_layout);
        TextView tv_description = (TextView) findViewById(es.deusto.arduinosinger.R.id.tv_description);
        tb.setTitle(tmpP.getName());
        tv_description.setText(tmpP.getDescription());

        // Header image for toolbar:
        ImageView headerImage = (ImageView) findViewById(R.id.CollapsingToolbarLayoutImage);
        String uri = "@drawable/" + tmpP.getImageName(); // Building the URI for every image.
        int imageResourceID = getResources().getIdentifier(uri, null, getPackageName()); // Get the ID of the image resource given the URI
        // Loading (large) bitmaps efficiently (more info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html):
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(),imageResourceID, options);
        headerImage.setImageBitmap(decodeSampledBitmapFromResource(getResources(), imageResourceID, 300, 300));
//        headerImage.setImageResource(R.drawable.header);
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
