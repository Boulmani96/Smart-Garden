package com.example.smartgarden.Adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.smartgarden.R;

import java.util.ArrayList;

public class BluetoothDeviceAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<BluetoothDevice> bluetoothDevices;

    public BluetoothDeviceAdapter(Context context, ArrayList<BluetoothDevice> bluetoothDevices) {
        this.context = context;
        this.bluetoothDevices = bluetoothDevices;
    }

    public void deleteDevice(BluetoothDevice device) {
        bluetoothDevices.remove(device);
    }

    public void addDevice(BluetoothDevice device) {
        if (!bluetoothDevices.contains(device)) {
            bluetoothDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return bluetoothDevices.get(position);
    }

    @Override
    public int getCount() {
        return this.bluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return this.bluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.bluetooth_device_layout, parent, false);
            TextView textViewDeviceName = convertView.findViewById(R.id.device_name_textView);
            TextView textViewDeviceAddress = convertView.findViewById(R.id.device_address_textView);

            BluetoothDevice bluetoothDevice = this.bluetoothDevices.get(position);
            if (bluetoothDevice.getName() == null){
                textViewDeviceName.setText(null);
                textViewDeviceAddress.setText(bluetoothDevice.getAddress());
            }else{
                textViewDeviceName.setText(bluetoothDevice.getName());
                textViewDeviceAddress.setText(null);
            }
        }
        return convertView;
    }
}