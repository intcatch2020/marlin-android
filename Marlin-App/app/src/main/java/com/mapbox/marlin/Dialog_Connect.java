package com.mapbox.marlin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Dialog_Connect extends DialogFragment {

    public static String ip = "xxx.xxx.xxx.xxx";

    private String pc_server_ip = "157.27.198.83"; //server pc
    private String boat_server_ip = "192.168.2.1"; //server boat

    private int mode = 0;

    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        View view = inflater.inflate((R.layout.popup_connect), null);
        final EditText editText = (EditText) view.findViewById(R.id.connect_password);
        final Spinner spinner = (Spinner) view.findViewById(R.id.connect_spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mode = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setView(view)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Done action
                        switch (mode){
                            case 0:
                                ip = boat_server_ip;
                                break;
                            case 1:
                                ip = pc_server_ip;
                                break;
                            default:
                                ip = "xxx.xxx.xxx.xxx";
                        }

                        spinner.getSelectedItemId();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Cancel
                    }

                });

        return builder.create();
    }

}