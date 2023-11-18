package com.example.smartgarden;

import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_HUMIDITY;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_LDR_PHOTORESISTOR;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_LED_STATUS;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_SOIL_MOISTURE;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_TEMPERATURE;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_WATER_LEVEL;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DATA_WATER_PUMP_STATUS;
import static com.example.smartgarden.BluetoothLeService.EXTRA_DEVICE_SERVICE;
import static com.example.smartgarden.MainActivity.PERMISSION_REQUEST_CODE;
import static com.example.smartgarden.MainActivity.REQUEST_ENABLE_BT;
import static com.example.smartgarden.MainActivity.deviceScanActivity;
import static com.example.smartgarden.MainActivity.requestEnableBluetooth;
import static com.example.smartgarden.MainActivity.spinner_devices;
import static com.example.smartgarden.MainActivity.updateConnectionState;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartgarden.Model.BluetoothStateReceiver;
import com.scwang.wave.MultiWaveHeader;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class HomeFragment extends Fragment implements BluetoothLeServiceListener{

    public static final String TAG = "BluetoothLeService";
    private BluetoothStateReceiver bluetoothStateReceiver;

    private Context mContext;

    private BluetoothLeService bluetoothService;

    String mDeviceAddress;

    private TextView textViewAirHumidityValue, textAirTemperatureValue, lightTextStatus, lightTextValue, textSoilMoistureValue, waterPumpTextStatus, waterLevelStatus;

    private SwitchCompat lightSwitch, waterPumpSwitch;

    private LocationManager locationManager;

    private BluetoothGattService mCustomService;

    private BluetoothGatt gatt;

    ProgressBar waterProgressBar;

    MultiWaveHeader multiWaveHeader;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("MissingPermission")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bluetoothService = new BluetoothLeService();

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        textViewAirHumidityValue = view.findViewById(R.id.textAirHumidityValue);
        textAirTemperatureValue = view.findViewById(R.id.textAirTemperatureValue);
        lightTextStatus = view.findViewById(R.id.lightTextStatus);
        lightTextValue = view.findViewById(R.id.lightTextValue);
        textSoilMoistureValue = view.findViewById(R.id.textSoilMoistureValue);
        waterPumpTextStatus = view.findViewById(R.id.waterPumpTextStatus);
        waterLevelStatus = view.findViewById(R.id.waterLevelStatus);

        waterProgressBar = view.findViewById(R.id.progressBarWaterLevel);
        multiWaveHeader = view.findViewById(R.id.waveHeader);

        ViewGroup.LayoutParams params = multiWaveHeader.getLayoutParams();
        params.height = (int) ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, waterProgressBar.getProgress(), getResources().getDisplayMetrics())
                - (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics())));
        multiWaveHeader.setLayoutParams(params);

        // Create a Handler object
        Handler handler = new Handler();

        lightSwitch = view.findViewById(R.id.lightSwitch);

        // Create a Runnable to turn off the switch after 3 seconds
        Runnable runnable = () -> lightSwitch.setChecked(false);

        lightSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(UUID.fromString("50fdb428-6ce1-11ee-b962-0242ac120002"));
            if (b){
                if (characteristic != null) {
                    String dataToSend = "ON";
                    byte[] dataBytes = dataToSend.getBytes(StandardCharsets.UTF_8);
                    characteristic.setValue(dataBytes);
                    gatt.writeCharacteristic(characteristic);
                    lightTextStatus.setText(R.string.homeFragmentTextON);
                    lightTextStatus.setTextColor(getResources().getColor(R.color.green));
                }
                // Use the Handler to delay the Runnable by 3 seconds
                handler.postDelayed(runnable, 3000); // 3000 milliseconds = 3 seconds
            }else{
                if (characteristic != null) {
                    String dataToSend = "OFF";
                    byte[] dataBytes = dataToSend.getBytes(StandardCharsets.UTF_8);
                    characteristic.setValue(dataBytes);
                    gatt.writeCharacteristic(characteristic);
                    lightTextStatus.setText(R.string.homeFragmentTextOFF);
                    lightTextStatus.setTextColor(getResources().getColor(R.color.red));
                }
            }
        });

        waterPumpSwitch = view.findViewById(R.id.waterPumpSwitch);
        Runnable runnable1 = () -> waterPumpSwitch.setChecked(false);
        waterPumpSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(UUID.fromString("54da7e24-7005-11ee-b962-0242ac120002"));
            if (b){
                if (characteristic != null) {
                    String dataToSend = "ON";
                    byte[] dataBytes = dataToSend.getBytes(StandardCharsets.UTF_8);
                    characteristic.setValue(dataBytes);
                    gatt.writeCharacteristic(characteristic);
                    waterPumpTextStatus.setText(R.string.homeFragmentTextON);
                    waterPumpTextStatus.setTextColor(getResources().getColor(R.color.green));
                }
                // Use the Handler to delay the Runnable by 3 seconds
                handler.postDelayed(runnable1, 3000); // 3000 milliseconds = 3 seconds
            }else{
                if (characteristic != null) {
                    String dataToSend = "OFF";
                    byte[] dataBytes = dataToSend.getBytes(StandardCharsets.UTF_8);
                    characteristic.setValue(dataBytes);
                    gatt.writeCharacteristic(characteristic);
                    waterPumpTextStatus.setText(R.string.homeFragmentTextOFF);
                    waterPumpTextStatus.setTextColor(getResources().getColor(R.color.red));
                }
            }
        });

        spinner_devices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle item selection
                mDeviceAddress = deviceScanActivity.getBluetoothDeviceAdapter().getDevice(position).getAddress();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Register for broadcasts on BluetoothAdapter state change
        bluetoothStateReceiver = new BluetoothStateReceiver(mContext);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        requireActivity().registerReceiver(bluetoothStateReceiver, filter);

        // Register the receiver to listen for the Bluetooth events
        IntentFilter filterBluetooth = new IntentFilter();
        filterBluetooth.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBluetooth.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        requireActivity().registerReceiver(broadcastReceiverBleEvents, filterBluetooth);

        return view;
    }

    @Override
    public void onBluetoothGattServiceReady(BluetoothGattService gattService) {
        mCustomService = gattService;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister broadcast listeners
        if (bluetoothStateReceiver != null) {
            requireActivity().unregisterReceiver(bluetoothStateReceiver);
            bluetoothStateReceiver = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // if permissions ACCESS FINE LOCATION & BLUETOOTH_SCAN & BLUETOOTH_CONNECT already are granted
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {

            // Listen for the connection and disconnection events,
            // and a flag to specify additional connection options.
            Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
            requireActivity().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check Bluetooth state
                checkBluetoothState(mContext);
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkPermissions(mContext);
            }
        }
    }

    @Override
    public void onResume() {
        requireActivity().registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothService != null
                && bluetoothService.getBluetoothAdapter() != null
                && bluetoothService.getBluetoothAdapter().isEnabled()
                && bluetoothService.getBluetoothAdapter().getState() == BluetoothAdapter.STATE_ON) {
            final boolean result = bluetoothService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.inflateMenu(R.menu.toolbar_menu);
                toolbar.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_connect){
                        if (mDeviceAddress != null){
                            if (bluetoothService != null){
                                bluetoothService.connect(mDeviceAddress);
                            }
                        }
                    }

                    if (itemId == R.id.menu_disconnect){
                        assert bluetoothService != null;
                        bluetoothService.disconnect();
                    }

                    if (itemId == R.id.menu_scan){
                        checkLocationState();
                        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            deviceScanActivity.scanLeDevice();
                        }
                    }
                    return true;
                });
            }
        }
    }

    private void checkLocationState(){
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is disabled, show an alert dialog to ask the user to enable it
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services to scan bluetooth devices");
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                // Show location settings page
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
            builder.setNegativeButton("Cancel", null);
            builder.create().show();
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                // call functions on service to check connection and connect to devices
                if (!bluetoothService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    requireActivity().finish();
                }

                bluetoothService.getBluetoothAdapter().getProfileProxy(mContext, listener, BluetoothProfile.A2DP);
                bluetoothService.setBluetoothLeServiceListener(HomeFragment.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                // Get the list of connected devices
                List<BluetoothDevice> devices = proxy.getConnectedDevices();
                if (!devices.isEmpty()) {
                    // Get the first connected device, which should be the only connected device if only one is connected
                    BluetoothDevice device = devices.get(0);
                    mDeviceAddress = device.getAddress();
                    // perform device connection
                    if (bluetoothService != null){
                        bluetoothService.connect(mDeviceAddress);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            // Called when the Bluetooth service has been disconnected
        }
    };

    private void displayData(final Intent intent) {
        getActivity().runOnUiThread(() -> {
            String temperature = intent.getStringExtra(EXTRA_DATA_TEMPERATURE);
            String humidity = intent.getStringExtra(EXTRA_DATA_HUMIDITY);
            String ledStatus = intent.getStringExtra(EXTRA_DATA_LED_STATUS);
            String LDR_Photoresistor = intent.getStringExtra(EXTRA_DATA_LDR_PHOTORESISTOR);
            String soilMoisture = intent.getStringExtra(EXTRA_DATA_SOIL_MOISTURE);
            String waterPumpStatus = intent.getStringExtra(EXTRA_DATA_WATER_PUMP_STATUS);
            String waterLevel = intent.getStringExtra(EXTRA_DATA_WATER_LEVEL);

            if (temperature != null) {
                textAirTemperatureValue.setText(temperature);
            }

            if (humidity != null) {
                textViewAirHumidityValue.setText(humidity);
            }

            if (ledStatus != null) {
                if (ledStatus.equals("ON")){
                    lightTextStatus.setText(R.string.homeFragmentTextON);
                    lightTextStatus.setTextColor(getResources().getColor(R.color.green));
                    lightSwitch.setChecked(true);
                }else{
                    if (ledStatus.equals("OFF")){
                        lightTextStatus.setText(R.string.homeFragmentTextOFF);
                        lightTextStatus.setTextColor(getResources().getColor(R.color.red));
                        lightSwitch.setChecked(false);
                    }
                }
            }

            if (waterPumpStatus != null) {
                if (waterPumpStatus.equals("ON")){
                    waterPumpTextStatus.setText(R.string.homeFragmentTextON);
                    waterPumpTextStatus.setTextColor(getResources().getColor(R.color.green));
                    waterPumpSwitch.setChecked(true);
                }else{
                    if (waterPumpStatus.equals("OFF")){
                        waterPumpTextStatus.setText(R.string.homeFragmentTextOFF);
                        waterPumpTextStatus.setTextColor(getResources().getColor(R.color.red));
                        waterPumpSwitch.setChecked(false);
                    }
                }
            }

            if (LDR_Photoresistor != null){
                lightTextValue.setText(LDR_Photoresistor);
            }

            if (soilMoisture != null) {
                textSoilMoistureValue.setText(soilMoisture);
            }

            if (waterLevel != null) {
                waterLevelStatus.setText(waterLevel);
                if (waterLevel.equals("Empty")) {
                    waterLevelStatus.setTextColor(getResources().getColor(R.color.red));
                    waterProgressBar.setProgress(0);
                }else{
                    if(waterLevel.equals("Low")){
                        waterLevelStatus.setTextColor(getResources().getColor(R.color.goldenrod));
                        waterProgressBar.setProgress(25);
                    }else{
                        if(waterLevel.equals("Medium")){
                            waterLevelStatus.setTextColor(getResources().getColor(R.color.orange));
                            waterProgressBar.setProgress(50);
                        }else{
                            if(waterLevel.equals("High")){
                                waterLevelStatus.setTextColor(getResources().getColor(R.color.green));
                                waterProgressBar.setProgress(75);
                            }else{
                                if(waterLevel.equals("Full")){
                                    waterLevelStatus.setTextColor(getResources().getColor(R.color.pale_blue));
                                    waterProgressBar.setProgress(100);
                                }
                            }
                        }
                    }
                }
                ViewGroup.LayoutParams params = multiWaveHeader.getLayoutParams();
                params.height = (int) ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, waterProgressBar.getProgress(), getResources().getDisplayMetrics())
                        - (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics())));
                multiWaveHeader.setLayoutParams(params);
            }
        });
    }

    private void setTextViewValue() {
        textAirTemperatureValue.setText("--");
        textViewAirHumidityValue.setText("--");
        lightTextStatus.setText("--");
        lightTextValue.setText("--");
        textSoilMoistureValue.setText("--");
        waterPumpTextStatus.setText("--");
        lightSwitch.setEnabled(false);
        waterPumpSwitch.setEnabled(false);
        waterLevelStatus.setText("--");
        waterProgressBar.setProgress(0);
        ViewGroup.LayoutParams params = multiWaveHeader.getLayoutParams();
        params.height = (int) ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, waterProgressBar.getProgress(), getResources().getDisplayMetrics())
                - (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics())));
        multiWaveHeader.setLayoutParams(params);
    }

    /**
     * Register the receiver to listen for the Bluetooth events
     */
    private final BroadcastReceiver broadcastReceiverBleEvents = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.i(TAG, "ACTION_ACL_CONNECTED");
                // A Bluetooth device has been connected, get the name and address of the connected device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    mDeviceAddress = device.getAddress();
                    if (bluetoothService != null) {
                        // perform device connection
                        boolean connected = bluetoothService.connect(mDeviceAddress);
                        if (connected) {
                            BluetoothDevice bluetoothDevice = bluetoothService.getBluetoothAdapter().getRemoteDevice(mDeviceAddress);
                        }
                    }
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.i(TAG, "ACTION_ACL_DISCONNECTED");
               // BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //deviceScanActivity.getBluetoothDeviceAdapter().deleteDevice(disconnectedDevice);
               // MainActivity.setSpinnerView();
                updateConnectionState(false);
            }
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "ACTION_GATT_CONNECTED");
                gatt = bluetoothService.getBluetoothGatt();
                MainActivity.isConnected = true;
                updateConnectionState(true);
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE_SERVICE);
                if (device != null) {
                    deviceScanActivity.getBluetoothDeviceAdapter().addDevice(device);
                    spinner_devices.setSelection(deviceScanActivity.bluetoothDevices.indexOf(device));
                    lightSwitch.setEnabled(true);
                    waterPumpSwitch.setEnabled(true);
                }

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "ACTION_GATT_DISCONNECTED");
                BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceScanActivity.getBluetoothDeviceAdapter().deleteDevice(disconnectedDevice);
                MainActivity.setSpinnerView();
                updateConnectionState(false);
                setTextViewValue();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * check bluetooth status and prompt the user to turn on Bluetooth if not.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static void checkBluetoothState(Context context) {
        if (deviceScanActivity.getBluetoothAdapter() == null) {
            Toast.makeText(context, "Bluetooth NOT support", Toast.LENGTH_SHORT).show();
        } else {
            if (deviceScanActivity.getBluetoothAdapter().isEnabled()) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Without Bluetooth scan permission, Bluetooth devices cannot be scanned!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_REQUEST_CODE);
                    return;
                }
                if (deviceScanActivity.getBluetoothAdapter().isDiscovering()) {
                    Toast.makeText(context, "Bluetooth is currently in device discovery process.", Toast.LENGTH_LONG).show();
                } else {
                    if (deviceScanActivity.getBluetoothLeScanner() == null) {
                        deviceScanActivity.setBluetoothLeScanner(deviceScanActivity.getBluetoothAdapter().getBluetoothLeScanner());
                    }
                    deviceScanActivity.scanLeDevice();
                }
            } else {
                Toast.makeText(context, "Bluetooth is not enabled !", Toast.LENGTH_SHORT).show();
                //Prompt the user to turn on Bluetooth
                requestEnableBluetooth(context, REQUEST_ENABLE_BT);
            }
        }
    }

    /**
     * check and request Bluetooth Permissions
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPermissions(Context context) {
        String[] PERMISSIONS = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE
        };

        if (!hasPermissions(context, PERMISSIONS)) {
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
                if (allPermissionsGranted) {
                    // All permissions granted. Start Bluetooth-related operations.
                    Toast.makeText(mContext, "All permissions granted", Toast.LENGTH_SHORT).show();
                    if (deviceScanActivity.isBluetoothAvailable() && locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        deviceScanActivity.scanLeDevice();
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        checkBluetoothState(mContext);
                    }
                    // Listen for the connection and disconnection events,
                    // and a flag to specify additional connection options.
                    Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
                    requireActivity().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                } else {
                    //Open the permission screen
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
    }
}