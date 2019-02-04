package com.mapbox.marlin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Dialog_Speed extends DialogFragment {

    private static int selectedSpeed = 50;

    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        View view = inflater.inflate((R.layout.popup_speed), null);
        SeekBar seekBarSpeed = view.findViewById(R.id.sb_speed);
        final TextView textViewSpeed = view.findViewById(R.id.tv_speed);

        seekBarSpeed.setProgress(selectedSpeed);
        textViewSpeed.setText(selectedSpeed + "%");

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                textViewSpeed.setText("" + progress + "%");
                selectedSpeed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        builder.setView(view);
        return builder.create();
    }

}
