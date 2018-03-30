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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.badaix.snapcast.control.json.Client;
import de.badaix.snapcast.control.json.Group;
import de.badaix.snapcast.control.json.ServerStatus;
import de.badaix.snapcast.control.json.Stream;

/**
 * Created by johannes on 06.12.16.
 */

public class GroupSettingsFragment extends PreferenceFragment {

    private static final String TAG = "GroupSettingsFragment";

    private ListPreference prefStreams;
    private Group group = null;
    private ServerStatus serverStatus;
    private PreferenceCategory prefCatClients;

    private String oldClients = "";
    private String oldStream = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.group_preferences);
        PreferenceScreen screen = this.getPreferenceScreen();

        prefStreams = (ListPreference) findPreference("pref_stream");

        Bundle bundle = getArguments();
        try {
            group = new Group(new JSONObject(bundle.getString("group")));
            serverStatus = new ServerStatus(new JSONObject(bundle.getString("serverStatus")));
        } catch (JSONException e) {
            Log.wtf(TAG, "Creating new group and server status", e);
        }

        final List<Stream> streams = serverStatus.getStreams();
        final CharSequence[] streamNames = new CharSequence[streams.size()];
        final CharSequence[] streamIds = new CharSequence[streams.size()];
        for (int i = 0; i < streams.size(); ++i) {
            streamNames[i] = streams.get(i).getName();
            streamIds[i] = streams.get(i).getId();
        }

        prefStreams.setEntries(streamNames);
        prefStreams.setEntryValues(streamIds);

        for (int i = 0; i < streams.size(); ++i) {
            if (streamIds[i].equals(group.getStreamId())) {
                prefStreams.setTitle(streamNames[i]);
                prefStreams.setValueIndex(i);
                oldStream = prefStreams.getValue();
                break;
            }
        }

        prefStreams.setOnPreferenceChangeListener((preference, newValue) -> {
            for (int i = 0; i < streams.size(); ++i) {
                if (streamIds[i].equals(newValue)) {
                    prefStreams.setTitle(streamNames[i]);
//                        client.getConfig().setStream(streamIds[i].toString());
                    prefStreams.setValueIndex(i);
                    break;
                }
            }

            return false;
        });


        prefCatClients = (PreferenceCategory) findPreference("pref_cat_clients");
        ArrayList<CheckBoxPreference> allClients = new ArrayList<>();
        for (Group g : serverStatus.getGroups()) {
            for (Client client : g.getClients()) {
                CheckBoxPreference checkBoxPref = new CheckBoxPreference(screen.getContext());
                checkBoxPref.setKey(client.getId());
                checkBoxPref.setTitle(client.getVisibleName());
                checkBoxPref.setChecked(g.getId().equals(group.getId()));
                checkBoxPref.setPersistent(false);
                allClients.add(checkBoxPref);
            }
        }
        Collections.sort(allClients, (lhs, rhs) -> lhs.getTitle().toString().compareToIgnoreCase(rhs.getTitle().toString()));
        for (CheckBoxPreference pref : allClients)
            prefCatClients.addPreference(pref);

        oldClients = getClients().toString();
    }

    public ArrayList<String> getClients() {
        ArrayList<String> clients = new ArrayList<>();
        for (int i = 0; i < prefCatClients.getPreferenceCount(); ++i) {
            CheckBoxPreference checkBoxPref = (CheckBoxPreference) prefCatClients.getPreference(i);
            if (checkBoxPref.isChecked())
                clients.add(checkBoxPref.getKey());
        }
        return clients;
    }


    public boolean didStreamChange() {
        return !oldStream.equals(prefStreams.getValue());
    }

    public boolean didClientsChange() {
        return !oldClients.equalsIgnoreCase(getClients().toString());
    }

    public String getStream() {
        return prefStreams.getValue();
    }

    public Group getGroup() {
        return group;
    }
}