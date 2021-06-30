package com.jakehorder.activityapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;


public class Session2TimeFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int hour2 = ((MainActivity) getActivity()).s2hour;
        int minute2 = ((MainActivity) getActivity()).s2min;

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour2, minute2,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

        Calendar s2 = Calendar.getInstance();
        s2.set(Calendar.HOUR_OF_DAY, hourOfDay);
        s2.set(Calendar.MINUTE, minute);

        ((MainActivity)getActivity()).s2hour = hourOfDay;
        ((MainActivity)getActivity()).s2min = minute;


        if ((hourOfDay == ((MainActivity)getActivity()).s1hour) ||
                (hourOfDay == ((MainActivity)getActivity()).s1hour + 1 && minute < ((MainActivity)getActivity()).s1min))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Please select a starting time at least 1 hour after the start of Session #1.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else if (hourOfDay < ((MainActivity)getActivity()).s1hour) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Please select a time after Session #1.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else {
            TextView s2TimeText = (TextView)getActivity().findViewById(R.id.session2time);
            String timeText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s2.getTime());
            s2TimeText.setText(timeText);
        }
    }
}