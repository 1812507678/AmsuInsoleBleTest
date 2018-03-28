package com.amsu.amsuinsolebletest.service;

import android.app.Activity;

import com.amsu.amsuinsolebletest.MainActivity;

import no.nordicsemi.android.dfu.BuildConfig;
import no.nordicsemi.android.dfu.DfuBaseService;


public class DfuService extends DfuBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
      /*
       * As a target activity the NotificationActivity is returned, not the MainActivity. This is because the notification must create a new task:
       *
       * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       *
       * when user press it. Using NotificationActivity we can check whether the new activity is a root activity (that means no other activity was open before)
       * or that there is other activity already open. In the later case the notificationActivity will just be closed. System will restore the previous activity.
       * However if the application has been closed during upload and user click the notification a NotificationActivity will be launched as a root activity.
       * It will create and start the main activity and terminate itself.
       *
       * This method may be used to restore the target activity in case the application was closed or is open. It may also be used to recreate an activity
       * history (see NotificationActivity).
       */
        return MainActivity.class;
    }

    /*@Override
    protected boolean isDebug() {
        // Here return true if you want the service to print more logs in LogCat.
        // Library's BuildConfig in current version of Android Studio is always set to DEBUG=false, so
        // make sure you return true or your.app.BuildConfig.DEBUG here.
        return BuildConfig.DEBUG;
    }*/
}

