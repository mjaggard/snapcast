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
import java.util.Map;

import de.badaix.snapcast.utils.NsdHelper;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Created by Mat
 */

public class SnapServerService extends SnapService {
    private static final String TAG = "Server";
    public static final String SPOTIFY_USERNAME = "SPOTIFY_USERNAME";
    public static final String SPOTIFY_PASSWORD = "snapcast";
    private final NsdHelper nsdHelper = new NsdHelper();

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
    protected int getNotificationId() {
        return 101;
    }

    @Override
    protected void stop() {
        nsdHelper.stopAdvertising();
        super.stop();
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

            String spotifyUsername = intent.getStringExtra(SPOTIFY_USERNAME);
            String spotifyPassword = intent.getStringExtra(SPOTIFY_PASSWORD);

            File cacheDir = new File(getCacheDir(), "librespot");
            cacheDir.mkdirs();
            String spotifyString = "spotify:///" + loc + "/librespot?name=Spotify&username=" + spotifyUsername + "&password=" + spotifyPassword + "&devicename=Snapcast&bitrate=320&cache=" + cacheDir.getAbsolutePath();

            //launchLibReSpot(intent);

            ProcessBuilder pb = new ProcessBuilder();
            Map<String,String> env = pb.environment();
            env.put("HOME", cacheDir.getAbsolutePath());
            env.put("TMPDIR", cacheDir.getAbsolutePath());
            process = pb
                    .command(binary.getAbsolutePath(), "-s", spotifyString)
                    .redirectErrorStream(true)
                    .start();

            nsdHelper.startAdvertising("_snapcast._tcp.", "Snapcast", 1704, this);
        } catch (Exception e) {
            Log.e(TAG, "Exception caught while starting server", e);
            if (logListener != null)
                logListener.onError(this, e.getMessage(), e);
            stop();
        }
    }

    private void launchLibReSpot(Intent intent) {
        File cacheDir = new File(getCacheDir(), "librespot");
        cacheDir.mkdirs();
        File binary = new File(getFilesDir(), "librespot");
        ProcessBuilder pb = new ProcessBuilder();
        try {
            String spotifyUsername = intent.getStringExtra(SPOTIFY_USERNAME);
            String spotifyPassword = intent.getStringExtra(SPOTIFY_PASSWORD);

            Process libRespotProcess = pb
                    .command(binary.getAbsolutePath(), "--cache", cacheDir.getAbsolutePath(), "-b", "320", "-v", "-u", spotifyUsername, "-p", spotifyPassword, "--backend", "pipe", "--name", "MatLibRespot")
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
            reader.start();
        } catch (IOException e) {
            Log.e(TAG, "Problem running librespot", e);
        }
    }
}
