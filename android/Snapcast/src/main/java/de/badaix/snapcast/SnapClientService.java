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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Created by johannes on 01.01.16.
 * Refactored by Mat
 */

public class SnapClientService extends SnapService {

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
}




