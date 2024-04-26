package com.jakehorder.silentapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.Data;
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
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    //==============================================================================================
    // Variables
    //==============================================================================================
        private BtleService.LocalBinder serviceBinder;
        private static final String TAG = "MetaWear";

        private final String MW_MAC_ADDRESS_1 = "C0:8E:E0:95:43:AA";    // left device
        private final String MW_MAC_ADDRESS_2 = "F7:5C:C2:51:B6:C9";    // right device

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
        public static File path;
        public static File path2;

        public Handler mHandler;

        private Animation anim;

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
            else
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Please Confirm");
                alertDialog.setMessage("You are about to begin data collection. If the devices are fitted, please continue!");
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
                int now_month = now.get(Calendar.MONTH) + 1;
                int now_day = now.get(Calendar.DAY_OF_MONTH);

                // Initialize filename and open file
                filename = (now_month + "_" + now_day + "_" + now_hour + "_" + now_minute + "_" + "left");
                filename2 = (now_month + "_" + now_day + "_" + now_hour + "_" + now_minute + "_" + "right");

                Log.i(TAG, "Created left device's file:"+filename);
                Log.i(TAG, "Created right device's file:"+filename2);

                path = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename + ".csv");
                path2 = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename2 + ".csv");

                // Write headers to file
                FileWriter writer = new FileWriter(path);
                writer.write("Time,Left_x,Left_y,Left_z");
                writer.write("\n");
                writer.flush();
                FileWriter writer2 = new FileWriter(path2);
                writer2.write("Time,Right_x,Right_y,Right_z");
                writer2.write("\n");
                writer2.flush();

                acc1.configure()
                        .odr(AccelerometerBmi160.OutputDataRate.ODR_50_HZ)  // set odr to 12.5 Hz
                        .range(AccelerometerBosch.AccRange.AR_4G)             // set range to +/-4g
                        .commit();

                acc2.configure()
                        .odr(AccelerometerBmi160.OutputDataRate.ODR_50_HZ)  // set odr to 25Hz
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

