package com.example.smartgarden;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.smartgarden.Adapter.BluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.List;


public class DeviceScanActivity extends ListActivity {

    ArrayList<BluetoothDevice> bluetoothDevices;

    private static DeviceScanActivity instance;

    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothAdapter bluetoothAdapter;

    private final BluetoothManager bluetoothManager;

    private boolean scanning;

    private final Handler handler = new Handler();

    // Stops scanning after 10 seconds.
    private BluetoothDeviceAdapter bluetoothDeviceAdapter;

    Context context;

    private DeviceScanActivity(Context context) {
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.context = context;
    }

    public static DeviceScanActivity getInstance(Context context) {
        if (instance == null)
            instance = new DeviceScanActivity((context));
        return instance;
    }

    public void init(Context context) {
        // Initializes a Bluetooth adapter. For API level 18 and above,
        // get a reference to BluetoothAdapter through BluetoothManager.
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothDevices = new ArrayList<>();
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(context, bluetoothDevices);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        return bluetoothLeScanner;
    }

    public void setBluetoothLeScanner(BluetoothLeScanner bluetoothLeScanner) {
        this.bluetoothLeScanner = bluetoothLeScanner;
    }

    public BluetoothDeviceAdapter getBluetoothDeviceAdapter() {
        return bluetoothDeviceAdapter;
    }

    private static final long SCAN_PERIOD = 10000;
    public void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            if (isBluetoothAvailable()) {
                handler.postDelayed(() -> {
                    scanning = false;
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                                != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        bluetoothLeScanner.stopScan(leScanCallback);

                }, SCAN_PERIOD);
                if (bluetoothLeScanner != null){
                    scanning = true;
                    bluetoothLeScanner.startScan(leScanCallback);
                }else {
                    init(context);
                }
            }
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /**
     * The interface used to deliver BLE scan results.
     */
    @SuppressLint("NewApi")
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //When results are found, they are added to a list adapter
            if (isBluetoothAvailable()) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                for(BluetoothDevice device : devices) {
                    if(device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                        bluetoothDeviceAdapter.addDevice(device);
                        bluetoothDeviceAdapter.notifyDataSetChanged();
                    }
                }
                bluetoothDeviceAdapter.addDevice(result.getDevice());
                bluetoothDeviceAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult scanResult : results) {
                BluetoothDevice device;
                device = scanResult.getDevice();
                if (!bluetoothDevices.contains(device)) {
                    bluetoothDeviceAdapter.addDevice(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("BLE", "scan failed with error code: " + errorCode);
        }
    };

    /**
     * check if Bluetooth is available
     * @return true is bluetoothAdapter is not null and bluetooth is on else false
     */
    public boolean isBluetoothAvailable() {
        return (bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled() &&
                bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON);
    }
}
