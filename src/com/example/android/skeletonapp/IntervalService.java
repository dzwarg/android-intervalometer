/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.skeletonapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.widget.Toast;
import android.util.Log;

import java.util.HashMap;



// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.skeletonapp.R;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application.  Because it can be in another
 * process, we must use IPC to interact with it.  The
 * {@link RemoteServiceController} and {@link RemoteServiceBinding} classes
 * show how to interact with the service.
 */
public class IntervalService extends Service {
    private static String TAG = "IntervalometerService";
    
    /**
     * This is a list of callbacks that have been registered with the
     * service.  Note that this is package scoped (instead of private) so
     * that it can be accessed more efficiently from inner classes.
     */
    final RemoteCallbackList<IIntervalServiceCallback> mCallbacks
            = new RemoteCallbackList<IIntervalServiceCallback>();
    
    int mInterval = 0;
    NotificationManager mNM;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Log.d(TAG, "showing notification");
        // Display a notification about us starting.
        showNotification();
        
        Log.d(TAG, "Sending empty message.");
        // While this service is running, it will continually increment a
        // number.  Send the first message that is used to perform the
        // increment.
        mHandler.sendEmptyMessage(SNAP_MSG);
    }

    @Override
    public void onDestroy() {
    	Log.d(TAG, "destroying service");
        // Cancel the persistent notification.
        mNM.cancel(R.string.interval_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.interval_service_stopped, Toast.LENGTH_SHORT).show();
        
        // Unregister all callbacks.
        mCallbacks.kill();
        
        // Remove the next pending message to increment the counter, stopping
        // the increment loop.
        mHandler.removeMessages(SNAP_MSG);
    }
    

    @Override
    public IBinder onBind(Intent intent) {
    	Log.d(TAG, "onBind with " + intent);
    	
    	Bundle extras = (Bundle)intent.getExtras();
    	mInterval = extras.getInt("interval");

        return mBinder;
    }

    /**
     * The IRemoteInterface is defined through IDL
     */
    private final IIntervalService.Stub mBinder = new IIntervalService.Stub() {
        public void registerCallback(IIntervalServiceCallback cb) {
        	Log.d(TAG, "Registering callback");
            if (cb != null) mCallbacks.register(cb);
        }
        public void unregisterCallback(IIntervalServiceCallback cb) {
        	Log.d(TAG, "Unregistering callback");
            if (cb != null) mCallbacks.unregister(cb);
        }
    };

    
    private static final int SNAP_MSG = 1;
    
    /**
     * Our Handler used to execute operations on the main thread.  This is used
     * to schedule increments of our value.
     */
    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
        	Log.d(TAG, "handling message in service");
        	Log.d(TAG, "msg.what = " + msg.what);
            switch (msg.what) {
                
                // It is time to bump the value!
                case SNAP_MSG: {                    
                    // Broadcast to all clients the new value.
                    final int N = mCallbacks.beginBroadcast();
                    for (int i=0; i<N; i++) {
                        try {
                        	Log.d(TAG, "broadcast \"valueChanged\" event");
                            mCallbacks.getBroadcastItem(i).valueChanged(0);
                        } catch (RemoteException e) {
                        	Log.e(TAG, "error broadcasting");
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();
                    
                    // Repeat every 1 second.
                    sendMessageDelayed(obtainMessage(SNAP_MSG), mInterval*1000);
                } break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.interval_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SkeletonActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.interval_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.interval_service_started, notification);
    }
}
