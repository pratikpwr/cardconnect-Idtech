package com.example.cardpointe;

import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.bolt.consumersdk.CCConsumer;
import com.bolt.consumersdk.domain.CCConsumerAccount;
import com.bolt.consumersdk.domain.CCConsumerError;
import com.bolt.consumersdk.domain.request.CCConsumerAndroidPayGetTokenRequest;
import com.bolt.consumersdk.listeners.BluetoothSearchResponseListener;
import com.bolt.consumersdk.swiper.SwiperControllerListener;
import com.bolt.consumersdk.swiper.enums.BatteryState;
import com.bolt.consumersdk.swiper.enums.SwiperError;
import com.bolt.consumersdk.swiper.enums.SwiperType;
import com.example.cardpointe.cardpointe.SwiperControllerManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {


    //    private final DiscoveredDevices discoveredDevices = new DiscoveredDevices();
    public static final String DISCOVER_DEVICES_STREAM = "cardPointeDiscover";


    private final Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create();
    EventChannel.EventSink discoverDevicesSink;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {

//        SplashScreen.show(this, true);
        GeneratedPluginRegistrant.registerWith(flutterEngine);

        final EventChannel eventChannel = new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), DISCOVER_DEVICES_STREAM);


        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                discoverDevicesSink = events;
            }

            @Override
            public void onCancel(Object arguments) {
                discoverDevicesSink = null;
            }
        });


        final BluetoothSearchResponseListener deviceHandler = new BluetoothSearchResponseListener() {

            @Override
            public void onDeviceFound(BluetoothDevice bluetoothDevice) {
                if (discoverDevicesSink != null) {


                    if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // failure
                        uiThreadHandler.post(() -> discoverDevicesSink.error("PERMISSIONS_DENIED", "Please allow all permissions and retry", null));
                        return;
                    }

                    String deviceName = bluetoothDevice.getName();

                    if (deviceName.contains("IDTECH")) {

                        Map<String, Object> deviceMap = new HashMap<>();
                        deviceMap.put("name", deviceName);
                        deviceMap.put("address", bluetoothDevice.getAddress());

                        uiThreadHandler.post(() -> discoverDevicesSink.success(gson.toJson(deviceMap)));
                    }
                }
            }
        };


        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), "cardPointe").setMethodCallHandler((call, result) -> {
            final SwiperControllerManager swiperManager = SwiperControllerManager.getInstance();

            swiperManager.setContext(this.getApplicationContext());
            swiperManager.setSwiperType(SwiperType.IDTech);

            final SwiperControllerListener swiperControllerListener = new SwiperControllerListener() {
                @Override
                public void onTokenGenerated(CCConsumerAccount ccConsumerAccount, CCConsumerError ccConsumerError) {

                }

                @Override
                public void onError(SwiperError swiperError) {
                }

                @Override
                public void onSwiperReadyForCard() {
                }

                @Override
                public void onSwiperConnected() {
                    Log.i("IDTECH", "Swiper Connected" + swiperManager.getMACAddr());
                    result.success(true);
                }

                @Override
                public void onSwiperDisconnected() {
                    Log.i("IDTECH", "Swiper Disconnected");
                    result.success(true);
                }

                @Override
                public void onBatteryState(BatteryState batteryState) {
                }

                @Override
                public void onStartTokenGeneration() {
                }

                @Override
                public void onLogUpdate(String s) {
                    Log.i("IDTECH", "onLogUpdate " + s);
                }

                @Override
                public void onDeviceConfigurationUpdate(String s) {
                }

                @Override
                public void onConfigurationProgressUpdate(double v) {
                }

                @Override
                public void onConfigurationComplete(boolean b) {
                }

                @Override
                public void onTimeout() {
                }

                @Override
                public void onLCDDisplayUpdate(String s) {
                }

                @Override
                public void onRemoveCardRequested() {
                }

                @Override
                public void onCardRemoved() {
                }

                @Override
                public void onDeviceBusy() {
                }
            };

            switch (call.method) {
                case "startDiscovering":
                    final Boolean useSimulated = call.<Boolean>argument("useSimulated");

                    if (useSimulated != null && useSimulated) {
                        Timer t = new java.util.Timer();
                        t.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                if (discoverDevicesSink != null) {
//                                                            android.util.Log.d(TAG, "Device added to EventSink (simulated)");
                                    Map<String, Object> deviceMap = new HashMap<>();
                                    deviceMap.put("name", "Dummy VP3300");
                                    deviceMap.put("address", "11:22:33:44:55:66");
                                    uiThreadHandler.post(() -> discoverDevicesSink.success(gson.toJson(deviceMap)));
                                }
                                t.cancel();
                            }
                        }, 6000); // Go go go!

                        return;
                    } else {

                        CCConsumer.getInstance().getApi().startBluetoothDeviceSearch(deviceHandler, this.getContext(), false);

                    }
//                                    discoveredDevices.startDiscovering(this.getApplicationContext(), useSimulated);
                    result.success(true);
                    break;
                case "connect":
                    final String macAddress = call.argument("macAddress");
                    swiperManager.setSwiperControllerListener(swiperControllerListener);
                    swiperManager.setMACAddress(macAddress);
                    // to do tasks after connecting I can add swiper controller listener here and manage data
                    // or can do that in SwiperControllerManager class itself
//              result.success(true);shadow$_klass_ = {Class@23533} "class io.flutter.plugin.common.MethodChannel$IncomingMethodCallHandler$1"… Navigate
                    break;
                case "disconnect":
                    swiperManager.disconnectFromDevice();
                    break;
                default:
                    result.notImplemented();
                    break;
            }
        });
    }

}
