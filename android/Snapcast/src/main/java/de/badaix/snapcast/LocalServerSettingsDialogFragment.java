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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Mat
 */
public class LocalServerSettingsDialogFragment extends DialogFragment {

    private static final String TAG = "LocalServerSettings";

    private EditText editSpotifyUsername;
    private EditText editSpotifyPassword;
    private String spotifyUsername = "";
    private String spotifyPassword = "";
    private SpotifyCredentialsListener spotifyCredentialsListener;

    public void setSpotifyCredentialsListener(SpotifyCredentialsListener spotifyCredentialsListener) {
        this.spotifyCredentialsListener = spotifyCredentialsListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_local_server_settings, null);
        editSpotifyUsername = (EditText) view.findViewById(R.id.spotifyUsername);
        editSpotifyPassword = (EditText) view.findViewById(R.id.spotifyPassword);
        update();

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    // sign in the user ...
                    spotifyUsername = editSpotifyUsername.getText().toString();
                    spotifyPassword = editSpotifyPassword.getText().toString();
                    if (spotifyCredentialsListener != null) {
                        spotifyCredentialsListener.onSpotifyCredentialsChanged(spotifyUsername, spotifyPassword);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        LocalServerSettingsDialogFragment.this.getDialog().cancel();
                    }
                })
                .setTitle(R.string.local_server)
                .setCancelable(false);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            update();
        }
    }

    private void update() {
        try {
            Runnable runnable = () -> {
                try {
                    editSpotifyUsername.setText(spotifyUsername);
                    editSpotifyPassword.setText(spotifyPassword);
                } catch (Exception e) {
                    Log.wtf(TAG, "Error setting UI details", e);
                }
            };
            if (getActivity() != null) {
                getActivity().runOnUiThread(runnable);
            } else {
                new Handler(Looper.getMainLooper()).post(runnable);
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Error setting UI details", e);
        }
    }

    public void setSpotifyCredentials(String username, String password) {
        this.spotifyUsername = username;
        this.spotifyPassword = password;
        update();
    }

    public interface SpotifyCredentialsListener {
        void onSpotifyCredentialsChanged(String username, String password);
    }
}
