package com.jakehorder.activityapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
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

import java.util.Calendar;

public class Session3TimeFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int hour3 = ((MainActivity) getActivity()).s3hour;
        int minute3 = ((MainActivity) getActivity()).s3min;

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour3, minute3,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {

        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        Calendar s3 = Calendar.getInstance();
        s3.set(Calendar.HOUR_OF_DAY, hourOfDay);
        s3.set(Calendar.MINUTE, minute);

        ((MainActivity) getActivity()).s3hour = hourOfDay;
        ((MainActivity) getActivity()).s3min = minute;

        if ((hourOfDay == ((MainActivity)getActivity()).s2hour) ||
                (hourOfDay == ((MainActivity)getActivity()).s2hour + 1 && minute < ((MainActivity)getActivity()).s2min))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Please select a starting time at least 1 hour after the start of Session #2.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else if (hourOfDay < ((MainActivity)getActivity()).s2hour) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Please select a start time after Session #2");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else if ((hourOfDay > current_hour + 7) ||
                (hourOfDay == current_hour + 7 && minute > current_minute))
        {
            Calendar bad = Calendar.getInstance();
            bad.set(Calendar.HOUR_OF_DAY, current_hour+7);
            bad.set(Calendar.MINUTE, current_minute);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("To ensure 8 hours of data collection, please select a time before" + " "+ java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(bad.getTime()));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
        else {
            TextView s3TimeText = (TextView)getActivity().findViewById(R.id.session3time);
            String timeText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s3.getTime());
            s3TimeText.setText(timeText);
        }
    }
}