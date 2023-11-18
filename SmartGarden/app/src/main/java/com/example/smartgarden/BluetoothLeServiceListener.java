package com.example.smartgarden;

import android.bluetooth.BluetoothGattService;

public interface BluetoothLeServiceListener {
    void onBluetoothGattServiceReady(BluetoothGattService gattService);
}
