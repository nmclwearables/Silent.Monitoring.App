package com.jakehorder.activityapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Session2Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent("SESSION_2"));
    }
}
