package com.example.smartgarden;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity{
    public static final int REQUEST_ENABLE_BT = 0;

    public static final int PERMISSION_REQUEST_CODE = 1;

    public static DeviceScanActivity deviceScanActivity;

    public static Spinner spinner_devices;

    HomeFragment homeFragment = new HomeFragment();

    public static boolean isConnected = false;

    static MenuItem connectMenuItem;

    static MenuItem disconnectMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if BLE is supported
        checkBLESupport();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Hide the app name or title from the Toolbar
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        spinner_devices = findViewById(R.id.spinner_devices);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();
                return true;
            }
            return false;
        });

        //mGattServicesList.setOnChildClickListener(servicesListClickListener);

        deviceScanActivity = DeviceScanActivity.getInstance(this);
        deviceScanActivity.init(this);

        setSpinnerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // set the color of an item icon
        MenuItem menuItem = menu.findItem(R.id.menu_scan);
        Drawable iconDrawable = menuItem.getIcon();
        assert iconDrawable != null;
        iconDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        menuItem.setIcon(iconDrawable);
        return true;
    }

    /**
     *
     * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
     *
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
         connectMenuItem = menu.findItem(R.id.menu_connect);
         disconnectMenuItem = menu.findItem(R.id.menu_disconnect);

        connectMenuItem.setVisible(!isConnected);
        disconnectMenuItem.setVisible(isConnected);

        return super.onPrepareOptionsMenu(menu);
    }

    public static void updateConnectionState(boolean isConnected){
        if (isConnected) {
            connectMenuItem.setVisible(false);
            disconnectMenuItem.setVisible(true);
        } else {
            connectMenuItem.setVisible(true);
            disconnectMenuItem.setVisible(false);
        }
    }

    /**
     *  request the user to enable Bluetooth
     */
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)


    public static void requestEnableBluetooth(Context context, int requestCode) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ((Activity) context).startActivityForResult(enableBtIntent, requestCode);
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
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    //Open the permission screen
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was successfully enabled
                deviceScanActivity.scanLeDevice();
            } else {
                // Bluetooth is not enable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestEnableBluetooth(this, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    public static void setSpinnerView(){
        spinner_devices.setAdapter(deviceScanActivity.getBluetoothDeviceAdapter());
        spinner_devices.setPrompt("Select a Bluetooth Device !");
    }

    /**
     * Check if BLE is supported on the device.
     */
    private void checkBLESupport() {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth low energy is not supported !", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}