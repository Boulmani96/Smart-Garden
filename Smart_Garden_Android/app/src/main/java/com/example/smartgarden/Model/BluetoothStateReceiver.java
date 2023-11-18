package com.example.smartgarden.Model;

import static com.example.smartgarden.MainActivity.REQUEST_ENABLE_BT;
import static com.example.smartgarden.MainActivity.requestEnableBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

/**
 * listen for any changes in the state of the BluetoothAdapter
 */
public class BluetoothStateReceiver extends BroadcastReceiver {

    Context context;

    public BluetoothStateReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF) {//Prompt the user to turn on Bluetooth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(context, "Bluetooth is not enabled !", Toast.LENGTH_SHORT).show();
                    requestEnableBluetooth(context, REQUEST_ENABLE_BT);
                }
            }
        }
    }
}
