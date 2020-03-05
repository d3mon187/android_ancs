package tl.com.ancs_service.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.greenrobot.eventbus.EventBus;

import tl.com.ancs_service.busbean.NotificationPostedBusBean;
import tl.com.ancs_service.busbean.NotificationRemovedBusBean;

/**
 * Created by tl on 2018-9-27
 * Notification usage right service is used to get android notifications
 */
public class NotificationService extends NotificationListenerService {

  // callback when the notification is removed
  @Override
  public void onNotificationRemoved(StatusBarNotification sbn) {
    super.onNotificationRemoved (sbn);
    Notification notification = sbn.getNotification();
    EventBus.getDefault().post(new NotificationRemovedBusBean(notification));
  }

  // callback when adding a notification
  @Override
  public void onNotificationPosted(StatusBarNotification sbn) {
    super.onNotificationPosted (sbn);
    Notification notification = sbn.getNotification();
    EventBus.getDefault().post(new NotificationPostedBusBean(notification));
  }


}
