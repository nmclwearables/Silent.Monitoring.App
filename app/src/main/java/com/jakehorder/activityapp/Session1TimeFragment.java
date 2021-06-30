package com.jakehorder.activityapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.text.format.DateFormat;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.timepicker.MaterialTimePicker;

import java.util.Calendar;
import java.util.List;


public class Session1TimeFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int hour = ((MainActivity)getActivity()).s1hour;
        int minute = ((MainActivity)getActivity()).s1min;

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        Calendar s1 = Calendar.getInstance();
        s1.set(Calendar.HOUR_OF_DAY, hourOfDay);
        s1.set(Calendar.MINUTE, minute);

        ((MainActivity)getActivity()).s1hour = hourOfDay;
        ((MainActivity)getActivity()).s1min = minute;

        if ((hourOfDay < current_hour))
                //|| (hourOfDay == current_hour && minute < current_minute + 30) ||
                //(hourOfDay == current_hour + 1 && minute < current_minute - 30))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Please select a starting time for Session #1 at least 30 minutes after the current time.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else
        {
            TextView s1TimeText = (TextView)getActivity().findViewById(R.id.session1time);
            String timeText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s1.getTime());
            s1TimeText.setText(timeText);
        }
    }
}