/*
 *     This file is part of snapcast
 *     Copyright (C) 2014-2017  Johannes Pohl
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.badaix.snapcast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by mat on 23/01/18.
 */
public abstract class SnapService extends Service {
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    private static final String TAG = "SnapService";
    private final IBinder mBinder = new LocalBinder();
    protected Process process;
    protected PowerManager.WakeLock wakeLock;
    protected WifiManager.WifiLock wifiWakeLock;
    protected boolean running;
    protected LogListener logListener;
    protected boolean logReceived;
    private StopListener stopListener;
    private StartListener startListener;

    public boolean isRunning() {
        return running;
    }

    public void setLogListener(LogListener listener) {
        this.logListener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;

        if (intent.getAction().equals(ACTION_STOP)) {
            stopService();
            return START_NOT_STICKY;
        } else if (intent.getAction().equals(ACTION_START)) {
            Intent stopIntent = new Intent(this, getClass());
            stopIntent.setAction(ACTION_STOP);
            PendingIntent piStop = PendingIntent.getService(this, 0, stopIntent, 0);

            NotificationCompat.Builder builder = createStopNotificationBuilder(intent, piStop);

            Intent resultIntent = new Intent(this, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            builder.setContentIntent(resultPendingIntent);
            // mId allows you to update the notification later on.
            final Notification notification = builder.build();
            startForeground(getNotificationId(), notification);

            start(intent);


            Thread reader = new Thread(() -> {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        logFromNative(line);
                    }
                    process.waitFor();
                    stop();
                } catch (IOException | InterruptedException e) {
                    Log.wtf(TAG, "Stopping native process", e);
                }
            });
            logReceived = false;
            reader.start();
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    protected abstract void start(Intent intent);

    protected abstract NotificationCompat.Builder createStopNotificationBuilder(Intent intent, PendingIntent piStop);

    protected abstract int getNotificationId();

    @Override
    public void onDestroy() {
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void stopService() {
        stop();
        stopForeground(true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(getNotificationId());
    }

    protected void logFromNative(String msg) {
        if (!logReceived) {
            logReceived = true;
            running = true;
            if (startListener != null)
                startListener.onStart(this);
        }
        if (logListener != null) {
            int idxBracketOpen = msg.indexOf('[');
            int idxBracketClose = msg.indexOf(']', idxBracketOpen);
            if ((idxBracketOpen > 0) && (idxBracketClose > 0)) {
                try {
                    logListener.onLog(SnapService.this, msg.substring(0, idxBracketOpen - 1), msg.substring(idxBracketOpen + 1, idxBracketClose), msg.substring(idxBracketClose + 2));
                } catch (StringIndexOutOfBoundsException sioobe) {
                    logListener.onLog(SnapService.this,"", "", msg);
                }
            }
        }
    }

    protected void stop() {
        try {
            if (process != null)
                process.destroy();
            if ((wakeLock != null) && wakeLock.isHeld())
                wakeLock.release();
            if ((wifiWakeLock != null) && wifiWakeLock.isHeld())
                wifiWakeLock.release();
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
            running = false;
        } catch (Exception e) {
            Log.wtf(TAG, "Releasing logs and destroying process", e);
        }
        if (stopListener != null)
            stopListener.onStop(this);
    }

    public void setStopListener(StopListener stopListener) {
        this.stopListener = stopListener;
    }

    public void setStartListener(StartListener startListener) {
        this.startListener = startListener;
    }

    public interface LogListener {
        void onLog(SnapService snapService, String timestamp, String logClass, String msg);

        void onError(SnapService snapService, String msg, Exception exception);
    }

    public interface StartListener {
        void onStart(SnapService snapService);
    }

    public interface StopListener {
        void onStop(SnapService snapService);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SnapService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SnapService.this;
        }
    }
}
