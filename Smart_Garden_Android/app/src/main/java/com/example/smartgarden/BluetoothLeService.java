package com.example.smartgarden;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class BluetoothLeService extends Service {

    public static final String EXTRA_DEVICE_SERVICE = "DEVICE_CONNECTED_SERVICE";

    public final static UUID UUID_AIR_TEMPERATURE = UUID.fromString(SampleGattAttributes.AIR_TEMPERATURE);

    public final static UUID UUID_AIR_HUMIDITY = UUID.fromString(SampleGattAttributes.AIR_HUMIDITY);

    public final static UUID UUID_LED_STATUS = UUID.fromString(SampleGattAttributes.LED_STATUS);

    public final static UUID UUID_LDR_PHOTORESISTOR = UUID.fromString(SampleGattAttributes.LDR_PHOTORESISTOR);

    public final static UUID UUID_SOIL_MOISTURE = UUID.fromString(SampleGattAttributes.SOIL_MOISTURE);

    public final static UUID UUID_WATER_PUMP_STATUS = UUID.fromString(SampleGattAttributes.WATER_PUMP_STATUS);

    public final static UUID UUID_WATER_LEVEL = UUID.fromString(SampleGattAttributes.WATER_LEVEL);


    //that provides access to the service for the activity.
    private final Binder binder = new LocalBinder();

    private BluetoothManager bluetoothManager;

    public static final String TAG = "BluetoothLeService";

    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    public static final String EXTRA_DATA_TEMPERATURE = "extra_data_temperature";
    public static final String EXTRA_DATA_HUMIDITY = "extra_data_humidity";
    public static final String EXTRA_DATA_LED_STATUS = "extra_data_led_status";

    public static final String EXTRA_DATA_LDR_PHOTORESISTOR = "extra_data_ldr_photoresistor";

    public static final String EXTRA_DATA_SOIL_MOISTURE = "extra_data_soil_moisture";

    public static final String EXTRA_DATA_WATER_PUMP_STATUS = "extra_data_water_pump_status";

    public static final String EXTRA_DATA_WATER_LEVEL = "extra_data_water_level";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";

    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt;

    private BluetoothLeServiceListener listener;

    public void setBluetoothLeServiceListener(BluetoothLeServiceListener listener) {
        this.listener = listener;
    }

    private void onGattServiceReady(BluetoothGattService gattService) {
        if (listener != null) {
            listener.onBluetoothGattServiceReady(gattService);
        }
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * Close GATT connection
     */
    private void close() {
        if (bluetoothGatt == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public void disconnect() {
        // Disconnect from the GATT server
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothGatt.disconnect();
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address: The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully.
     * The connection result is reported asynchronously through the callback.
     */
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (bluetoothAdapter == null) {
            initialize();
            if (bluetoothAdapter == null) {
                Log.w(TAG, "BluetoothAdapter not initialized.");
                return false;
            }
        }

        if (!bluetoothAdapter.isEnabled() || bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            Log.w(TAG, "Bluetooth is not Enable");
            return false;
        }

        if (address == null) {
            Log.w(TAG, "Unspecified address.");
            return false;
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            Log.i(TAG, "Connection successfully");
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            if (bluetoothGatt != null) {
                bluetoothGatt.discoverServices();
            }
            return true;

        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }
    }

    /**
     * The function is triggered when the connection to the device’s GATT server changes.
     */
    @SuppressLint("NewApi")
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // successfully connected to the GATT Server
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    Log.i(TAG, "Connected to GATT server.");

                    BluetoothDevice connectedDevice = gatt.getDevice();
                    Log.d(TAG, "Connected to device " + connectedDevice.getName() + " at address " + connectedDevice.getAddress());

                    // Attempts to discover services after successful connection.
                    bluetoothGatt.discoverServices();

                    Intent intent = new Intent(ACTION_GATT_CONNECTED);
                    intent.putExtra(EXTRA_DEVICE_SERVICE, connectedDevice);
                    sendBroadcast(intent);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> mylist = getSupportedGattServices();
                for (BluetoothGattService BluetoothGattService : mylist) {
                    Log.w(TAG, "Service: "+BluetoothGattService.getUuid());
                    List<BluetoothGattCharacteristic> characteristics = BluetoothGattService.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        UUID charUUID = characteristic.getUuid();
                        String charUUIDStr = charUUID.toString();
                        Log.i(TAG, "Characteristic UUID: " + charUUIDStr);
                    }
                }
                onGattServiceReady(bluetoothGatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")));

                BluetoothGattService mCustomService = bluetoothGatt.getService(UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b"));

                BluetoothGattCharacteristic temperatureCharacteristic = mCustomService.getCharacteristic(UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"));
                if (temperatureCharacteristic != null) {
                    if ((temperatureCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(temperatureCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = temperatureCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor temperatureCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic humidityCharacteristic = mCustomService.getCharacteristic(UUID.fromString("1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e"));
                if (humidityCharacteristic != null) {
                    if ((humidityCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(humidityCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = humidityCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor humidityCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic LED_StatusCharacteristic = mCustomService.getCharacteristic(UUID.fromString("cad5807a-4b4c-11ee-be56-0242ac120002"));
                if (LED_StatusCharacteristic != null) {
                    if ((LED_StatusCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(LED_StatusCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = LED_StatusCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor LED_StatusCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic LDRCharacteristic = mCustomService.getCharacteristic(UUID.fromString("0adbfb76-6da4-11ee-b962-0242ac120002"));
                if (LDRCharacteristic != null) {
                    if ((LDRCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(LDRCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = LDRCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor LDRCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic SoilMoistureCharacteristic = mCustomService.getCharacteristic(UUID.fromString("2f905938-7003-11ee-b962-0242ac120002"));
                if (SoilMoistureCharacteristic != null) {
                    if ((SoilMoistureCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(SoilMoistureCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = SoilMoistureCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor SoilMoistureCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic waterPumpStatusCharacteristic = mCustomService.getCharacteristic(UUID.fromString("4e12fff8-7005-11ee-b962-0242ac120002"));
                if (waterPumpStatusCharacteristic != null) {
                    if ((waterPumpStatusCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(waterPumpStatusCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = waterPumpStatusCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor waterPumpStatusCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }

                BluetoothGattCharacteristic waterLevelCharacteristic = mCustomService.getCharacteristic(UUID.fromString("cd2d1f72-7004-11ee-b962-0242ac120002"));
                if (waterLevelCharacteristic != null) {
                    if ((waterLevelCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        // Enable notifications for the characteristic
                        gatt.setCharacteristicNotification(waterLevelCharacteristic, true);

                        // Enable the Notification descriptor for the characteristic
                        BluetoothGattDescriptor descriptor = waterLevelCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean descriptorWriteSuccess = gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor waterLevelCharacteristic WriteSuccess: " + descriptorWriteSuccess);
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " +status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Characteristic write successful
                Log.i(TAG, "Characteristic write successful");
            } else {
                // Characteristic write unsuccessful
                Log.i(TAG, "Characteristic write unsuccessful");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * get the reported data.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null)
            return null;
        return bluetoothGatt.getServices();
    }

    /**
     * Notify the activity of the new state, when the server connects or disconnects from the GATT server.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_AIR_TEMPERATURE.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            float temperatureFloat = buffer.getFloat();
            Log.i(TAG, "Temperature: " + temperatureFloat);
            String formattedTemperature = String.format(Locale.US,"%.1f", temperatureFloat);
            intent.putExtra(EXTRA_DATA_TEMPERATURE, formattedTemperature);
        }

        if (UUID_AIR_HUMIDITY.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            // Konvertieren Sie das Byte-Array zurück in einen Float
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            float humidityFloat = buffer.getFloat();
            Log.i(TAG, "Humidity: " + humidityFloat);
            String formattedHumidity = String.format(Locale.US,"%.1f", humidityFloat);
            intent.putExtra(EXTRA_DATA_HUMIDITY, formattedHumidity);
        }

        if (UUID_LDR_PHOTORESISTOR.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            int LDR_value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            String formattedLDR = Integer.toString(LDR_value);
            intent.putExtra(EXTRA_DATA_LDR_PHOTORESISTOR, formattedLDR);
        }

        if (UUID_LED_STATUS.equals(characteristic.getUuid())) {
            String LED_status = null;
            final byte[] data = characteristic.getValue();
            int LED_value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            if (LED_value == 1){
                LED_status = "ON";
            }else{
                if (LED_value == 0){
                    LED_status = "OFF";
                }
            }
            intent.putExtra(EXTRA_DATA_LED_STATUS, LED_status);
        }

        if (UUID_SOIL_MOISTURE.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            int soilMoisture = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            String formattedSoilMoisture = Integer.toString(soilMoisture);
            intent.putExtra(EXTRA_DATA_SOIL_MOISTURE, formattedSoilMoisture);
        }

        if (UUID_WATER_PUMP_STATUS.equals(characteristic.getUuid())) {
            String waterPump_status = null;
            final byte[] data = characteristic.getValue();
            int waterPump_value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            if (waterPump_value == 1){
                waterPump_status = "ON";
            }else{
                if (waterPump_value == 0){
                    waterPump_status = "OFF";
                }
            }
            intent.putExtra(EXTRA_DATA_WATER_PUMP_STATUS, waterPump_status);
        }

        if (UUID_WATER_LEVEL.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            String receivedString = new String(data); // Convert byte array to string
            // Log.i(TAG, "Water Level: " + receivedString);
            intent.putExtra(EXTRA_DATA_WATER_LEVEL, receivedString);
        }
        sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        bluetoothGatt.readCharacteristic(characteristic);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        readCharacteristic(characteristic);
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}