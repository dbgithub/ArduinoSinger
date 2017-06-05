package com.example.bt.testingbluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;

public class MainBluetooth extends AppCompatActivity {

    private static final int  MY_PERMISSIONS_BT = 1; // 1 doesn't mean True, it's just an ID.
    private static final int  MY_PERMISSIONS_BT_ADMIN = 2; // 1 doesn't mean True, it's just an ID.
    public static final int REQUEST_ENABLE_BT = 0; // ID for request BT Intent
    private BroadcastReceiver mBroadcastReceiver1;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bluetooth);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mBluetoothAdapter.cancelDiscovery(); // cancel discovery
                Log.i("MYLOG", "BT discovery cancelled!");
            }
        });

        // Checking for BT permissions:
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            // Permission DENIED!
            // So, we request him/her to grant the permission for that purpose:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_BT);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
            // Permission DENIED!
            // So, we request him/her to grant the permission for that purpose:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONS_BT_ADMIN);
        } else {
            // Permission GRANTED! (or not needed to ask)
            // So, we continue as planned...
            checkBTcompatibility();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_bluetooth, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_BT:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Now we check another permission:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
                        // Permission DENIED!
                        // So, we request him/her to grant the permission for that purpose:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONS_BT_ADMIN);
                    } else {
                        // Permission GRANTED! (or not needed to ask)
                        // Here we continue as expected:
                        checkBTcompatibility();
                    }
                }
                break;
            case MY_PERMISSIONS_BT_ADMIN:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Here we continue as expected:
                    checkBTcompatibility();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                // The user enabled BT
                Log.i("MYLOG", "User enabled BT successfully!");
                checkSyncedDevices();
            } else {
                Log.i("MYLOG", "User declined to enable BT!");
            }
        }
    }

    private void checkBTcompatibility() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.i("MYLOG", "BT is not supported in this device!");
        } else {
            Log.i("MYLOG", "BT is compatible with this device!");
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


    private void bindBroadcastReceiver() {
        Log.i("MYLOG", "Binding broadcast receiver for BT...!");
        BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
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
                } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
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
                        BluetoothSocket btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        btSocket.connect();
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

}
