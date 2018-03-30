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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Created by johannes on 01.01.16.
 * Refactored by Mat
 */

public class SnapClientService extends SnapService {
    public static final String EXTRA_HOST = "EXTRA_HOST";
    public static final String EXTRA_PORT = "EXTRA_PORT";
    private static final String TAG = "SnapClientService";

    @Override
    protected NotificationCompat.Builder createStopNotificationBuilder(Intent intent, PendingIntent piStop) {
        String host = intent.getStringExtra(EXTRA_HOST);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_media_play)
                .setTicker(getText(R.string.ticker_text))
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_text))
                .setContentInfo(host)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getText(R.string.notification_text)))
                .addAction(R.drawable.ic_media_stop, getString(R.string.stop), piStop);
    }

    @Override
    protected int getNotificationId() {
        return 102;
    }

    @Override
    protected void start(Intent intent) {
        String host = intent.getStringExtra(EXTRA_HOST);
        int port = intent.getIntExtra(EXTRA_PORT, 1704);

        try {
            //https://code.google.com/p/android/issues/detail?id=22763
            if (running)
                return;
            File binary = new File(getFilesDir(), "snapclient");
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "SnapcastWakeLock");
            wakeLock.acquire();

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiWakeLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SnapcastWifiWakeLock");
            wifiWakeLock.acquire();

            process = new ProcessBuilder()
                    .command(binary.getAbsolutePath(), "-h", host, "-p", Integer.toString(port))
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            Log.wtf(TAG, "Starting native process", e);
            if (logListener != null)
                logListener.onError(this, e.getMessage(), e);
            stop();
        }
    }
}




