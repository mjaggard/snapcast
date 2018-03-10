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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import de.badaix.snapcast.utils.Settings;

/**
 * Created by johannes on 05.05.16.
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Settings settings = new Settings(context);
            if (settings.isAutostart()) {
                String host = settings.getHost();
                int port = settings.getStreamPort();
                if (TextUtils.isEmpty(host))
                    return;

                Intent i = new Intent(context, SnapClientService.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra(SnapClientService.EXTRA_HOST, host);
                i.putExtra(SnapClientService.EXTRA_PORT, port);
                i.setAction(SnapClientService.ACTION_START);

                context.startService(i);
            }
        }
    }
}


