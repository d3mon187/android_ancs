package tl.com.ancs_service;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import tl.com.ancs_service.busbean.NotificationPostedBusBean;
import tl.com.ancs_service.busbean.NotificationRemovedBusBean;
import tl.com.ancs_service.service.ANCSService;

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "MainActivity";
  public static final int NO_BLUETOOTH_REQUST = 0X101;
  public static final int NO_NOTIFY_REQUST = 0x201;

  private ANCSService ancsService;

  ServiceConnection ancsconn = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      Log.d(TAG, "ancs onServiceConnected");
      ANCSService.ANCSBinder binder = (ANCSService.ANCSBinder) iBinder;
      ancsService = binder.getANCSService ();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      Toast.makeText(MainActivity.this, "ancs service disconnected", Toast.LENGTH_SHORT).show();
    }
  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate (savedInstanceState);
    EventBus.getDefault().register(this);
    setContentView(R.layout.activity_main);
    checkBluetooth();
    checkNotify ();

    Button bt=findViewById(R.id.update_btn);
    bt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //ancsService.upDate(notification);
      }
    });
  }


  // Enable Bluetooth connection and notification service
  private void openService() {
    Intent serviceIntent = new Intent(MainActivity.this, ANCSService.class);
    bindService(serviceIntent, ancsconn, Context.BIND_AUTO_CREATE);
  }


  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(Object object) {
    if (object instanceof NotificationPostedBusBean) {// A new notification
      Notification notification = (Notification) ((NotificationPostedBusBean) object)
              .getNotification();
      Bundle extras = notification.extras;
      int notificationIcon = extras.getInt(Notification.EXTRA_SMALL_ICON);
      //Bitmap notificationLargeIcon = (Bitmap) extras.get(Notification.EXTRA_LARGE_ICON);
      CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
      CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
      //Toast.makeText(this, notificationText + "\n" + notificationSubText, Toast.LENGTH_SHORT).show();

      ancsService.upDate(notification);
      Log.d(TAG, "new notification");
    } else if (object instanceof NotificationRemovedBusBean) {// Removed notification

    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == NO_BLUETOOTH_REQUST) {
      checkBluetooth();
    } else if (requestCode == NO_NOTIFY_REQUST) {
      checkNotify ();
    }
  }


  /**
   * Check if the device has Bluetooth turned on
   */
  private void checkBluetooth() {
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context
            .BLUETOOTH_SERVICE);
    if (bluetoothManager != null) {
      BluetoothAdapter blueToothAdapter = bluetoothManager.getAdapter();
      if (blueToothAdapter != null && blueToothAdapter.isEnabled()) {
        openService();
      } else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, NO_BLUETOOTH_REQUST);
        Toast.makeText (this, "Please allow Bluetooth, otherwise the app will not work properly", Toast.LENGTH_SHORT) .show ();
      }
    } else {
      Toast.makeText (this, "This device cannot use the app without Bluetooth", Toast.LENGTH_SHORT) .show ();
    }
  }

  // Check if you have notification permission
  private void checkNotify () {
    boolean enable = false;
    String packageName = getPackageName();
    String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
    if (flat != null) {
      enable = flat.contains(packageName);
    }
    if (!enable) {
      gotoNotificationAccessSetting();
    }
  }


  // Jump to notification permission interface
  private boolean gotoNotificationAccessSetting() {
    try {
      Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivityForResult(intent, NO_NOTIFY_REQUST);
      return true;
    } catch (ActivityNotFoundException e) {
      try {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName cn = new ComponentName("com.android.settings", "com.android.settings" +
                ".Settings$NotificationAccessSettingsActivity");
        intent.setComponent(cn);
        intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
        startActivityForResult(intent, NO_NOTIFY_REQUST);
        return true;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      return false;
    }
  }


  @Override
  protected void onDestroy() {
    super.onDestroy ();
    EventBus.getDefault().unregister(this);
    if (ancsService != null) {
      ancsService.unbindService(ancsconn);
    }

  }
}
