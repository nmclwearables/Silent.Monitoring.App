package com.jakehorder.activityapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.se.omapi.Session;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.airbnb.lottie.utils.Utils;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;

import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    //==============================================================================================
    // Variables
    //==============================================================================================
        private BtleService.LocalBinder serviceBinder;
        private static final String TAG = "MetaWear";

        //private final String MW_MAC_ADDRESS_1 = "FE:7E:FE:40:B6:6A";    // phone 1 left device
        //private final String MW_MAC_ADDRESS_2 = "C5:10:B9:F0:DD:C7";    // phone 1 right device

        //private final String MW_MAC_ADDRESS_1 = "F0:DA:4E:95:78:E7";    // phone 3 left device
        //private final String MW_MAC_ADDRESS_2 = "F0:70:E4:18:EE:46";    // phone 3 right device

        //private final String MW_MAC_ADDRESS_1 = "D9:50:FE:38:FB:CB";    // phone 4 left device
        //private final String MW_MAC_ADDRESS_2 = "F3:59:D6:D4:B5:52";    // phone 4 right device

        private final String MW_MAC_ADDRESS_1 = "DF:A7:2F:BD:75:1D";    // phone 5 left device
        private final String MW_MAC_ADDRESS_2 = "EA:1F:F9:8D:66:4C";    // phone 5 right device

        public static MetaWearBoard board1;
        public static MetaWearBoard board2;

        public static Led led_left;
        public static Led led_right;

        public static AccelerometerBmi160 acc1;
        public static AccelerometerBmi160 acc2;

        public static Logging log1;
        public static Logging log2;

        public static String filename;
        public static String filename2;
        public static String filename_cues;
        public static File path;
        public static File path2;
        public static File path_cues;

        public static float acc1data;
        public static float acc2data;

        public int s1hour;
        public int s1min;
        public int s2hour;
        public int s2min;
        public int s3hour;
        public int s3min;

        public Handler mHandler;

        private Animation anim;

        final Calendar c = Calendar.getInstance();
        public int current_hour = c.get(Calendar.HOUR_OF_DAY);
        public int current_minute = c.get(Calendar.MINUTE);
        public int current_month = c.get(Calendar.MONTH);
        public int current_day = c.get(Calendar.DAY_OF_MONTH);

        public boolean isLeftConnected = false;
        public boolean isRightConnected = false;

    //==============================================================================================
    // standard activity methods
    //==============================================================================================
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            getApplicationContext().bindService(new Intent(this, BtleService.class),
                    this, Context.BIND_AUTO_CREATE);

            Log.i(TAG, "App created");

            ImageView leftStatus = findViewById(R.id.leftStatusIcon);
            ImageView rightStatus = findViewById(R.id.rightStatusIcon);

            ImageView leftBattery = findViewById(R.id.imageLeftBattery);
            ImageView rightBattery = findViewById(R.id.imageRightBattery);

            TextView leftBatPercent = findViewById(R.id.leftBatteryText2);
            TextView rightBatPercent = findViewById(R.id.rightBatteryText2);

            TextView leftConnecting = findViewById(R.id.textLeftConnecting);
            TextView rightConnecting = findViewById(R.id.textRightConnecting);

            leftStatus.setImageResource(R.drawable.ic_baseline_close_24);
            rightStatus.setImageResource(R.drawable.ic_baseline_close_24);

            // Begin 'connecting...' animation
                makeMeBlink(leftConnecting);
                makeMeBlink(rightConnecting);

            // intialize default times
                s1hour = 9;
                s1min = 30;
                s2hour = 12;
                s2min = 30;
                s3hour = 16;
                s3min = 0;

            // handler for updating watch statuses
            mHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1: // left device connected
                            leftConnecting.setText("Connected!");
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    leftConnecting.setVisibility(View.INVISIBLE);
                                }
                            }, 4000);
                            leftStatus.setImageResource(R.drawable.ic_baseline_wifi_24);
                            leftBattery.setVisibility(View.VISIBLE);
                            leftBatPercent.setVisibility(View.VISIBLE);
                            stopBlink(leftConnecting);
                            updateBatteryLeft();
                            isLeftConnected = true;
                            break;
                        case 2: // right device connected
                            rightConnecting.setText("Connected!");
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    rightConnecting.setVisibility(View.INVISIBLE);
                                }
                            }, 4000);
                            rightStatus.setImageResource(R.drawable.ic_baseline_wifi_24);
                            rightBattery.setVisibility(View.VISIBLE);
                            rightBatPercent.setVisibility(View.VISIBLE);
                            stopBlink(rightConnecting);
                            updateBatteryRight();
                            isRightConnected = true;
                            break;
                    }
                }
            };
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // disconnect from boards
            disconnectBoard(board1, 1);
            disconnectBoard(board2, 2);

            // Unbind the service when the activity is destroyed
            getApplicationContext().unbindService(this);
        }

    //==============================================================================================
    // ServiceConnection Methods
    //==============================================================================================
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Typecast the binder to the service's LocalBinder class
            serviceBinder = (BtleService.LocalBinder) service;

            retrieveBoard1();
            retrieveBoard2();

            // Clear any previous routes on boards
                board1.tearDown();
                board2.tearDown();

            // connect to boards
                board1.connectAsync().continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        if (task.isFaulted()) {
                            Log.i(TAG, "Failed to connect to board 1 on service");
                            attemptConnect1();
                        } else {
                            Log.i(TAG, "Connected to board 1 on service");
                            mHandler.sendEmptyMessage(1);
                        }
                        return null;
                    }
                });
                board2.connectAsync().continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        if (task.isFaulted()) {
                            Log.i(TAG, "Failed to connect to board 2 on service");
                            attemptConnect2();
                        } else {
                            Log.i(TAG, "Connected to board 2 on service");
                            mHandler.sendEmptyMessage(2);
                        }
                        return null;
                    }
                });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }

    //==============================================================================================
    // Retrieves bluetooth boards and binds services
    //==============================================================================================
        public void retrieveBoard1() {
            final BluetoothManager btManager1 = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothDevice remoteDevice1 = btManager1.getAdapter().getRemoteDevice(MW_MAC_ADDRESS_1);
            board1 = serviceBinder.getMetaWearBoard(remoteDevice1);
        }
        public void retrieveBoard2() {
            final BluetoothManager btManager2 = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothDevice remoteDevice2 = btManager2.getAdapter().getRemoteDevice(MW_MAC_ADDRESS_2);
            board2 = serviceBinder.getMetaWearBoard(remoteDevice2);
        }

    //==============================================================================================
    // "Confirm Settings" Button OnClick Method
    // Ensures time are properly set with no errors
    // If times are ok, displays "ready" button
    //==============================================================================================
        public void confirmSettings(View view) {

            if ((!isRightConnected) || (!isLeftConnected))
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Check Connections");
                alertDialog.setMessage("Please ensure both devices are connected before continuing. If devices are unable to connect, try restarting the app.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            if ((s1hour < current_hour))
                    //|| (s1hour == current_hour && s1min < current_minute + 30) ||
                    //(s1hour == current_hour + 1 && s1min < current_minute - 30))
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 1");
                alertDialog.setMessage("Please select a starting time for Session #1 at least 30 minutes after the current time.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            else if ((s2hour == s1hour) ||
                    (s2hour == s1hour + 1 && s2min < s1min))
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 2");
                alertDialog.setMessage("Please select a starting time at least 1 hour after the start of Session #1.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            else if (s2hour < s1hour)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 2");
                alertDialog.setMessage("Please select a time after Session #1.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            else if ((s3hour == s2hour) ||
                    (s3hour == s2hour + 1 && s3min < s2min))
            {
                Calendar bad = Calendar.getInstance();
                bad.set(Calendar.HOUR_OF_DAY, current_hour+7);
                bad.set(Calendar.MINUTE, current_minute);

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 3");
                alertDialog.setMessage("Please select a starting time at least 1 hour after the start of Session #2.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            else if (s3hour < s2hour)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 3");
                alertDialog.setMessage("Please select a start time after Session #2");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            else if ((s3hour > current_hour + 7) ||
                    (s3hour == current_hour + 7 && s3min > current_minute))
            {
                Calendar bad = Calendar.getInstance();
                bad.set(Calendar.HOUR_OF_DAY, current_hour+7);
                bad.set(Calendar.MINUTE, current_minute);

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Error - Session 3");
                alertDialog.setMessage("To ensure 8 hours of data collection, please select a time before" + " "+ java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(bad.getTime()));
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
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Please Confirm");
                alertDialog.setMessage("You are about to begin data collection. If the devices are fitted and the session times are correct, please continue!");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Continue",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                try {
                                    confirmReady();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }

    //==============================================================================================
    //
    // "Confirm Ready" Button OnClick Method
    // Begins data logging to csv file
    //
    //==============================================================================================
        public void confirmReady() throws IOException {
                GlobalClass globalClass = (GlobalClass) getApplicationContext();

                acc1 = board1.getModule(AccelerometerBmi160.class);
                acc2 = board2.getModule(AccelerometerBmi160.class);

                log1 = board1.getModule(Logging.class);
                log2 = board2.getModule(Logging.class);

                try {
                    led_left = board1.getModuleOrThrow(Led.class);
                } catch (UnsupportedModuleException e) {
                    e.printStackTrace();
                }
                try {
                    led_right = board2.getModuleOrThrow(Led.class);
                } catch (UnsupportedModuleException e) {
                    e.printStackTrace();
                }

            // unexpected disconnect handlers
                board1.onUnexpectedDisconnect(new MetaWearBoard.UnexpectedDisconnectHandler() {
                    @Override
                    public void disconnected(int status) {
                        Log.i("METAWEAR", "Lost connection: " + status);
                        isLeftConnected = false;
                        // Update the UI
                            SessionActivity.UIhandler.sendEmptyMessage(1);

                        // stop data stream
                            try {
                                acc1.acceleration().stop();
                                Log.i("METAWEAR", "Stopped data collection!");
                            } catch (Exception e) {
                                Log.i("METAWEAR", "stop didnt work!");
                            }

                        // start attempting reconnect
                            attemptReconnect1();
                    }
                });
                board2.onUnexpectedDisconnect(new MetaWearBoard.UnexpectedDisconnectHandler() {
                    @Override
                    public void disconnected(int status) {
                        Log.i("METAWEAR", "Right lost connection: " + status);
                        isRightConnected = false;
                        // Update the UI
                        SessionActivity.UIhandler.sendEmptyMessage(2);

                        // stop data stream
                        try {
                            acc2.acceleration().stop();
                            Log.i("METAWEAR", "Stopped data collection!");
                        } catch (Exception e) {
                            Log.i("METAWEAR", "stop didnt work!");
                        }

                        // start attempting reconnect
                            attemptReconnect2();
                    }
                });

            // Initialize data file
                // Get current date/time for csv file name
                Calendar now = Calendar.getInstance();
                int now_hour = now.get(Calendar.HOUR_OF_DAY);
                int now_minute = now.get(Calendar.MINUTE);
                int now_month = now.get(Calendar.MONTH);
                int now_day = now.get(Calendar.DAY_OF_MONTH);

                // Initialize filename and open file
                filename = (now_month + "_" + now_day + "_" + now_hour + "_" + now_minute + "_" + "left");
                filename2 = (now_month + "_" + now_day + "_" + now_hour + "_" + now_minute + "_" + "right");
                filename_cues = (now_month + "_" + now_day + "_" + now_hour + "_" + now_minute + "_" + "cues");

                Log.i(TAG, "Created left device's file:"+filename);
                Log.i(TAG, "Created right device's file:"+filename2);
                Log.i(TAG, "Created cue file:"+filename_cues);

                path = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename + ".csv");
                path2 = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename2 + ".csv");
                path_cues = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename_cues + ".csv");

                // Write headers to file
                FileWriter writer = new FileWriter(path);
                writer.write("Time,Left_x,Left_y,Left_z");
                writer.write("\n");
                writer.flush();
                FileWriter writer2 = new FileWriter(path2);
                writer2.write("Time,Right_x,Right_y,Right_z");
                writer2.write("\n");
                writer2.flush();
                FileWriter writer3 = new FileWriter(path_cues);
                writer3.write("Time");
                writer3.write("\n");
                writer3.flush();

                acc1.configure()
                        .odr(AccelerometerBmi160.OutputDataRate.ODR_12_5_HZ)  // set odr to 12.5 Hz
                        .range(AccelerometerBosch.AccRange.AR_4G)             // set range to +/-4g
                        .commit();

                acc2.configure()
                        .odr(AccelerometerBmi160.OutputDataRate.ODR_12_5_HZ)  // set odr to 25Hz
                        .range(AccelerometerBosch.AccRange.AR_4G)             // set range to +/-4g
                        .commit();

                acc1.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                String value = data.timestamp().getTimeInMillis() + "," +
                                        data.value(Acceleration.class).x() + "," +
                                        data.value(Acceleration.class).y() + "," +
                                        data.value(Acceleration.class).z();
                                Log.i("Data 1", value);

                                OutputStream out;
                                try {
                                    out = new BufferedOutputStream(new FileOutputStream(path, true));
                                    out.write(value.getBytes());
                                    out.write("\n".getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "CSV creation error", e);
                                }
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        acc1.acceleration().start();
                        acc1.start();
                        return null;
                    }
                });

                acc2.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                String value2 = data.timestamp().getTimeInMillis() + "," +
                                        data.value(Acceleration.class).x() + "," +
                                        data.value(Acceleration.class).y() + "," +
                                        data.value(Acceleration.class).z();
                                Log.i("Data 2", value2);

                                OutputStream out2;
                                try {
                                    out2 = new BufferedOutputStream(new FileOutputStream(path2, true));
                                    out2.write(value2.getBytes());
                                    out2.write("\n".getBytes());
                                    out2.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "CSV creation error", e);
                                }
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        acc2.acceleration().start();
                        acc2.start();
                        return null;
                    }
                });


        // push to SessionActivity class
            globalClass.setAcc1(acc1);
            globalClass.setAcc2(acc2);

            Intent myIntent = new Intent(MainActivity.this, SessionActivity.class);
            myIntent.putExtra("s1hour", s1hour);
            myIntent.putExtra("s1min", s1min);
            myIntent.putExtra("s2hour", s2hour);
            myIntent.putExtra("s2min", s2min);
            myIntent.putExtra("s3hour", s3hour);
            myIntent.putExtra("s3min", s3min);

            MainActivity.this.startActivity(myIntent);
        }

    //==============================================
    // Disconnects from a board
    //==============================================
        public void disconnectBoard(MetaWearBoard board, final int boardNo){
            board.disconnectAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    Log.i(TAG, "Disconnected board #"+boardNo);

                    Context context = getApplicationContext();
                    CharSequence text = "Disconnected from board #"+boardNo;
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    return null;
                }
            });
        }

        public void showTime1Dialog(View v) {
            DialogFragment newFragment = new Session1TimeFragment();
            newFragment.show(getSupportFragmentManager(), "timePicker");
        }

        public void showTime2Dialog(View v) {
                DialogFragment newFragment = new Session2TimeFragment();
                newFragment.show(getSupportFragmentManager(), "timePicker");
        }

        public void showTime3Dialog(View v) {
            DialogFragment newFragment = new Session3TimeFragment();
            newFragment.show(getSupportFragmentManager(), "timePicker");
        }

        public void attemptConnect1() {
            board1.connectAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        Log.i(TAG, "Failed to connect to board 1");
                        attemptConnect1();
                    } else {
                        Log.i(TAG, "Connected to board 1 on reconnect");
                        mHandler.sendEmptyMessage(1);
                    }
                    return null;
                }
            });
        }
        public void attemptConnect2() {
            board2.connectAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        Log.i(TAG, "Failed to connect to board 2");
                        attemptConnect2();
                    } else {
                        Log.i(TAG, "Connected to board 2");
                        mHandler.sendEmptyMessage(2);
                    }
                    return null;
                }
            });
        }

        public void attemptReconnect1() {
            board1.connectAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        Log.i(TAG, "Failed to RECONNECT to board 1");
                        attemptReconnect1();
                    } else {
                        Log.i(TAG, "WE RECONNECTED to board 1!!");
                        SessionActivity.UIhandler.sendEmptyMessage(3);
                    }
                    return null;
                }
            });
        }

        public void attemptReconnect2() {
            board2.connectAsync().continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        Log.i(TAG, "Failed to RECONNECT to board 2");
                        attemptReconnect2();
                    } else {
                        Log.i(TAG, "WE RECONNECTED to board 2!!");
                        SessionActivity.UIhandler.sendEmptyMessage(4);
                    }
                    return null;
                }
            });
        }

        public void updateBatteryLeft() {
            TextView leftBat = findViewById(R.id.leftBatteryText2);
            ImageView leftBatImage = findViewById(R.id.imageLeftBattery);
            leftBat.setVisibility(View.VISIBLE);
            board1.readBatteryLevelAsync().continueWith(new Continuation<Byte, Object>() {
                @Override
                public Object then(Task<Byte> task) throws Exception {
                    leftBat.setText(String.format(Locale.US, "%d%%", task.getResult()));
                    Log.i("METAWEAR", "Left Battery:" + task.getResult());
                    if (task.getResult() > 60) {
                        leftBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorGreen));
                    } else if (task.getResult() < 20) {
                        leftBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorRed));
                    } else {
                        leftBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorYellow));
                    }
                    return null;

                }
            }, Task.UI_THREAD_EXECUTOR);
        }
        public void updateBatteryRight() {
            TextView rightBat = findViewById(R.id.rightBatteryText2);
            ImageView rightBatImage = findViewById(R.id.imageRightBattery);
            rightBat.setVisibility(View.VISIBLE);
            board2.readBatteryLevelAsync().continueWith(new Continuation<Byte, Object>() {
                @Override
                public Object then(Task<Byte> task) throws Exception {
                    rightBat.setText(String.format(Locale.US, "%d%%", task.getResult()));
                    Log.i("METAWEAR", "Right Battery:" + task.getResult());
                    if (task.getResult() > 60) {
                        rightBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorGreen));
                    } else if (task.getResult() < 20) {
                        rightBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorRed));
                    } else {
                        rightBatImage.setColorFilter(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.colorYellow));
                    }
                    return null;

                }
            }, Task.UI_THREAD_EXECUTOR);
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

        public void stopBlink(TextView textView)
        {
            textView.clearAnimation();
        }
}

