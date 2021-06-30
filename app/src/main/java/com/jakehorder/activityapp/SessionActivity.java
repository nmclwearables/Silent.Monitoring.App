package com.jakehorder.activityapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bolts.Continuation;
import bolts.Task;

public class SessionActivity extends AppCompatActivity {

    // Global variables
        public static TextView textViewStatus;
        public static TextView textViewMessage;
        public static LottieAnimationView lav;
        public static LottieAnimationView lav_dots;
        public static TextView s1TimeText;
        public static TextView s2TimeText;
        public static TextView s3TimeText;
        public static TextView endTimeText;

        public static ImageView leftStatus;
        public static ImageView leftWatch;
        public static ImageView leftBatImage;
        public static TextView leftBat;

        public static ImageView rightStatus;
        public static ImageView rightWatch;
        public static ImageView rightBatImage;
        public static TextView rightBat;

        public static ProgressBar progressCircle;
        public static TextView textCue;
        public static TextView textViewProgress;

        public Handler s1handler;
        public static Handler UIhandler;

        public String CHANNEL_ID;
        public Intent intentNotif;

        private Animation anim;

        public boolean leftConnection;
        public boolean rightConnection;

        public boolean file1Success;
        public boolean file2Success;
        public boolean file3Success;

        DriveServiceHelper driveServiceHelper;

    @Override
    public void onBackPressed()
    {
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Authorize access to Google Drive API
        // Only appears to user for the first time the application is run
            requestSignIn();

        // Register receivers for session and ending alarms
            registerReceiver(uploadReceiver, new IntentFilter("UPLOAD_DATA"));
            registerReceiver(session1Receiver, new IntentFilter("SESSION_1"));
            registerReceiver(session2Receiver, new IntentFilter("SESSION_2"));
            registerReceiver(session3Receiver, new IntentFilter("SESSION_3"));
            registerReceiver(KillReceiver, new IntentFilter("KILL_APP"));

        // Initialize UI components
            textViewStatus = findViewById(R.id.textStatus);
            textViewMessage = findViewById(R.id.textMessage);
            lav = findViewById(R.id.lav_main);

            s1TimeText = findViewById(R.id.s1current);
            s2TimeText = findViewById(R.id.s2current);
            s3TimeText = findViewById(R.id.s3current);
            endTimeText = findViewById(R.id.textEndTime);

            leftBat = findViewById(R.id.leftBatteryText);
            rightBat = findViewById(R.id.rightBatteryText);

            leftStatus = findViewById(R.id.leftStatusIcon2);
            rightStatus = findViewById(R.id.rightStatusIcon2);

            leftBatImage = findViewById(R.id.imageLeftBattery2);
            rightBatImage = findViewById(R.id.imageRightBattery2);

            leftWatch = findViewById(R.id.imageLeftWatch2);
            rightWatch = findViewById(R.id.imageRightWatch2);

            progressCircle = findViewById(R.id.progressCircle);
            textCue = findViewById(R.id.textCueStatus);
            textViewProgress = findViewById(R.id.textProgress);

            leftConnection = true;
            rightConnection = true;

            file1Success = false;
            file2Success = false;
            file3Success = false;

        // Initialize notifications
            createNotificationChannel();
            intentNotif = new Intent(this, SessionActivity.class);

        // Pull session times from MainActivity class
            Intent intent = getIntent();
            int s1hour = intent.getIntExtra("s1hour", 0);
            int s1min = intent.getIntExtra("s1min", 0);
            int s2hour = intent.getIntExtra("s2hour", 0);
            int s2min = intent.getIntExtra("s2min", 0);
            int s3hour = intent.getIntExtra("s3hour", 0);
            int s3min = intent.getIntExtra("s3min", 0);

        // create calendars for session times and ending time
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            int current_hour = c.get(Calendar.HOUR_OF_DAY);
            int current_minute = c.get(Calendar.MINUTE);

            Calendar s1 = Calendar.getInstance();
            s1.set(Calendar.HOUR_OF_DAY, s1hour);
            s1.set(Calendar.MINUTE, s1min);
            String time1Text = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s1.getTime());

            Calendar s2 = Calendar.getInstance();
            s2.set(Calendar.HOUR_OF_DAY, s2hour);
            s2.set(Calendar.MINUTE, s2min);
            String time2Text = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s2.getTime());

            Calendar s3 = Calendar.getInstance();
            s3.set(Calendar.HOUR_OF_DAY, s3hour);
            s3.set(Calendar.MINUTE, s3min);
            String time3Text = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(s3.getTime());

            Calendar end = Calendar.getInstance(); // ending alarm 8 hours after current time
            end.set(Calendar.HOUR_OF_DAY, current_hour);
            end.set(Calendar.MINUTE, current_minute+2);
            String endText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(end.getTime());

            Calendar kill = Calendar.getInstance(); // kill app 12 hours after current time
            kill.set(Calendar.HOUR_OF_DAY, current_hour+12);
            kill.set(Calendar.MINUTE, current_minute);
            String killText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(end.getTime());

        // Display session times
            s1TimeText.setText(time1Text);
            s2TimeText.setText(time2Text);
            s3TimeText.setText(time3Text);
            endTimeText.setText(endText);

        // Schedule battery update interval every 10 minutes
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate
                    (new Runnable()
                    {
                        public void run()
                        {
                            batteryUpdate();
                        }
                    }, 0, 10, TimeUnit.MINUTES);       // 10-minute interval

        // Update UI on disconnects/reconnects
            UIhandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:                             // left disconnected
                        // Update UI
                            leftBat.setVisibility(View.INVISIBLE);
                            leftBatImage.setVisibility(View.INVISIBLE);
                            leftStatus.setImageResource(R.drawable.ic_baseline_close_24);
                            makeMeBlink(leftWatch);
                        //update flag
                            leftConnection = false;
                            break;

                        case 2:                             //right disconnected
                            rightBat.setVisibility(View.INVISIBLE);
                            rightBatImage.setVisibility(View.INVISIBLE);
                            rightStatus.setImageResource(R.drawable.ic_baseline_close_24);
                            makeMeBlink(rightWatch);
                        // update flag
                            rightConnection = false;
                            break;

                        case 3:                             // left re-connected
                        // update UI
                            leftBat.setVisibility(View.VISIBLE);
                            leftBatImage.setVisibility(View.VISIBLE);
                            leftStatus.setImageResource(R.drawable.ic_baseline_wifi_24);
                            stopBlink(leftWatch);
                        // update flag
                            leftConnection = true;
                            break;

                        case 4:                             // right re-connected
                            // update UI
                            rightBat.setVisibility(View.VISIBLE);
                            rightBatImage.setVisibility(View.VISIBLE);
                            rightStatus.setImageResource(R.drawable.ic_baseline_wifi_24);
                            stopBlink(rightWatch);
                            // update flag
                            rightConnection = true;
                            break;
                    }
                }
            };
        // Handler for updating session progress
            s1handler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                        // get current time for cue timestamp
                        LocalTime timestamp = LocalTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

                        // check if device is connected
                        if(!leftConnection) {
                            // log unsuccessful cue time
                                try (PrintWriter p = new PrintWriter(new FileOutputStream(MainActivity.path_cues, true))) {
                                    p.println(timestamp.format(formatter) + "," + "No connection");
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                    Log.e("METAWEAR", "cues CSV creation error", e1);
                                }
                            // do not cue device or update UI
                        } else {
                            // log cue time
                                try (PrintWriter p = new PrintWriter(new FileOutputStream(MainActivity.path_cues, true))) {
                                    p.println(timestamp.format(formatter));
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                    Log.e("METAWEAR", "cues CSV creation error", e1);
                                }
                            // Cue device
                                try { MainActivity.board1.getModuleOrThrow(Haptic.class)
                                        .startMotor(80.f, (short) 2000);
                                } catch (UnsupportedModuleException e) {
                                    e.printStackTrace();
                                    Log.i("METAWEAR", "device not connected?");
                                }
                            // update UI
                                progressCircle.incrementProgressBy(1);
                                String progressText = progressCircle.getProgress()-1 +"/60";
                                textViewProgress.setText(progressText);
                                Log.i("Metawear", "cue#"+(progressCircle.getProgress()-1));
                            }
                            break;
                        case 2:
                            textViewStatus.setText("Collecting Data...");
                            s1TimeText.setTextColor(Color.BLACK);
                            s1TimeText.setTypeface(null, Typeface.NORMAL);
                            s2TimeText.setTextColor(Color.BLACK);
                            s2TimeText.setTypeface(null, Typeface.NORMAL);
                            s3TimeText.setTextColor(Color.BLACK);
                            s3TimeText.setTypeface(null, Typeface.NORMAL);

                            textViewProgress.setText("");
                            textViewProgress.setVisibility(View.INVISIBLE);
                            textCue.setVisibility(View.INVISIBLE);
                            progressCircle.setVisibility(View.INVISIBLE);
                            progressCircle.setIndeterminate(true);
                            progressCircle.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            };
        // Start alarm for session 1
            startS1Alarm(s1);

        // Start alarm for session 2
            startS2Alarm(s2);

        // Start alarm for session 3
            startS3Alarm(s3);

        // Start alarm for end of session
            startUploadAlarm(end);
            Log.i("METAWEAR", "End time:" + endText);

        // start alarm for killing app
            startKillAlarm(kill);

    }
    @Override
    public void onDestroy() {

        super.onDestroy();

        int id = android.os.Process.myPid();
        android.os.Process.killProcess(id);
    }


    public void makeMeBlink(View view)
    {
        anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(600);
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        view.startAnimation(anim);
    }
    public void stopBlink(View view)
    {
        view.clearAnimation();
    }

    //==============================================================================================
    // This method updates the battery % of the devices
    //==============================================================================================
    public void batteryUpdate() {
        Log.i("METAWEAR", "Battery text updated!");

        if (leftConnection) {
            MainActivity.board1.readBatteryLevelAsync().continueWith(new Continuation<Byte, Object>() {
                @Override
                public Object then(Task<Byte> task) throws Exception {
                    leftBat.setText(String.format(Locale.US, "%d%%", task.getResult()));
                    Log.i("METAWEAR", "Left Battery:" + task.getResult());
                    if (task.getResult() > 60) {
                        leftBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorGreen));
                    } else if (task.getResult() < 20) {
                        leftBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorRed));
                    } else {
                        leftBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorYellow));
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        } else {
            Log.i("METAWEAR", "Tried udpating battery but left disconnected");
        }

        if (rightConnection) {
            MainActivity.board2.readBatteryLevelAsync().continueWith(new Continuation<Byte, Object>() {
                @Override
                public Object then(Task<Byte> task) throws Exception {
                    rightBat.setText(String.format(Locale.US, "%d%%", task.getResult()));
                    Log.i("METAWEAR", "Right Battery:" + task.getResult());
                    if (task.getResult() > 60) {
                        rightBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorGreen));
                    } else if (task.getResult() < 20) {
                        rightBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorRed));
                    } else {
                        rightBatImage.setColorFilter(ContextCompat.getColor(SessionActivity.this.getApplicationContext(), R.color.colorYellow));
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        } else {
            Log.i("METAWEAR", "Tried udpating battery but right disconnected");
        }
    }

    //==============================================================================================
    // Session 1 Alarm Scheduler
    //      This method initializes the alarm for the start of session 1.
    //==============================================================================================
    private void startS1Alarm(Calendar c)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, Session1Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }
    BroadcastReceiver session1Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("METAWEAR", "session 1 initiating.....");

            // Update UI
                textViewStatus.setText("Session 1");                        // set status to session 1
                s1TimeText.setTextColor(getColor(R.color.ColorAccent2));    // highlight current session time
                s1TimeText.setTypeface(null, Typeface.BOLD);

                textCue.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.VISIBLE);
                textViewProgress.setText("0/60");

                progressCircle.setIndeterminate(false);                     // update progress circle
                progressCircle.setMax(61);
                progressCircle.setProgress(1, true);     // initialize progress = 0

            // Turn on LED at session start time
            try {
                MainActivity.led_left.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                MainActivity.led_left.play();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.led_left.stop(true);
                        Log.i("METAWEAR", "LED warning off");
                    }
                }, 9000);
            } catch(Exception e)
            {
                Log.i("METAWEAR", "Couldnt give warnings");
            }

            // Ping device three times at session start time
                for (int i = 1; i < 4; i++)
                {
                    int finalI = i;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                MainActivity.board1.getModuleOrThrow(Haptic.class)
                                        .startMotor(60.f, (short) 1000);
                                Log.i("METAWEAR", "warning #" + finalI);
                            } catch (UnsupportedModuleException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 2000*finalI);
                }

            // Begin cueing device 60 times
                int [] cueTimeStamps = new int[60];
                for (int i = 0; i < cueTimeStamps.length; i++)
                {
                    cueTimeStamps[i] = 30*i;
                }
                Arrays.sort(cueTimeStamps);

                Log.i("METAWEAR", "Timestamps:" + Arrays.toString(cueTimeStamps));

                for (int i = 1; i < cueTimeStamps.length; i++)
                {
                    s1handler.sendEmptyMessageDelayed(1, 1000*cueTimeStamps[i]);
                }

            // On session completion, revert UI to collection state
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("METAWEAR", "session 1 complete!");
                        s1handler.sendEmptyMessage(2);
                    }
                }, 10000 + cueTimeStamps[59]*1000);
        }
    };

    //==============================================================================================
    // Session 2 Alarm Scheduler
    //      This method initializes the alarm for the start of session 2.
    //==============================================================================================
    private void startS2Alarm(Calendar c)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, Session2Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 2, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }
    BroadcastReceiver session2Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("METAWEAR", "session 2 initiating.....");

            // Update UI
            //lav_dots.setVisibility(View.INVISIBLE);                       // make animated dots invisible
            textViewStatus.setText("Session 2");                        // set status to session 1
            s2TimeText.setTextColor(getColor(R.color.ColorAccent2));    // highlight current session time
            s2TimeText.setTypeface(null, Typeface.BOLD);

            textCue.setVisibility(View.VISIBLE);
            textViewProgress.setVisibility(View.VISIBLE);
            textViewProgress.setText("0/60");

            progressCircle.setIndeterminate(false);                     // update progress circle
            progressCircle.setMax(61);
            progressCircle.setProgress(1, true);     // initialize progress = 0

            // Turn on LED at session start time
            try {
                MainActivity.led_left.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                MainActivity.led_left.play();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.led_left.stop(true);
                        Log.i("METAWEAR", "LED warning off");
                    }
                }, 9000);
            } catch(Exception e)
            {
                Log.i("METAWEAR", "Couldnt give warnings");
            }

            // Ping device three times at session start time
            for (int i = 1; i < 4; i++)
            {
                int finalI = i;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.board1.getModuleOrThrow(Haptic.class)
                                    .startMotor(60.f, (short) 1000);
                            Log.i("METAWEAR", "warning #" + finalI);
                        } catch (UnsupportedModuleException e) {
                            e.printStackTrace();
                        }
                    }
                }, 2000*finalI);
            }

            // Begin cueing device after 3-minute buffer window at the start
            int [] cueTimeStamps = new int[60];
            for (int i = 0; i < cueTimeStamps.length; i++)
            {
                cueTimeStamps[i] = 30*i;
            }
            Arrays.sort(cueTimeStamps);

            Log.i("METAWEAR", "Timestamps:" + Arrays.toString(cueTimeStamps));

            for (int i = 0; i < cueTimeStamps.length; i++)
            {
                s1handler.sendEmptyMessageDelayed(1, 1000*cueTimeStamps[i]);
            }

            // On session completion, revert UI to collection state
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i("METAWEAR", "session 2 complete!");
                    s1handler.sendEmptyMessage(2);
                }
            }, 10000 + cueTimeStamps[59]*1000);
        }
    };

    private void startS3Alarm(Calendar c)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, Session3Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 3, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }
    BroadcastReceiver session3Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("METAWEAR", "session 3 initiating.....");

            // Update UI
            textViewStatus.setText("Session 3");                        // set status to session 1
            s3TimeText.setTextColor(getColor(R.color.ColorAccent2));    // highlight current session time
            s3TimeText.setTypeface(null, Typeface.BOLD);

            textCue.setVisibility(View.VISIBLE);
            textViewProgress.setVisibility(View.VISIBLE);
            textViewProgress.setText("0/60");

            progressCircle.setIndeterminate(false);                     // update progress circle
            progressCircle.setMax(61);
            progressCircle.setProgress(1, true);     // initialize progress = 0

            // Turn on LED at session start time
            try {
                MainActivity.led_left.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID).commit();
                MainActivity.led_left.play();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.led_left.stop(true);
                        Log.i("METAWEAR", "LED warning off");
                    }
                }, 9000);
            } catch(Exception e)
            {
                Log.i("METAWEAR", "Couldnt give warnings");
            }

            // Ping device three times at session start time
            for (int i = 1; i < 4; i++)
            {
                int finalI = i;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.board1.getModuleOrThrow(Haptic.class)
                                    .startMotor(60.f, (short) 1000);
                            Log.i("METAWEAR", "warning #" + finalI);
                        } catch (UnsupportedModuleException e) {
                            e.printStackTrace();
                        }
                    }
                }, 2000*finalI);
            }

            // Begin cueing device after 5-minute buffer window at the start
            int [] cueTimeStamps = new int[60];
            for (int i = 0; i < cueTimeStamps.length; i++)
            {
                cueTimeStamps[i] = 30*i;
            }
            Arrays.sort(cueTimeStamps);

            Log.i("METAWEAR", "Timestamps:" + Arrays.toString(cueTimeStamps));

            for (int i = 0; i < cueTimeStamps.length; i++)
            {
                s1handler.sendEmptyMessageDelayed(1, 1000*cueTimeStamps[i]);
            }

            // On session completion, revert UI to collection state
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i("METAWEAR", "session 3 complete!");
                    s1handler.sendEmptyMessage(2);
                }
            }, 10000 + cueTimeStamps[59]*1000);
        }
    };

    //==============================================================================================
    // The following methods are used for end-of-day activities:
    //      startAlarm()            initializes AlarmManager 8 hours after initial app access
    //      broadcastReceiver       stops collecting data & uploads data files
    //==============================================================================================
    private void startUploadAlarm(Calendar c)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, UploadReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 4, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Stop collecting accelerometer data
            try {
                MainActivity.acc1.stop();
                MainActivity.acc2.stop();
                Log.i("MetaWear", "Stopped data collection");
            } catch (Exception e)
            {
                Log.i("MetaWear", "Can't stop collecting");
            }

            // hide old UI components from session
                progressCircle.setVisibility(View.INVISIBLE);
                SessionActivity.textViewStatus.setText("");
                SessionActivity.lav.setVisibility(View.VISIBLE);
                SessionActivity.lav.playAnimation();
                textViewMessage.setText("");
                endTimeText.setText("");

            // upload files
                String filePath1 = "/storage/emulated/0/Download/" + MainActivity.filename + ".csv";
                String filePath2 = "/storage/emulated/0/Download/" + MainActivity.filename2 + ".csv";
                String filePathcues = "/storage/emulated/0/Download/" + MainActivity.filename_cues + ".csv";

                uploadFiles(filePath1, MainActivity.filename, filePath2, MainActivity.filename2, filePathcues, MainActivity.filename_cues);
        }
    };


    //==============================================================================================
    // The following methods are used for end-of-day activities:
    //      startAlarm()            initializes AlarmManager 8 hours after initial app access
    //      broadcastReceiver       stops collecting data & uploads data files
    //==============================================================================================
    private void startKillAlarm(Calendar c)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, KillReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 5, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }
    BroadcastReceiver KillReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("METAWEAR", "Lets end this");

            finishAffinity();
            System.exit(0);
        }
    };

    //==============================================================================================
    // The following Methods are used for authenticating the Google Drive API:
    //      requestSignIn()         prompts user to authorize Google Drive Access
    //      onActivityResult()      used for authenticating access
    //      handleSignInIntent()    signs in and creates new Google Drive service
    //      uploadFile()            uploads CSV files to Google Drive
    //==============================================================================================
    private void requestSignIn()
    {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        startActivityForResult(client.getSignInIntent(), 400);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case 400:
                if(resultCode == RESULT_OK)
                {
                    handleSignInIntent(data);
                }
            break;
        }
    }

    private void handleSignInIntent(Intent data)
    {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                    // user has successfully signed in

                        GoogleAccountCredential credential = GoogleAccountCredential
                                .usingOAuth2(SessionActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));

                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        Drive googleDriveService = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("Activity App")
                                .build();

                        driveServiceHelper = new DriveServiceHelper(googleDriveService);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    public void uploadFiles(String filePath1, String fileName1, String filePath2, String fileName2, String filePath3, String fileName3)
    {
        //String filePath = "/storage/emulated/0/mypdf.pdf";
        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + MainActivity.filename + ".csv";
        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "2_23_10_4_left" + ".csv";

        driveServiceHelper.createFile(filePath1, fileName1).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i("METAWEAR", "upload #1 success");
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("METAWEAR", "upload #1 failure");
                Toast.makeText(getApplicationContext(), "Check your google drive API key", Toast.LENGTH_LONG).show();
            }
        });
        driveServiceHelper.createFile(filePath2, fileName2).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i("METAWEAR", "upload #2 success");
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("METAWEAR", "upload #2 failure");
                Toast.makeText(getApplicationContext(), "Check your google drive API key", Toast.LENGTH_LONG).show();
            }
        });
        driveServiceHelper.createFile(filePath3, fileName3).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i("METAWEAR", "upload #3 success");
                SessionActivity.lav.setAnimation("5323-uploading-completed.json");
                SessionActivity.lav.playAnimation();
                SessionActivity.lav.setRepeatCount(1);
                textViewMessage.setText("See you next time!");
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("METAWEAR", "upload #3 failure");
                Toast.makeText(getApplicationContext(), "Check your google drive API key", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CHANNEL_ID = "ActivityNotifications";
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

