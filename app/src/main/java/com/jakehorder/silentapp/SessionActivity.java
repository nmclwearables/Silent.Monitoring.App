package com.jakehorder.silentapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
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

        public static Handler UIhandler;

        public String CHANNEL_ID;
        public Intent intentNotif;

        private Animation anim;

        public boolean leftConnection;
        public boolean rightConnection;

        public boolean file1Success;
        public boolean file2Success;

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
            registerReceiver(KillReceiver, new IntentFilter("KILL_APP"));

        // Initialize UI components
            textViewStatus = findViewById(R.id.textStatus);
            textViewMessage = findViewById(R.id.textMessage);
            lav = findViewById(R.id.lav_main);

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

            leftConnection = true;
            rightConnection = true;

            file1Success = false;
            file2Success = false;

        // Initialize notifications
            createNotificationChannel();
            intentNotif = new Intent(this, SessionActivity.class);

        // create calendars for session times and ending time
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            int current_hour = c.get(Calendar.HOUR_OF_DAY);
            int current_minute = c.get(Calendar.MINUTE);

            Calendar end = Calendar.getInstance(); // ending alarm 12 hours after current time
            end.set(Calendar.HOUR_OF_DAY, current_hour+12);
            end.set(Calendar.MINUTE, current_minute);
            String endText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(end.getTime());

            Calendar kill = Calendar.getInstance(); // kill app 13 hours after current time
            kill.set(Calendar.HOUR_OF_DAY, current_hour+13);
            kill.set(Calendar.MINUTE, current_minute);
            String killText = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(end.getTime());

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

        // Initialize static UI for silent monitoring
            textViewStatus.setText("Collecting Data...");

            progressCircle.setVisibility(View.INVISIBLE);
            progressCircle.setIndeterminate(true);
            progressCircle.setVisibility(View.VISIBLE);

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

                uploadFiles(filePath1, MainActivity.filename, filePath2, MainActivity.filename2);
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

    public void uploadFiles(String filePath1, String fileName1, String filePath2, String fileName2)
    {
        //String filePath = "/storage/emulated/0/mypdf.pdf";
        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + MainActivity.filename + ".csv";
        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "2_23_10_4_left" + ".csv";
        try {
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
        } catch (Exception e) {
            Log.i("METAWEAR", "Could not connect to service for upload. Check intenet connection.");
            Toast.makeText(getApplicationContext(), "Check device connection. Data did not upload successfully but is available on the device storage.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

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

