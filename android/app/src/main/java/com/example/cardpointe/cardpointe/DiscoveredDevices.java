package com.example.cardpointe.cardpointe;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.bolt.consumersdk.CCConsumer;
import com.bolt.consumersdk.listeners.BluetoothSearchResponseListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import io.flutter.plugin.common.EventChannel;

public class DiscoveredDevices implements EventChannel.StreamHandler, BluetoothSearchResponseListener {

    private EventChannel.EventSink discoverDevicesSink;
    final List<BluetoothDevice> devices = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create();
    Boolean _useSimulated = false;

    Context mContext;
    public static String TAG = DiscoveredDevices.class.getSimpleName();

    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());

    public void startDiscovering(Context context, Boolean useSimulated) {
        _useSimulated = useSimulated;
        mContext = context;
        if (useSimulated) {
            Timer t = new java.util.Timer();
            t.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (discoverDevicesSink != null) {
                                Log.d(TAG, "Device added to EventSink (simulated)");
                                Map<String, Object> deviceMap = new HashMap<>();
                                deviceMap.put("name", "Dummy VP3300");
                                deviceMap.put("address", "11:22:33:44:55:66");
                                uiThreadHandler.post(() -> discoverDevicesSink.success(gson.toJson(deviceMap)));
                            }
                            t.cancel();
                        }
                    },
                    2000
            ); // Go go go!

            return;
        }
//        CCConsumer.getInstance().getApi().startBluetoothDeviceSearch(this, context, false);
    }

    @Override
    public void onListen(Object args, EventChannel.EventSink events) {
        discoverDevicesSink = events;

    }

    @Override
    public void onCancel(Object args) {
        devices.clear();
        discoverDevicesSink = null;
    }

    @Override
    public void onDeviceFound(BluetoothDevice bluetoothDevice) {

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onDeviceFound: " + bluetoothDevice.getName());
        }

        devices.add(bluetoothDevice);
        if (discoverDevicesSink != null) {
            Log.d(TAG, "Device added to EventSink");

            uiThreadHandler.post(() -> discoverDevicesSink.success(gson.toJson(bluetoothDevice)));
        }

    }

    private Map<String, Object> deviceToMap(BluetoothDevice device) {
        Map<String, Object> deviceMap = new HashMap<>();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            deviceMap.put("name", device.getName());
        }
        deviceMap.put("address", device.getAddress());
        return deviceMap;
    }
}
