import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final MethodChannel methodChannel = MethodChannel('cardPointe');

  final EventChannel eventChannel = EventChannel('cardPointeDiscover');

  Stream devicesStream = Stream.empty();

  Future<void> startSearchingDevice() async {
    Map<Permission, PermissionStatus> statuses = await [
      Permission.locationWhenInUse,
      Permission.location,
      Permission.bluetooth,
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
    ].request();

    if (statuses.values.every((element) => element.isGranted)) {
      final result = await methodChannel.invokeMethod('startDiscovering', {
        "useSimulated": useSimulated,
      });
      print("Discovering Success $result");
      showSnackbar("Finding readers...");
    } else {
      print('No Permissions');
      showSnackbar("Allow Permissions and Retry!");
    }
  }

  Stream discoveredDevicesStream() {
    devicesStream = eventChannel.receiveBroadcastStream();
    return devicesStream;
  }

  bool isConnected = false;
  bool useSimulated = false;

  Future<bool> connectToDevice(String macAddress) async {
    final connected = await methodChannel
        .invokeMethod<bool>("connect", {"macAddress": macAddress});

    setState(() {
      isConnected = connected ?? false;
    });
    if (isConnected) {
      showSnackbar("Successfully connected to Reader");
    }
    return isConnected;
  }

  Future<bool> disconnectDevice() async {
    final disconnected = await methodChannel.invokeMethod<bool>("disconnect");

    setState(() {
      isConnected = !(disconnected ?? false);
    });
    if (!isConnected) {
      showSnackbar("Reader Disconnected");
    }

    return isConnected;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("VP3300"),
        actions: [
          CupertinoSwitch(
              value: useSimulated,
              onChanged: (value) {
                setState(() {
                  useSimulated = value;
                  devicesStream = Stream.empty();
                });
              })
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              StreamBuilder(
                stream: discoveredDevicesStream(),
                builder: (context, AsyncSnapshot snapshot) {
                  if (snapshot.hasData) {
                    final data = jsonDecode(snapshot.data);
                    return Card(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 8),
                        child: Row(
                          children: [
                            Text("${data["name"]}"),
                            Spacer(),
                            CupertinoSwitch(
                              value: isConnected,
                              onChanged: (value) async {
                                if (useSimulated) {
                                  showSnackbar(
                                      "Can not connect to simulated device");
                                  return;
                                }
                                if (value) {
                                  showSnackbar("Connecting reader...");
                                  await connectToDevice(data["address"]);
                                } else {
                                  showSnackbar("Disconnecting to reader...");
                                  await disconnectDevice();
                                }
                              },
                            )
                          ],
                        ),
                      ),
                    );
                  }
                  return Text("Start Scanning Devices");
                },
              ),
              if (isConnected) Text('Device is connected')
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => startSearchingDevice(),
        child: Icon(Icons.search),
      ),
    );
  }

  showSnackbar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}
