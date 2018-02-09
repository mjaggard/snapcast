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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import uk.org.jaggard.snapcast.AdDetails;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Created by Mat
 */

public class SnapServerService extends SnapService {

    private static final String TAG = "Server";

    @Override
    protected NotificationCompat.Builder createStopNotificationBuilder(Intent intent, PendingIntent piStop) {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_media_play)
                .setTicker(getText(R.string.ticker_text_server))
                .setContentTitle(getText(R.string.notification_title_server))
                .setContentText(getText(R.string.notification_text_server))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getText(R.string.notification_text_server)))
                .addAction(R.drawable.ic_media_stop, getString(R.string.stop), piStop);
    }

    @Override
    protected void start(Intent intent) {
        try {
            //https://code.google.com/p/android/issues/detail?id=22763
            if (running)
                return;
            File binary = new File(getFilesDir(), "snapserver");
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "SnapcastWakeLock");
            wakeLock.acquire();

            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiWakeLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SnapcastWifiWakeLock");
            wifiWakeLock.acquire();
            String loc = getFilesDir().getAbsolutePath();
            String spotifyString = "spotify:///"+loc+"/librespot?name=Spotify&username=MatJaggard&password=" + AdDetails.MY_PASSWORD + "&devicename=Snapcast&bitrate=320";

            launchLibReSpot();

            ProcessBuilder pb = new ProcessBuilder();
            Map<String,String> env = pb.environment();
            env.put("HOME", binary.getParentFile().getAbsolutePath());
            process = pb
                    .command(binary.getAbsolutePath(), "-s", spotifyString)
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            Log.e(TAG, "Exception caught while starting server", e);
            if (logListener != null)
                logListener.onError(this, e.getMessage(), e);
            stop();
        }
    }

    private void launchLibReSpot() {
        File binary = new File(getFilesDir(), "librespot");
        ProcessBuilder pb = new ProcessBuilder();
        try {
            Process libRespotProcess = pb
                    .command(binary.getAbsolutePath(), "--disable-audio-cache", "-b", "320", "-v", "-u", "MatJaggard", "-p", AdDetails.MY_PASSWORD, "--disable-discovery", "--backend", "pipe", "--name", "MatLibRespot")
                    .redirectErrorStream(true)
                    .start();

            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(libRespotProcess.getInputStream()));
                    String line;
                    try {
                        while ((line = bufferedReader.readLine()) != null) {
                            logFromNative(line);
                        }
                        libRespotProcess.waitFor();
                    } catch (IOException | InterruptedException e) {
                        Log.e(TAG, "Problem getting output from librespot", e);
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Problem running librespot", e);
        }
    }
}

