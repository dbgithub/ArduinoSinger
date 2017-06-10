package es.deusto.arduinosinger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class SongDetailsActivity extends AppCompatActivity {

    public static final String SONG_DETAILS = "SONG_DETAILS";
    public static final int EDIT_SONG = 0; // ID for EditPlace Intent
    private Song tmpP;
    private boolean fieldsUpdated = false;
    private String lyric;
    // BLUETOOTH:
    public static final int REQUEST_ENABLE_BT = 1; // ID for request BT Intent
    private BroadcastReceiver mBroadcastReceiver1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket;
    private boolean isConnected = false;

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
                if (!mBluetoothAdapter.isEnabled()) {
                    Log.i("MYLOG", "Enabling BT...");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    // BT is already enabled! We continue as expected.
                    // But, one again, we check if the smartphone was connected eventually to the BT module of Arduino, otherwise it will
                    // raise an exception because no song can be transmitted.
                    if (isConnected) {messageDealer();} else {
                        Toast.makeText(SongDetailsActivity.this, R.string.toast_not_connected_yet, Toast.LENGTH_LONG).show();
                    }
                }

                // TODO: quitar...
//                Intent editPlaceIntent = new Intent(getBaseContext(), CreateEditSongActivity.class);
//                editPlaceIntent.putExtra(CreateEditSongActivity.PLACE_EDIT, tmpP);
//                startActivityForResult(editPlaceIntent, EDIT_SONG);
            }
        });

        tmpP = (Song)getIntent().getSerializableExtra("SONG_DETAILS");
        updateFields();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            bindBroadcastReceiver();
            if (!mBluetoothAdapter.isEnabled()) {
                Log.i("MYLOG", "Enabling BT...");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // BT is already enabled! We continue as expected:
                checkSyncedDevices();
            }
        }
    }

    // Find out more about to which UUID connect: https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord(java.util.UUID)
    private void checkSyncedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.i("MYLOG", "Discovered device: "  +device.getName() + "\t" + device.getAddress());
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if (device.getAddress().equals("20:14:05:19:29:69")) {
                    try {
                        // A Bluetooth socket is created with a specific UUID used for Serial communication
                        btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        new establishConnectionToBTmodule().execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i("MYLOG", "Connected to "+device.getName()+"'s BT Socket!");
                }
            }
        }
        //mBluetoothAdapter.startDiscovery(); // remember calling 'cancelDiscovery' !!!
        //Log.i("MYLOG", "BT discovery started...!");
    }

    /**
     * Retrieves the output stream required for sending data to HC-06 module. InputStream is not necessary this the BT module is only slave.
     */
    private void messageDealer() {
        OutputStream outStream = null;
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) { }

        byte[] msgBuffer = lyric.getBytes(); //converts entered String into bytes
        write(outStream, msgBuffer);
        Log.i("MYLOG", "Lyric message sent!");
        FloatingActionButton fab = (FloatingActionButton) findViewById(es.deusto.arduinosinger.R.id.fab);
        Snackbar.make(fab, "Playing song...! :) ENJOY", Snackbar.LENGTH_LONG).setAction("Action", null).show();

    }

    /* Call this from the main activity to send data to the remote device */
    public void write(OutputStream ons, byte[] bytes) {
        try {
            ons.write(bytes);
        } catch (IOException e) {
            Log.i("MYLOG", "An exception occured!");
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            btSocket.close();
        } catch (IOException e) { }
    }

    /**
     * Convenience class to connect to BT module and to avoid blocking UI elements
     */
    private class establishConnectionToBTmodule extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            Log.i("MYLOG", "AsynTask started... (doing in background)!");
            try {
                btSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.i("MYLOG", "Connecting to HC-06............ "+values[0]+"%");
//            barConnectivity.setProgress(values[0].intValue());
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("MYLOG", "AsynTask finished (onPostExecute)!");
            isConnected = true;
            Toast.makeText(SongDetailsActivity.this, R.string.toast_connected_successfully, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } else if (requestCode == REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                // The user enabled BT
                Log.i("MYLOG", "User enabled BT successfully!");
                checkSyncedDevices();
            } else {
                Log.i("MYLOG", "User declined to enable BT!");
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
        lyric = tmpP.getLyric();

        // Header image for toolbar:
        ImageView headerImage = (ImageView) findViewById(R.id.CollapsingToolbarLayoutImage);
        String uri = "@drawable/" + tmpP.getImageName(); // Building the URI for every image.
        int imageResourceID = getResources().getIdentifier(uri, null, getPackageName()); // Get the ID of the image resource given the URI
        // Loading (large) bitmaps efficiently (more info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html):
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(),imageResourceID, options);
        headerImage.setImageBitmap(decodeSampledBitmapFromResource(getResources(), imageResourceID, 300, 300));
    }

    private void bindBroadcastReceiver() {
        Log.i("MYLOG", "Binding broadcast receiver for BT...!");
        mBroadcastReceiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch(state) {
                        case BluetoothAdapter.STATE_OFF:
                            Log.i("MYLOG", "BT is OFF!");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.i("MYLOG", "Turning BT OFF...");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.i("MYLOG", "BT is ON...");
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.i("MYLOG", "Turning BT ON...");
                            break;
                    }
                } else if (action.equals(BluetoothDevice.ACTION_FOUND)) { // This part will be executed when a device is found due to 'startDiscovery'
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    Log.i("MYLOG", "Discovered device: "  +device.getName() + "\t" + device.getAddress());
                    //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    if (device.getAddress().equals("20:14:05:19:29:69")) {
                        mBluetoothAdapter.cancelDiscovery(); // cancel discovery and connect to device
                        Log.i("MYLOG", "BT discovery cancelled!");
                    }
                }
            }
        };
        // Register the BroadcastReceiver for ACTION_STATE_CHANGED:
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);
        Log.i("MYLOG", "Broadcast receiver 'state_changed' binding OK!");
        // Register the BroadcastReceiver for ACTION_FOUND (for whenever 'start_discovery' is called):
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver1, filter);
        Log.i("MYLOG", "Broadcast receiver 'action_found' binding OK!");
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
