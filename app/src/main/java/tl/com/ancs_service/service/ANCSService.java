package tl.com.ancs_service.service;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class


import tl.com.ancs_service.util.ConstanceValue;

/**
 * Created by tl on 2018-9-27
 * Broadcast ancs service
 */
public class ANCSService extends Service {

  public static final String TAG = "ANCSService";

  private BluetoothManager bluetoothManager;
  private BluetoothAdapter blueToothAdapter;
  private BluetoothLeAdvertiser bluetoothLeAdvertiser;
  private BluetoothGattServer gattServer;
  private BluetoothGattCharacteristic notificationCharacteristic;
  private BluetoothGattCharacteristic controlpointCharacteristic;
  private BluetoothGattCharacteristic datasourceCharacteristic;
  private ANCSBinder ancsBinder = new ANCSBinder();
  private BluetoothGattServerCallback bluetoothGattServerCallback;
  private boolean isConnected = false;
  private boolean doSendData = false;
  private boolean doSendApp = false;
  private String notifCategory = "00";
  private int notifNumber = 1;
  private CharSequence notificationTitle = "";
  private CharSequence notificationText = "";


  public class ANCSBinder extends Binder {
    public ANCSService getANCSService() {
      return ANCSService.this;
    }
  }


  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return ancsBinder;
  }


  @Override
  public void onCreate() {
    super.onCreate();
    initGATTServer ();
  }


  /**
   * 1. Initialize the BLE Bluetooth Advertiser and configure the service with the specified UUID
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void initGATTServer() {

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build();

    AdvertiseData advertiseData = new AdvertiseData.Builder()
            //    .addServiceUuid(new ParcelUuid(UUID.fromString(ConstanceValue.SERVICE_ANCS)))
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build();

    // Build by UUID_SERVICE
    AdvertiseData scanResponseData = new AdvertiseData.Builder()
            .addServiceUuid(new ParcelUuid(UUID.fromString(ConstanceValue.SERVICE_ANCS)))
            .setIncludeTxPowerLevel(true)
            .build();

    // Callback after the broadcast is successfully created
    AdvertiseCallback callback = new AdvertiseCallback() {
      @Override
      public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        Log.d(TAG, "BLE advertisement added successfully");
        //showText("1. initGATTServer success");
        //println("1. initGATTServer success");
      }

      @Override
      public void onStartFailure(int errorCode) {
        Log.e(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
        //showText("1. initGATTServer failure");
      }
    };

    bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    blueToothAdapter = bluetoothManager.getAdapter();

    // Some devices do not support Ble Center
    bluetoothLeAdvertiser = blueToothAdapter.getBluetoothLeAdvertiser();
    if (bluetoothLeAdvertiser == null) {
      Log.i(TAG, "BluetoothLeAdvertiser为null");
    }

    // Initialize the service
    initServices(ANCSService.this);

    // Start broadcasting
    if (bluetoothLeAdvertiser != null) {
      bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);
    }
  }


  /**
   * Initialize the Gatt service, mainly to configure various UUIDs for the Gatt service
   *
   * @param context
   */

  private void initServices(Context context) {
    setBluetoothGattServerCallback();
    // Create GattServer server

    gattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);

    // This specified service creates the specified UUID
    BluetoothGattService service = new BluetoothGattService(UUID.fromString
            (ConstanceValue.SERVICE_ANCS),
            BluetoothGattService.SERVICE_TYPE_PRIMARY);

    // Add a readable characteristic descriptor
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString
            (ConstanceValue.DESCRIPTOR_CONFIG),
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    BluetoothGattDescriptor descriptor1 = new BluetoothGattDescriptor(UUID.fromString
            (ConstanceValue.DESCRIPTOR_CONFIG1),
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    BluetoothGattDescriptor descriptor2 = new BluetoothGattDescriptor(UUID.fromString
            (ConstanceValue.DESCRIPTOR_CONFIG2),
            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

    // Add a writable characteristicistic of the specified UUID
    notificationCharacteristic = new BluetoothGattCharacteristic(UUID
            .fromString(ConstanceValue.CHARACTERISTICS_NOTIFICATION_SOURCE),
            BluetoothGattCharacteristic.PROPERTY_WRITE |
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE |
                    BluetoothGattCharacteristic.PERMISSION_READ);
    notificationCharacteristic.addDescriptor(descriptor);
    notificationCharacteristic.setValue("notify");

      // Add a writable characteristicistic of the specified UUID
      datasourceCharacteristic = new BluetoothGattCharacteristic(UUID
              .fromString(ConstanceValue.CHARACTERISTICS_DATA_SOURCE),
              BluetoothGattCharacteristic.PROPERTY_WRITE |
                      BluetoothGattCharacteristic.PROPERTY_READ |
                      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
              BluetoothGattCharacteristic.PERMISSION_WRITE |
                      BluetoothGattCharacteristic.PERMISSION_READ);

      datasourceCharacteristic.setValue("notify");

    // Add a writable characteristicistic of the specified UUID
    controlpointCharacteristic = new BluetoothGattCharacteristic(UUID
            .fromString(ConstanceValue.CHARACTERISTICS_CONTROL_POINT),
            BluetoothGattCharacteristic.PROPERTY_WRITE |
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE |
                    BluetoothGattCharacteristic.PERMISSION_READ);

    controlpointCharacteristic.setValue("notify");

    service.addCharacteristic(notificationCharacteristic);
    service.addCharacteristic(datasourceCharacteristic);
    service.addCharacteristic(controlpointCharacteristic);

    gattServer.addService(service);


    Log.e(TAG, "2. initServices ok");
  }


  /**
   * Bluetooth server callback
   */
  private void setBluetoothGattServerCallback() {
    bluetoothGattServerCallback = new BluetoothGattServerCallback() {

      // Call back, indicating when the remote device is connected or disconnected.
      @Override
      public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Log.i(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s",
                device.getName(), device.getAddress()));
        Log.i(TAG, String.format("1.onConnectionStateChange：status = %s, newState =%s ", status,
                newState));
        super.onConnectionStateChange(device, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          isConnected = true;

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          isConnected = false;
        }
        Log.i(TAG, String.format("Is connected：status = %s", isConnected));
      }

      // Indicate whether the local service has been successfully added.
      @Override
      public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.i(TAG, String.format("onServiceAdded：status = %s", status));
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString
                (ConstanceValue.DESCRIPTOR_CONFIG),
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        service.getCharacteristic(UUID
                .fromString(ConstanceValue.CHARACTERISTICS_CONTROL_POINT)).addDescriptor(descriptor);
        BluetoothGattDescriptor descriptor2 = new BluetoothGattDescriptor(UUID.fromString
                (ConstanceValue.DESCRIPTOR_CONFIG),
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        service.getCharacteristic(UUID.fromString(ConstanceValue.CHARACTERISTICS_DATA_SOURCE)).addDescriptor(descriptor2);
      }

      // The remote client has requested to read the local characteristics.
      @Override
      public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                              BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s",
                device.getName(), device.getAddress()));
        Log.e(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s",
                requestId, offset));
        // Respond to the client's request
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                characteristic.getValue());
      }

      // The remote client has requested to write a local feature.
      @Override
      public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                               BluetoothGattCharacteristic characteristic,
                                               boolean
                                                       preparedWrite, boolean responseNeeded, int
                                                       offset, byte[] requestBytes) {
        Log.e(TAG, String.format("3.onCharacteristicWriteRequest：device name = %s, address = " +
                        "%s",
                device.getName(), device.getAddress()));
        Log.e(TAG, String.format("3.onCharacteristicWriteRequest：requestId = %s, " +
                        "preparedWrite=%s, " +
                        "responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite,
                responseNeeded,
                offset, Arrays.toString(requestBytes)));
        Log.d(TAG, "3.uuid of request: " + characteristic.getUuid());
        // Respond to the client's request
        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                requestBytes);

        // Handle write feature value request
        onResponseToClient(requestBytes, device, requestId, characteristic);
      }

      // The remote client has requested to write to the local descriptor.
      @Override
      public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                           BluetoothGattDescriptor descriptor, boolean
                                                   preparedWrite, boolean responseNeeded, int
                                                   offset,
                                           byte[] value) {
        Log.e(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s",
                device.getName(), device.getAddress()));

        Log.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = " +
                        "%s, " +
                        "responseNeeded = %s, descriptor = %s, offset = %s, value = %s,", requestId, preparedWrite,
                responseNeeded, descriptor, offset, bytesToHex(value)));
        Log.d(TAG, "current value: " +Arrays.toString(descriptor.getValue()));
        Log.d(TAG, "uuid of request: " + descriptor.getCharacteristic().getUuid());

        // Respond to client requests
        gattServer.sendResponse(device, requestId, 0, offset, null);

      }

      // The remote client has requested to read the local descriptor.
      @Override
      public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                          BluetoothGattDescriptor descriptor) {
        Log.e(TAG, String.format("onDescriptorReadRequest：device name = %s, address = %s",
                device
                        .getName(), device.getAddress()));
        Log.e(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId));

        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
      }

      // Callback when notification or indication is sent to the remote device.
      @RequiresApi(api = Build.VERSION_CODES.O)
      @Override
      public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
        Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device
                .getName(), device.getAddress()));
        Log.e(TAG, String.format("5.onNotificationSent：status = %s", status));
        if (doSendApp == true) {
          sendApp(device);
          doSendApp = false;
        }
        if (doSendData == true) {
          sendData(device);
          doSendData = false;
        }

      }

      // Indicates that the callback for the MTU of the given device connection has changed.
      @Override
      public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged (device, mtu);
        Log.e(TAG, String.format("onMtuChanged：mtu = %s", mtu));
      }

      // Perform all pending write operations for this device.
      @Override
      public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
        Log.e(TAG, String.format("onExecuteWrite：requestId = %s", requestId));
        if (gattServer != null) {
          gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[] {});
        }
      }
    };
  }

  // Process the feature value write request
  private void onResponseToClient(byte[] requestBytes, BluetoothDevice device, int requestId,
                                  BluetoothGattCharacteristic characteristic) {
    Log.e(TAG, String.format("4.onResponseToClient：device name = %s, address = %s", device
            .getName(), device.getAddress()));
    Log.e(TAG, String.format("4.onResponseToClient：requestId = %s", requestId));

    controlpointCharacteristic.setValue(requestBytes);
    gattServer.notifyCharacteristicChanged(device, controlpointCharacteristic, false);
    Log.i(TAG, "4.：" +bytesToHex(requestBytes));
    if (bytesToHex(requestBytes).substring(0,2).equals("00")) {
      Log.i(TAG, "4. do send data");
      doSendData = true;
    } else if (bytesToHex(requestBytes).substring(0,2).equals("01")) {
      Log.i(TAG, "4. do send app");
      doSendApp = true;
    }
  }



  // Notify the client that the feature value has been updated
  //public void upDate(BluetoothGattCharacteristic characteristic, String value) {
 //   characteristic.setValue(value.getBytes());
    // todo needs to determine which connected Bluetooth device
 //   gattServer.notifyCharacteristicChanged(gattServer.getConnectedDevices().get(0),
  //          characteristic, false);
 // }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
              + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }
  public static String convertStringToHex(String str) {

    StringBuffer hex = new StringBuffer();

    // loop chars one by one
    for (char temp : str.toCharArray()) {

      // convert char to int, for char `a` decimal 97
      int decimal = (int) temp;

      // convert int to hex, for decimal 97 hex 61
      hex.append(Integer.toHexString(decimal));
    }

    return hex.toString();

  }

  // Notify the client that the feature value has been updated test
  public void upDate(Notification notification) {

    switch (notification.category) {
      case "call":
        notifCategory = "01";
        break;
      case "social":
        notifCategory = "04";
        break;
      case "msg":
        notifCategory = "04";
        break;
      case "event":
        notifCategory = "05";
        break;
      case "alarm":
        notifCategory = "05";
        break;
      case "reminder":
        notifCategory = "05";
        break;
      case "email":
        notifCategory = "06";
        break;
      case "navigation":
        notifCategory = "0A";
        break;
      case "transport":
        notifCategory = "0A";
        break;
      default:
        notifCategory = "00";
        break;
    }

    if (notifCategory.equals("00")) {
      Log.d(TAG, "Notification not in main category - "+notifCategory);
      return;
    }
    notifNumber++;

    Bundle extras = notification.extras;
    notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
    CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
    notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE);

    Log.d(TAG, "notification is - category:"+notification.category+"; text: "+notificationText+"; subtext: "+notificationSubText+"; title:"+notificationTitle);

    notificationCharacteristic.setValue(hexStringToByteArray("0018"+notifCategory+"01"+ String.format("%08d", notifNumber)));
    if (isConnected && bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).size()
            > 0) {
      Log.d(TAG, "ran notification update");
      gattServer.notifyCharacteristicChanged(bluetoothManager.getConnectedDevices
              (BluetoothProfile.GATT).get(0), notificationCharacteristic, false);
    } else {
      Log.d(TAG, "was not connected for update");
    }
    //sendData();
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public void sendData(BluetoothDevice device) {
    LocalDateTime myDateObj = LocalDateTime.now();
    System.out.println("Before formatting: " + myDateObj);
    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    String formattedDate = myDateObj.format(myFormatObj);
    Log.d(TAG, "DATE TEXT:"+formattedDate);
    Log.d(TAG, "DATE:"+convertStringToHex(formattedDate));

    int titleLength = notificationTitle.length();
    int textLength = notificationText.length();

    Log.d(TAG, "hex title length is:"+String.format("%02X", titleLength));
    Log.d(TAG, "hex text length is:"+String.format("%02X", textLength));

    Log.d(TAG, "send data");
    datasourceCharacteristic.setValue(hexStringToByteArray("00"+String.format("%08d", notifNumber)+"001400636F6D2E6170706C652E6D6F62696C656D61696C050F00"+convertStringToHex(formattedDate)+"01"+String.format("%02X", titleLength)+"00"+convertStringToHex(notificationTitle.toString())+"03"+String.format("%02X", textLength)+"00"+convertStringToHex(notificationText.toString())));
    gattServer.notifyCharacteristicChanged(device, datasourceCharacteristic, false);
    Log.d(TAG, "data sent");
  }
  public void sendApp(BluetoothDevice device) {
    Log.d(TAG, "send app");
    datasourceCharacteristic.setValue(hexStringToByteArray("01636F6D2E6170706C652E6D6F62696C656D61696C000004004D61696C"));
    gattServer.notifyCharacteristicChanged(device, datasourceCharacteristic, false);
    Log.d(TAG, "app sent");
  }
}
