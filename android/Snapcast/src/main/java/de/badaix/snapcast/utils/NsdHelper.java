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

package de.badaix.snapcast.utils;

/**
 * Created by johannes on 19.01.16.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NsdHelper {

    private static final String TAG = "NsdHelper";
    private static NsdHelper mInstance;
    private String serviceName;
    private NsdManager mNsdManager;
    private final NsdManager.DiscoveryListener mDiscoveryListener = new ServiceDiscoveryListener();
    private final NsdManager.ResolveListener mResolveListener = new ServiceResolveListener();
    private NsdHelperListener listener;
    private final NsdManager.RegistrationListener ignoringListener = new NullRegistrationListener();

    public void startListening(String serviceType, String serviceName, NsdHelperListener listener, Context context) {
        stopListening();
        this.listener = listener;
        this.serviceName = serviceName;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopListening() {
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (Exception e) {
            Log.wtf(TAG, e);
        }
    }

    class ServiceResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.d(TAG, "Resolve failed");
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            NsdServiceInfo info = serviceInfo;
            // sometimes it returns an IPv6 address...
            if (!info.getHost().getCanonicalHostName().contains(":"))
                listener.onResolved(NsdHelper.this, serviceInfo);
        }
    }

    class ServiceDiscoveryListener implements NsdManager.DiscoveryListener {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, "Discovery failed");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, "Stopping discovery failed");
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Discovery started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "Discovery stopped");
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            NsdServiceInfo info = serviceInfo;
            Log.d(TAG, "Service found: " + info.getServiceName());
            if (info.getServiceName().equals(serviceName))
                mNsdManager.resolveService(info, mResolveListener);
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            NsdServiceInfo info = serviceInfo;
            Log.d(TAG, "Service lost: " + info.getServiceName());
        }
    }


    class NullRegistrationListener implements NsdManager.RegistrationListener {
        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {

        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {

        }
    }

    public void startAdvertising(String serviceType, String serviceName, int port, Context context) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType(serviceType);
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, ignoringListener);
    }

    public void stopAdvertising() {
        mNsdManager.unregisterService(ignoringListener);
    }

    public interface NsdHelperListener {
        void onResolved(NsdHelper nsdHelper, NsdServiceInfo serviceInfo);
    }
}

