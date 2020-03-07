package tl.com.ancs_service.service;

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
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

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
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void initServices(Context context) {
    setBluetoothGattServerCallback();
    // Create GattServer server
    gattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);

    // This specified service creates the specified UUID
    BluetoothGattService service = new BluetoothGattService(UUID.fromString
            (ConstanceValue.SERVICE_ANCS),
            BluetoothGattService.SERVICE_TYPE_PRIMARY);

    // Add a writable characteristicistic of the specified UUID
    notificationCharacteristic = new BluetoothGattCharacteristic(UUID
            .fromString(ConstanceValue.CHARACTERISTICS_NOTIFICATION_SOURCE),
            BluetoothGattCharacteristic.PROPERTY_WRITE |
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE |
                    BluetoothGattCharacteristic.PERMISSION_READ);
    // Add a readable characteristic descriptor
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString
            (ConstanceValue.DESCRIPTOR_CONFIG),
            BluetoothGattCharacteristic.PERMISSION_WRITE);
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    notificationCharacteristic.addDescriptor(descriptor);
    notificationCharacteristic.setValue("notify");
    service.addCharacteristic(notificationCharacteristic);

      // Add a writable characteristicistic of the specified UUID
      datasourceCharacteristic = new BluetoothGattCharacteristic(UUID
              .fromString(ConstanceValue.CHARACTERISTICS_DATA_SOURCE),
              BluetoothGattCharacteristic.PROPERTY_WRITE |
                      BluetoothGattCharacteristic.PROPERTY_READ |
                      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
              BluetoothGattCharacteristic.PERMISSION_WRITE |
                      BluetoothGattCharacteristic.PERMISSION_READ);
      // Add a readable characteristic descriptor
      datasourceCharacteristic.addDescriptor(descriptor);
      datasourceCharacteristic.setValue("notify");
      service.addCharacteristic(datasourceCharacteristic);

      descriptor.setValue(hexStringToByteArray("10"));
    // Add a writable characteristicistic of the specified UUID
    controlpointCharacteristic = new BluetoothGattCharacteristic(UUID
            .fromString(ConstanceValue.CHARACTERISTICS_CONTROL_POINT),
            BluetoothGattCharacteristic.PROPERTY_WRITE |
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE |
                    BluetoothGattCharacteristic.PERMISSION_READ);
    // Add a readable characteristic descriptor
    controlpointCharacteristic.addDescriptor(descriptor);
    controlpointCharacteristic.setValue("write");
    service.addCharacteristic(controlpointCharacteristic);

      Log.e(TAG, String.format("Initial Control point value is = %s",  Arrays.toString(controlpointCharacteristic.getValue())));

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
                offset, requestBytes.toString()));

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
                responseNeeded, descriptor, offset, Arrays.toString(value)));
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
      @Override
      public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
        Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device
                .getName(), device.getAddress()));
        Log.e(TAG, String.format("5.onNotificationSent：status = %s", status));
        Log.e(TAG, String.format("Control point value is = %s",  Arrays.toString(controlpointCharacteristic.getValue())));
        Log.e(TAG, String.format("Control point descriptor value is = %s",  Arrays.toString(controlpointCharacteristic.getDescriptor(UUID.fromString(ConstanceValue.DESCRIPTOR_CONFIG)).getValue())));
          ;
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
      }
    };
  }

  // Process the feature value write request
  private void onResponseToClient(byte[] reqeustBytes, BluetoothDevice device, int requestId,
                                  BluetoothGattCharacteristic characteristic) {
    Log.e(TAG, String.format("4.onResponseToClient：device name = %s, address = %s", device
            .getName(), device.getAddress()));
    Log.e(TAG, String.format("4.onResponseToClient：requestId = %s", requestId));

    String str = new String(reqeustBytes);
    notificationCharacteristic.setValue(str.getBytes());
    // Tell the client that the feature value has been updated (confirm: true means request confirmation (indication) from the client, false means send notification)
    gattServer.notifyCharacteristicChanged(device, notificationCharacteristic, false);
    Log.i(TAG, "4.响应：" + str);

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

  // Notify the client that the feature value has been updated test
  public void upDate() {
    notificationCharacteristic.setValue(hexStringToByteArray("0010060101020304"));
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

  public void sendData() {
      datasourceCharacteristic.setValue(hexStringToByteArray("00010203040103006E52460302003532"));
      gattServer.notifyCharacteristicChanged(bluetoothManager.getConnectedDevices
              (BluetoothProfile.GATT).get(0), datasourceCharacteristic, false);
      datasourceCharacteristic.setValue(hexStringToByteArray("000300636F6D"));
      gattServer.notifyCharacteristicChanged(bluetoothManager.getConnectedDevices
              (BluetoothProfile.GATT).get(0), datasourceCharacteristic, false);
  }
}
