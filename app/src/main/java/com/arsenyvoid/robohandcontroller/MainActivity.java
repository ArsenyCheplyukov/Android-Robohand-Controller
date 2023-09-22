package com.arsenyvoid.robohandcontroller;


import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Collections;

//import static android.Manifest.permission.BLUETOOTH;
//import static android.Manifest.permission.BLUETOOTH_ADMIN;
//import static android.Manifest.permission.BLUETOOTH_SCAN;
//import static android.Manifest.permission.BLUETOOTH_CONNECT;
//import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "bluetooth2";
    Button button1, button2, button3, button4, button5, button6, button7, button8, button9, button10;
    TextView txtArduino;
    static private Handler h;
    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;      // Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private final StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;
    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-адрес Bluetooth модуля
    private static final String address = "98:D3:21:FC:86:C5";
    private static final int PERMISSIONS_REQUEST_CODE = 11;

    String current_x = "";
    String current_y = "";
    String current_z = "";
    String current_rot = "";
    String current_grip = "";

    // on below line we are creating variables.
    private ListView lv;
    private Button addBtn;
    private Button add_actionBtn;
    private Button fill_button;
    private Button stop_button;
    private EditText enterActionName;
    private EditText enterDelay;
    private EditText enterX;
    private EditText enterY;
    private EditText enterZ;
    private EditText enterGrip;
    private EditText enterRot;
    private TextView name;

    // GET CONTEXT FROM MAIN FUNCTION, BECAUSE CAN'T FIND ANOTHER WAY TO GET IT
    private final Context context_for_adapter = this;
    // LIST CONTAIN NAMES OF ACTIONS
    private ArrayList<ActionModel> actionList = new ArrayList<>();
    // POSITION OF CURRENT ELEMENT IN DATABASE
    private int position_of_points = 0;
    // DATABASE THAT CONTAINS ANY DATA OF POINTS IN OUR LIST
    private ArrayList<ArrayList<ArrayList<Integer>>> data = new ArrayList<>();
    private MyListAdaper adapter;
    private ActionAdapter mAdapter;

    int state = 0; // 0-runnable is unactive, 1-one time, 2-loop until break
    ArrayList<ArrayList<Integer>> base_points = new ArrayList<>(); // main points to iterate
    int current_step = 0;
    // FIND TWO MAIN GROUPS OF SWITCHES: SWITCH OF ROBOTIC HAND AND SWITCH OF CONTROL MODE
    RadioGroup triangle_or_trapezoid, keys_voice_axis;

    // create state variables for radio buttons:
    int triangle_or_trapezoid_state = 0;
    int keys_voice_axis_state = 0;
    // staff for voice control:
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    // DEFINE HANDLERS (SOME ANALOG OF THREADS OR REMINDERS ON SOMETHING THAT WE SHOULD DO)
    // FOR SEND POINTS IN DATABASE, AUDIO COMMANDS, AND GET SENSORS (GYROSCOPE, MAGNETOMETER, ACCELEROMETER) DATA
    final private Handler points_moving_loop = new Handler();
    final private Handler audioCommandsHandler = new Handler();
    final private Handler sensorHandler = new Handler();
    // sensors listen delay
    final int sensorsListenDelay = 500; // ms
    // System sensor manager instance.
    private SensorManager mSensorManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;

    // Current data from accelerometer & magnetometer.  The arrays hold values
    // for X, Y, and Z.
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];

    // litened data from sensors
    float azimuth = 0;
    float pitch = 0;
    float roll = 0;

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;
    private TextView mXAxisData;
    private TextView mYAxisData;
    // System display. Need this for determining rotation.
    private Display mDisplay;

    // maximum for seek bar
    final int maximum_value = 58;
    private TextView step_info;
    int progress_value = 1;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    // create text formatter
    private static final DecimalFormat REAL_FORMATTER = new DecimalFormat("0.##");

    /**
     * Called when the activity is first created.
     */
    @SuppressLint({"ClickableViewAccessibility", "HandlerLeak"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // call parent method
        super.onCreate(savedInstanceState);
        //CODE TO DISABLE ACTION BAR
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        // apply activity main image
        setContentView(R.layout.activity_main);
        // ask for all the permissions
        //ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);

        // create button array
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);
        button6 = (Button) findViewById(R.id.button6);
        button7 = (Button) findViewById(R.id.button7);
        button8 = (Button) findViewById(R.id.button8);
        button9 = (Button) findViewById(R.id.button9);
        button10 = (Button) findViewById(R.id.button10);

        // get seek bar result:
        SeekBar simpleSeekBar = (SeekBar) findViewById(R.id.seekBar); // initiate the Seek bar
        simpleSeekBar.setMax(500);
        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progress_value = (int) ((maximum_value / Math.exp(-1.0)) * (Math.exp(progressChangedValue / 500.0 - 1.0) -
                        Math.exp(-1.0)) + 1);
                step_info = (TextView) findViewById(R.id.stepText);
                step_info.setText(String.valueOf((int) (progress_value)));
            }
        });

        h = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    // IF GET MESSAGE IN Handler
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        // FORM STRING
                        sb.append(strIncom);
                        // FIND AND-LINE SYMBOLS
                        int endOfLineIndex = sb.indexOf("\r\n");
                        // IF GET SUCH, CRAWL IT
                        if (endOfLineIndex > 0) {
                            if (endOfLineIndex > 10) {
                                @SuppressLint("HandlerLeak") String sbprint = sb.substring(0, endOfLineIndex);
                                String[] words = sbprint.split("\\:");
                                if (words.length == 5) {
                                    current_x = words[0];
                                    current_y = words[1];
                                    current_z = words[2];
                                    current_grip = words[3];
                                    current_rot = words[4];
                                }
                            }
                            // CLEAR sb
                            sb.delete(0, sb.length());
                        }
                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                        break;
                }
            }
        };
        // make bluetooth data handler
        btAdapter = BluetoothAdapter.getDefaultAdapter();    // получаем локальный Bluetooth адаптер
        checkBTState();
        // MINIMUM DELAY BETWEEN PACKAGES ON CLICK TO BUTTON
        final int delay_time = 150;
        // MAKE LISTENER TO CLICK TO BUTTON
        button1.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            // IF CLICKED WHEN SAVED ACTION MODE ON, WORKS AS STOP TRIGGER
            public boolean onTouch(View v, MotionEvent event) {
                // IF CLICK BUTTON - MAKE HANDLER TO SEND MESSAGES
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // IF BUTTON FREE - STOP HANDLER
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("1%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 1 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button2.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("2%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 2 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button3.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("3%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 3 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button4.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("4%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 4 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button5.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("5%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 5 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button6.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("6%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 6 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button7.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("7%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 7 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button8.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, delay_time);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("8%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 8 via bluetooth
                    }
                    buttonHandler.postDelayed(this, delay_time);
                }
            };
        });

        button9.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, 500);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("9%03d", progress_value);
                        mConnectedThread.write(padded);   // Send 9 via bluetooth
                    }
                    buttonHandler.postDelayed(this, 500);
                }
            };
        });

        button10.setOnTouchListener(new View.OnTouchListener() {
            private Handler buttonHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (buttonHandler != null) return true;
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(mAction, 500);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (buttonHandler == null) return true;
                    buttonHandler.removeCallbacks(mAction);
                    buttonHandler = null;
                }
                return false;
            }      // DEFINE HANDLER TO BUTTON CLICK

            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    state = 0;
                    if (keys_voice_axis_state == 0) {
                        String padded = String.format("a%03d", progress_value);
                        mConnectedThread.write(padded);   // Send a via bluetooth
                    }
                    buttonHandler.postDelayed(this, 500);
                }
            };
        });

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        // set voice recognition staff
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        triangle_or_trapezoid = findViewById(R.id.type);
        keys_voice_axis = findViewById(R.id.control);
        // GET SWITCH DATA
        triangle_or_trapezoid.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.triangle:
                        points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        state = 0;
                        triangle_or_trapezoid_state = 0;
                        mConnectedThread.write("b000");
                        break;
                    case R.id.trapezoid:
                        points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        state = 0;
                        triangle_or_trapezoid_state = 1;
                        mConnectedThread.write("c000");
                        break;
                    default:
                        triangle_or_trapezoid_state = -1;
                        break;
                }
            }
        });

        //create staff for get x and y coordinates
        ImageView imgFavorite = findViewById(R.id.viewImage);
        imgFavorite.setClickable(true);
        imgFavorite.setEnabled(true);
        imgFavorite.setFocusable(true);
        imgFavorite.setFocusableInTouchMode(true);
        imgFavorite.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                points_moving_loop.removeCallbacks(LoopPointsRunnable);
                state = 0;
                if (event != null) {
                    if (MotionEvent.ACTION_UP == event.getAction()) {
                        view.performClick();
                        float[] location = new float[2];
                        location[0] = event.getX();
                        location[1] = event.getY();
                        float height = imgFavorite.getHeight();
                        float width = imgFavorite.getWidth();
                        location[0] /= width;
                        location[1] /= height;
                        if (Math.abs(location[0]) >= VALUE_DRIFT) {
                            mXAxisData.setText(REAL_FORMATTER.format(location[0]));
                        }
                        if (Math.abs(location[1]) >= VALUE_DRIFT) {
                            mYAxisData.setText(REAL_FORMATTER.format(location[1]));
                        }
                        // x controls rotation
                        if (keys_voice_axis_state == 2) {
                            if (location[0] >= 0.6) {
                                String padded = String.format("8%03d", progress_value);
                                mConnectedThread.write(padded);   // Send 8 via bluetooth
                            } else if (location[0] <= 0.4) {
                                String padded = String.format("7%03d", progress_value);
                                mConnectedThread.write(padded);   // Send 7 via bluetooth
                            }
                            if (location[1] >= 0.6) {
                                String padded = String.format("9%03d", progress_value);
                                mConnectedThread.write(padded);   // Send 9 via bluetooth
                            } else if (location[1] <= 0.4) {
                                String padded = String.format("a%03d", progress_value);
                                mConnectedThread.write(padded);   // Send a via bluetooth
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        // on below line we are initializing our variables.
        lv = (ListView) findViewById(R.id.listview);
        add_actionBtn = findViewById(R.id.add_action);
        enterActionName = findViewById(R.id.Enter_action_name);
        addBtn = findViewById(R.id.idBtnAdd);
        enterDelay = findViewById(R.id.Enter_delay);
        enterX = findViewById(R.id.Enter_X);
        enterY = findViewById(R.id.Enter_Y);
        enterZ = findViewById(R.id.Enter_Z);
        enterGrip = findViewById(R.id.Enter_grip_angle);
        enterRot = findViewById(R.id.Enter_rot_angle);
        fill_button = findViewById(R.id.idBtnFill);
        stop_button = findViewById(R.id.stop_action);

        // on below line we are adding items to our list
        readFromFile();
        // on the below line we are initializing the adapter for our list view.
        adapter = new MyListAdaper(this, R.layout.customlayout, data.get(position_of_points));
        lv.setAdapter(adapter);

        // on below line we are setting adapter for our list view.
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mAdapter = new ActionAdapter(actionList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        // on below line we are adding click listener for our button.
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                points_moving_loop.removeCallbacks(LoopPointsRunnable);
                state = 0;
                // on below line we are getting text from edit text
                String staff_x = enterX.getText().toString();
                String staff_y = enterY.getText().toString();
                String staff_z = enterZ.getText().toString();
                String staff_rot = enterRot.getText().toString();
                String staff_grip = enterGrip.getText().toString();
                String staff_delay = enterDelay.getText().toString();
                int x = 0, y = 0, z = 0, delay = 0, rot = 0, grip = 0;
                if ((!staff_x.isEmpty()) && (!staff_y.isEmpty()) && (!staff_z.isEmpty()) &&
                        (!staff_delay.isEmpty()) && (!staff_rot.isEmpty()) && (!staff_grip.isEmpty())) {
                    x = Integer.parseInt(staff_x);
                    y = Integer.parseInt(staff_y);
                    z = Integer.parseInt(staff_z);
                    delay = Integer.parseInt(staff_delay);
                    rot = Integer.parseInt(staff_rot);
                    grip = Integer.parseInt(staff_grip);
                }

                // on below line we are checking if item is not empty
                ArrayList<Integer> staff = new ArrayList<Integer>();
                staff.add(x);
                staff.add(y);
                staff.add(z);
                staff.add(delay);
                staff.add(rot);
                staff.add(grip);
                data.get(position_of_points).add(staff);
                // on below line we are notifying adapter
                // that data in list is updated to
                // update our list view.
                adapter.notifyDataSetChanged();
            }
        });

        add_actionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                points_moving_loop.removeCallbacks(LoopPointsRunnable);
                state = 0;
                // on below line we are getting text from edit text
                String item = enterActionName.getText().toString();
                if (!item.isEmpty()) {
                    // on below line we are checking if item is not empty
                    ActionModel model0 = new ActionModel(item, 100);
                    position_of_points = actionList.size();
                    actionList.add(model0);
                    ArrayList<Integer> staff1 = new ArrayList<Integer>();
                    ArrayList<ArrayList<Integer>> staff = new ArrayList<>();
                    staff1.add(Integer.valueOf(current_x));
                    staff1.add(Integer.valueOf(current_y));
                    staff1.add(Integer.valueOf(current_z));
                    staff1.add(100);
                    staff1.add(Integer.valueOf(current_rot));
                    staff1.add(Integer.valueOf(current_grip));
                    data.add(staff);
                    // on below line we are notifying adapter
                    // that data in list is updated to
                    // update our list view.
                    adapter.notifyDataSetChanged();
                    mAdapter.notifyItemInserted(position_of_points);
                }
            }
        });

        fill_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                points_moving_loop.removeCallbacks(LoopPointsRunnable);
                state = 0;
                enterX.setText(current_x);
                enterY.setText(current_y);
                enterZ.setText(current_z);
                enterGrip.setText(current_grip);
                enterRot.setText(current_rot);
            }
        });


        // make voice recognizer:
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                //voice_info.setText("Start recognizing...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                //voice_info.setText("Stop recognizing...");
            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                String padded = "";
                if (matches != null) {
                    string = matches.get(0);
                    //voice_info.setText(string);
                    switch (string) {
                        case "forward":
                        case "вперёд":
                        case "перед":
                        case "вперед":
                            padded = String.format("1%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 1 via bluetooth
                            break;
                        case "left":
                        case "налево":
                        case "лево":
                            padded = String.format("2%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 2 via bluetooth
                            break;
                        case "backward":
                        case "назад":
                        case "зад":
                            padded = String.format("3%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 3 via bluetooth
                            break;
                        case "right":
                        case "направо":
                        case "право":
                            padded = String.format("4%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 4 via bluetooth
                            break;
                        case "upward":
                        case "вверх":
                        case "наверх":
                            padded = String.format("5%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 5 via bluetooth
                            break;
                        case "down":
                        case "вниз":
                        case "низ":
                            padded = String.format("6%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 6 via bluetooth
                            break;
                        case "decreasing":
                        case "уменьши":
                        case "уменьшение":
                        case "уменьшай":
                        case "сжимай":
                            padded = String.format("8%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 8 via bluetooth
                            break;
                        case "increasing":
                        case "увеличивай":
                        case "увеличение":
                        case "разжимай":
                            padded = String.format("7%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 7 via bluetooth
                            break;
                        case "anticlockwise":
                        case "против часовой":
                        case "против часовой стрелки":
                        case "против":
                            padded = String.format("9%03d", progress_value);
                            mConnectedThread.write(padded);   // Send 9 via bluetooth
                            break;
                        case "clockwise":
                        case "почасовой":
                        case "по часовой":
                        case "по":
                        case "по часовой стрелке":
                            padded = String.format("a%03d", progress_value);
                            mConnectedThread.write(padded);   // Send a via bluetooth
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        keys_voice_axis.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.keys:
                        audioCommandsHandler.removeCallbacks(audioCommandsRunnable);
                        sensorHandler.removeCallbacks(audioCommandsRunnable);
                        speechRecognizer.stopListening();
                        state = 0;
                        keys_voice_axis_state = 0;
                        mConnectedThread.write("d000");
                        break;
                    case R.id.voice:
                        points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        sensorHandler.removeCallbacks(audioCommandsRunnable);
                        audioCommandsRunnable.run();
                        state = 0;
                        keys_voice_axis_state = 1;
                        mConnectedThread.write("e000");
                        break;
                    case R.id.axis:
                        points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        audioCommandsHandler.removeCallbacks(audioCommandsRunnable);
                        speechRecognizer.stopListening();
                        state = 0;
                        sensorsRunnable.run();
                        keys_voice_axis_state = 2;
                        mConnectedThread.write("f000");
                        break;
                    default:
                        keys_voice_axis_state = -1;
                        break;
                }
            }
        });

        // Get the display from the window manager (for rotation).
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
    }

    private void fillModel() {
        ActionModel action0 = new ActionModel("Hello", 20);
        actionList.add(action0);
        //
        ArrayList<ArrayList<Integer>> inner1 = new ArrayList<>();
        //
        ArrayList<Integer> inner11 = new ArrayList<Integer>();
        ArrayList<Integer> inner12 = new ArrayList<Integer>();
        ArrayList<Integer> inner13 = new ArrayList<Integer>();
        ArrayList<Integer> inner14 = new ArrayList<Integer>();
        //
        ArrayList<Integer> inner15 = new ArrayList<Integer>();
        ArrayList<Integer> inner16 = new ArrayList<Integer>();
        ArrayList<Integer> inner17 = new ArrayList<Integer>();
        ArrayList<Integer> inner18 = new ArrayList<Integer>();
        ArrayList<Integer> inner19 = new ArrayList<Integer>();
        ArrayList<Integer> inner110 = new ArrayList<Integer>();
        ArrayList<Integer> inner111 = new ArrayList<Integer>();
        ArrayList<Integer> inner112 = new ArrayList<Integer>();
        //
        ArrayList<Integer> inner113 = new ArrayList<Integer>();
        ArrayList<Integer> inner114 = new ArrayList<Integer>();
        ArrayList<Integer> inner115 = new ArrayList<Integer>();
        ArrayList<Integer> inner116 = new ArrayList<Integer>();
        //
        ArrayList<Integer> inner117 = new ArrayList<Integer>();
        ArrayList<Integer> inner118 = new ArrayList<Integer>();
        ArrayList<Integer> inner119 = new ArrayList<Integer>();
        ArrayList<Integer> inner120 = new ArrayList<Integer>();
        ArrayList<Integer> inner121 = new ArrayList<Integer>();
        ArrayList<Integer> inner122 = new ArrayList<Integer>();
        ArrayList<Integer> inner123 = new ArrayList<Integer>();
        ArrayList<Integer> inner124 = new ArrayList<Integer>();
        ArrayList<Integer> inner125 = new ArrayList<Integer>();
        ArrayList<Integer> inner126 = new ArrayList<Integer>();
        ArrayList<Integer> inner127 = new ArrayList<Integer>();
        ArrayList<Integer> inner128 = new ArrayList<Integer>();
        ArrayList<Integer> inner129 = new ArrayList<Integer>();
        ArrayList<Integer> inner130 = new ArrayList<Integer>();
        //
        ArrayList<Integer> inner131 = new ArrayList<Integer>();
        ArrayList<Integer> inner132 = new ArrayList<Integer>();
        ArrayList<Integer> inner133 = new ArrayList<Integer>();
        ArrayList<Integer> inner134 = new ArrayList<Integer>();
        ArrayList<Integer> inner135 = new ArrayList<Integer>();
        ArrayList<Integer> inner136 = new ArrayList<Integer>();
        //
        ArrayList<Integer> inner137 = new ArrayList<Integer>();
        ArrayList<Integer> inner138 = new ArrayList<Integer>();
        ArrayList<Integer> inner139 = new ArrayList<Integer>();
        ArrayList<Integer> inner140 = new ArrayList<Integer>();
        //
        inner11.add(30);
        inner11.add(150);
        inner11.add(100);
        inner11.add(500);
        inner11.add(2);
        inner11.add(0);
        inner1.add(inner11);
        inner12.add(30);
        inner12.add(150);
        inner12.add(200);
        inner12.add(500);
        inner12.add(2);
        inner12.add(0);
        inner1.add(inner12);
        inner13.add(-30);
        inner13.add(150);
        inner13.add(200);
        inner13.add(500);
        inner13.add(2);
        inner13.add(0);
        inner1.add(inner13);
        inner14.add(-30);
        inner14.add(150);
        inner14.add(100);
        inner14.add(1000);
        inner14.add(2);
        inner14.add(0);
        inner1.add(inner14);
        //
        inner15.add(30);
        inner15.add(150);
        inner15.add(100);
        inner15.add(680);
        inner15.add(2);
        inner15.add(0);
        inner1.add(inner15);
        inner16.add(30);
        inner16.add(150);
        inner16.add(200);
        inner16.add(200);
        inner16.add(2);
        inner16.add(0);
        inner1.add(inner16);
        inner17.add(0);
        inner17.add(150);
        inner17.add(200);
        inner17.add(200);
        inner17.add(2);
        inner17.add(0);
        inner1.add(inner17);
        inner18.add(-10);
        inner18.add(150);
        inner18.add(190);
        inner18.add(200);
        inner18.add(2);
        inner18.add(0);
        inner1.add(inner18);
        inner19.add(-15);
        inner19.add(150);
        inner19.add(180);
        inner19.add(200);
        inner19.add(2);
        inner19.add(0);
        inner1.add(inner19);
        inner110.add(-10);
        inner110.add(150);
        inner110.add(170);
        inner110.add(200);
        inner110.add(2);
        inner110.add(0);
        inner1.add(inner110);
        inner111.add(0);
        inner111.add(150);
        inner111.add(160);
        inner111.add(300);
        inner111.add(2);
        inner111.add(0);
        inner1.add(inner111);
        inner112.add(30);
        inner112.add(150);
        inner112.add(150);
        inner112.add(1000);
        inner112.add(2);
        inner112.add(0);
        inner1.add(inner112);
        //
        inner113.add(20);
        inner113.add(150);
        inner113.add(200);
        inner113.add(450);
        inner113.add(2);
        inner113.add(0);
        inner1.add(inner113);
        inner114.add(20);
        inner114.add(150);
        inner114.add(100);
        inner114.add(600);
        inner114.add(2);
        inner114.add(0);
        inner1.add(inner114);
        inner115.add(-20);
        inner115.add(150);
        inner115.add(200);
        inner115.add(450);
        inner115.add(2);
        inner115.add(0);
        inner1.add(inner115);
        inner116.add(-20);
        inner116.add(150);
        inner116.add(100);
        inner116.add(1000);
        inner116.add(2);
        inner116.add(0);
        inner1.add(inner116);
        //
        inner117.add(30);
        inner117.add(150);
        inner117.add(100);
        inner117.add(500);
        inner117.add(2);
        inner117.add(0);
        inner1.add(inner117);
        inner118.add(30);
        inner118.add(150);
        inner118.add(200);
        inner118.add(250);
        inner118.add(2);
        inner118.add(0);
        inner1.add(inner118);
        inner119.add(0);
        inner119.add(150);
        inner119.add(200);
        inner119.add(250);
        inner119.add(2);
        inner119.add(0);
        inner1.add(inner119);
        inner120.add(-10);
        inner120.add(150);
        inner120.add(200);
        inner120.add(250);
        inner120.add(2);
        inner120.add(0);
        inner1.add(inner120);
        inner121.add(-10);
        inner121.add(150);
        inner121.add(190);
        inner121.add(250);
        inner121.add(2);
        inner121.add(0);
        inner1.add(inner121);
        inner122.add(-10);
        inner122.add(150);
        inner122.add(180);
        inner122.add(250);
        inner122.add(2);
        inner122.add(0);
        inner1.add(inner122);
        inner123.add(0);
        inner123.add(150);
        inner123.add(160);
        inner123.add(300);
        inner123.add(2);
        inner123.add(0);
        inner1.add(inner123);
        inner124.add(30);
        inner124.add(150);
        inner124.add(160);
        inner124.add(300);
        inner124.add(2);
        inner124.add(0);
        inner1.add(inner124);
        inner125.add(0);
        inner125.add(150);
        inner125.add(160);
        inner125.add(250);
        inner125.add(2);
        inner125.add(0);
        inner1.add(inner125);
        inner126.add(-15);
        inner126.add(150);
        inner126.add(160);
        inner126.add(250);
        inner126.add(2);
        inner126.add(0);
        inner1.add(inner126);
        inner127.add(-20);
        inner127.add(150);
        inner127.add(150);
        inner127.add(300);
        inner127.add(2);
        inner127.add(0);
        inner1.add(inner127);
        inner128.add(-20);
        inner128.add(150);
        inner128.add(110);
        inner128.add(250);
        inner128.add(2);
        inner128.add(0);
        inner1.add(inner128);
        inner129.add(-10);
        inner129.add(150);
        inner129.add(100);
        inner129.add(250);
        inner129.add(2);
        inner129.add(0);
        inner1.add(inner129);
        inner130.add(30);
        inner130.add(150);
        inner130.add(100);
        inner130.add(1000);
        inner130.add(2);
        inner130.add(0);
        inner1.add(inner130);
        //
        inner131.add(-20);
        inner131.add(150);
        inner131.add(100);
        inner131.add(250);
        inner131.add(2);
        inner131.add(0);
        inner1.add(inner131);
        inner132.add(20);
        inner132.add(150);
        inner132.add(100);
        inner132.add(480);
        inner132.add(2);
        inner132.add(0);
        inner1.add(inner132);
        inner133.add(20);
        inner133.add(150);
        inner133.add(200);
        inner133.add(250);
        inner133.add(2);
        inner133.add(0);
        inner1.add(inner133);
        inner134.add(-20);
        inner134.add(150);
        inner134.add(200);
        inner134.add(435);
        inner134.add(2);
        inner134.add(0);
        inner1.add(inner134);
        inner135.add(20);
        inner135.add(150);
        inner135.add(150);
        inner135.add(250);
        inner135.add(2);
        inner135.add(0);
        inner1.add(inner135);
        inner136.add(-20);
        inner136.add(150);
        inner136.add(150);
        inner136.add(1000);
        inner136.add(2);
        inner136.add(0);
        inner1.add(inner136);
        //
        inner137.add(20);
        inner137.add(150);
        inner137.add(200);
        inner137.add(425);
        inner137.add(2);
        inner137.add(0);
        inner1.add(inner137);
        inner138.add(-20);
        inner138.add(150);
        inner138.add(200);
        inner138.add(250);
        inner138.add(2);
        inner138.add(0);
        inner1.add(inner138);
        inner139.add(0);
        inner139.add(150);
        inner139.add(200);
        inner139.add(938);
        inner139.add(2);
        inner139.add(0);
        inner1.add(inner139);
        inner140.add(0);
        inner140.add(150);
        inner140.add(100);
        inner140.add(1000);
        inner140.add(2);
        inner140.add(0);
        inner1.add(inner140);
        data.add(inner1);


        ActionModel action1 = new ActionModel("StartPosition", 20);
        actionList.add(action1);
        //
        ArrayList<ArrayList<Integer>> inner2 = new ArrayList<>();
        //
        ArrayList<Integer> inner21 = new ArrayList<Integer>();
        ArrayList<Integer> inner22 = new ArrayList<Integer>();
        ArrayList<Integer> inner23 = new ArrayList<Integer>();
        ArrayList<Integer> inner24 = new ArrayList<Integer>();
        ArrayList<Integer> inner25 = new ArrayList<Integer>();
        //
        inner21.add(60);
        inner21.add(150);
        inner21.add(150);
        inner21.add(750);
        inner21.add(2);
        inner21.add(0);
        inner2.add(inner21);
        inner22.add(60);
        inner22.add(140);
        inner22.add(100);
        inner22.add(500);
        inner22.add(2);
        inner22.add(0);
        inner2.add(inner22);
        inner23.add(60);
        inner23.add(130);
        inner23.add(50);
        inner23.add(500);
        inner23.add(2);
        inner23.add(0);
        inner2.add(inner23);
        inner24.add(60);
        inner24.add(120);
        inner24.add(0);
        inner24.add(500);
        inner24.add(2);
        inner24.add(0);
        inner2.add(inner24);
        inner25.add(60);
        inner25.add(110);
        inner25.add(-50);
        inner25.add(1000);
        inner25.add(2);
        inner25.add(0);
        inner2.add(inner25);
        data.add(inner2);
    }


    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...onResume - try to connect...");
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();
        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection created and ready to send data...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }
        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Creating socket...");
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");
        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth is unsupported");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth disabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256]; // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);       // Получаем кол-во байт и само собщение в байтовый массив "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();   // Отправляем в очередь сообщений Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Sending data error: " + e.getMessage() + "...");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void writeToFile() {
        try {
            File filename;
            String path = Environment.getExternalStorageDirectory().toString();

            new File(path + "/Android/data/com.arsenyvoid.robohandcontroller/files").mkdirs();
            filename = new File(path + "/Android/data/com.arsenyvoid.robohandcontroller/files/mysdfile.txt");
            FileOutputStream fOut = new FileOutputStream(filename);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
            int array_size = actionList.size();
            for (int i = 0; i < array_size; ++i) {
                outputStreamWriter.write(actionList.get(i).getName() + "\n");
                ArrayList<ArrayList<Integer>> staff_data_need_to_format = data.get(i);
                String pocketed_data = "";
                int formatted_data_size = staff_data_need_to_format.size();
                for (int j = 0; j < formatted_data_size; ++j) {
                    ArrayList<String> string_transformed_array = new ArrayList<>();
                    int element_of_data_size = (staff_data_need_to_format.get(j)).size();
                    for (int k = 0; k < element_of_data_size; ++k) {
                        string_transformed_array.add((staff_data_need_to_format.get(j).get(k)).toString());
                    }
                    String part_formatted_data = String.join(" ", string_transformed_array);
                    pocketed_data += part_formatted_data + "; ";
                }
                outputStreamWriter.write(pocketed_data + "\n");
            }
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e);
        }
    }

    private void readFromFile() {
        try {
            File filename;
            String path = Environment.getExternalStorageDirectory().toString();
            new File(path + "/Android/data/com.arsenyvoid.robohandcontroller/files").mkdirs();
            filename = new File(path + "/Android/data/com.arsenyvoid.robohandcontroller/files/mysdfile.txt");
            FileInputStream fIn = new FileInputStream(filename);

            BufferedReader myReader = new BufferedReader(
                    new InputStreamReader(fIn));
            String aDataRow = "";
            int counter = 0;
            // CREATE NEW DATA CONTAINERS WHERE WE SAVE ALL DATA THAT WE PARSE
            ArrayList<ActionModel> actionList_new = new ArrayList<>();
            ArrayList<ArrayList<ArrayList<Integer>>> data_new = new ArrayList<>();
            while ((aDataRow = myReader.readLine()) != null) {
                counter++;
                if ((counter % 2) == 1) {
                    ActionModel new_action_model = new ActionModel(aDataRow, 0);
                    actionList_new.add(new_action_model);
                } else {
                    // SPLIT GIVEN DATA IN ROW INTO GROUPS THAT CONTAINS ONLY POINT COORDINATES, DELAY, ANGLES FOR ONLY ONE POINT
                    ArrayList<String> points_list = new ArrayList<>(Arrays.asList(aDataRow.split(";")));
                    // OUR NEW ARRAY THAT WE FILL WITH FILTERED DATA CONTAINS POINTS DATA
                    ArrayList<ArrayList<Integer>> points_filtered_list = new ArrayList<>();
                    int size_of_points_list = points_list.size();
                    for (int i = 0; i < size_of_points_list; ++i) {
                        // ARRAY THAT WE PARSE TO GET DATA
                        ArrayList<String> coordinate_of_point_list = new ArrayList<>(Arrays.asList(
                                (points_list.get(i)).split(" ")));
                        // ARRAY THAT WE FILL WITH FILTERED DATA
                        ArrayList<Integer> array_of_numbers = new ArrayList<>();
                        int size_of_coordinate_of_point_list = coordinate_of_point_list.size();
                        if (size_of_points_list > 5) {
                            for (int j = 0; j < size_of_coordinate_of_point_list; ++j) {
                                if (!(coordinate_of_point_list.get(j)).equals("")) {
                                    int single_number = Integer.valueOf(coordinate_of_point_list.get(j));
                                    array_of_numbers.add(single_number);
                                }
                            }
                        }
                        if (array_of_numbers.size() > 5) {
                            points_filtered_list.add(array_of_numbers);
                        }
                    }
                    data_new.add(points_filtered_list);
                }
            }
            if (counter < 2) {
                fillModel();
            } else {
                data = data_new;
                actionList = actionList_new;
            }
            fIn.close();
            // FIND OUT ALL STRING DATA FROM STAFF FILE
            // PARSE IT INTO data AND actionList

        } catch (FileNotFoundException e) {
            fillModel();
            Log.e("login activity", "File not found: " + e);
        } catch (IOException e) {
            fillModel();
            Log.e("login activity", "Can not read file: " + e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // The sensor type (as defined in the Sensor class).
        int sensorType = sensorEvent.sensor.getType();

        // The sensorEvent object is reused across calls to onSensorChanged().
        // clone() gets a copy so the data doesn't change out from under us
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;
            default:
                return;
        }
        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        // The second argument is an inclination matrix, which isn't
        // used in this example.
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);

        // Remap the matrix based on current device/activity rotation.
        float[] rotationMatrixAdjusted = new float[9];
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted);
                break;
        }

        // Get the orientation of the device (azimuth, pitch, roll) based
        // on the rotation matrix. Output units are radians.
        float[] orientationValues = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);
        }

        // Pull out the individual values from the array.
        azimuth = orientationValues[0];
        pitch = orientationValues[1];
        roll = orientationValues[2];

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    //Listeners for the sensors are registered in this callback so that
    //they can be unregistered in onStop().
    //
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
        writeToFile();
    }

    private final Runnable audioCommandsRunnable = new Runnable() {
        @Override
        public void run() {
            speechRecognizer.startListening(intentRecognizer);
            audioCommandsHandler.postDelayed(this, 3000);
        }
    };

    private final Runnable sensorsRunnable = new Runnable() {
        @Override
        public void run() {
            if (keys_voice_axis_state == 2) {
                if (azimuth >= 0.2) {
                    String padded = String.format("5%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 5 via bluetooth
                } else if (azimuth <= -0.2) {
                    String padded = String.format("6%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 6 via bluetooth
                }
                if (pitch >= 0.2) {
                    String padded = String.format("4%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 4 via bluetooth
                } else if (pitch <= -0.2) {
                    String padded = String.format("2%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 2 via bluetooth
                }
                if (roll >= 0.2) {
                    String padded = String.format("3%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 3 via bluetooth
                } else if (roll <= -0.2) {
                    String padded = String.format("1%03d", progress_value);
                    mConnectedThread.write(padded);   // Send 1 via bluetooth
                }
            }
            sensorHandler.postDelayed(this, sensorsListenDelay);
        }
    };

    private final Runnable LoopPointsRunnable = new Runnable() {
        private void delay(int delay_time) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Magic Here
                }
            }, delay_time); // 150 ms delay
        }

        @Override
        public void run() {
            // ABSOLUTE POSITIONS IS LIKE DEFAULT BUT WITH CAPS (FORWARD+, BACKWARD-)
            if (state != 0) {
                ArrayList<Integer> staff = base_points.get(current_step);
                int abs_x = staff.get(0);
                int abs_y = staff.get(1);
                int abs_z = staff.get(2);
                int delay_between_movements = staff.get(3);
                int abs_rot = staff.get(4);
                int abs_grip = staff.get(5);
                if (abs_x >= 0) {
                    mConnectedThread.write(String.format("!%03d", abs_x));
                } else {
                    mConnectedThread.write(String.format("@%03d", -abs_x));
                }
                if (abs_y >= 0) {
                    mConnectedThread.write(String.format("#%03d", abs_y));
                } else {
                    mConnectedThread.write(String.format("$%03d", -abs_y));
                }
                if (abs_z >= 0) {
                    mConnectedThread.write(String.format("?%03d", abs_z));
                } else {
                    mConnectedThread.write(String.format("^%03d", -abs_z));
                }
                if (abs_rot > 0) {
                    mConnectedThread.write(String.format("&%03d", abs_rot));
                }
                if (abs_grip > 0) {
                    mConnectedThread.write(String.format("(%03d", abs_grip));
                }
                if (current_step < (base_points.size() - 1)) {
                    current_step++;
                    points_moving_loop.postDelayed(this, delay_between_movements);
                } else if (current_step == (base_points.size() - 1)) {
                    if (state == 2) {
                        current_step = 0;
                        points_moving_loop.postDelayed(this, delay_between_movements);
                    }
                }
            } else {
                points_moving_loop.removeCallbacks(LoopPointsRunnable);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private class MyListAdaper extends ArrayAdapter<ArrayList<Integer>> {
        private final int layout;
        private final ArrayList<ArrayList<Integer>> mObjects;

        private MyListAdaper(Context context, int resource, ArrayList<ArrayList<Integer>> objects) {
            super(context, resource, objects);
            mObjects = objects;
            layout = resource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder mainViewholder = null;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.title_x = (TextView) convertView.findViewById(R.id.x_axis_title);
                viewHolder.title_y = (TextView) convertView.findViewById(R.id.y_axis_title);
                viewHolder.title_z = (TextView) convertView.findViewById(R.id.z_axis_title);
                viewHolder.title_delay = (TextView) convertView.findViewById(R.id.delay_title);
                viewHolder.delay_dim = (TextView) convertView.findViewById(R.id.delay_dim);
                viewHolder.data_delay = (TextView) convertView.findViewById(R.id.delay_data);
                viewHolder.data_x = (TextView) convertView.findViewById(R.id.x_data);
                viewHolder.data_y = (TextView) convertView.findViewById(R.id.y_data);
                viewHolder.data_z = (TextView) convertView.findViewById(R.id.z_data);
                viewHolder.data_grip = (TextView) convertView.findViewById(R.id.grip_data);
                viewHolder.data_rot = (TextView) convertView.findViewById(R.id.rot_data);
                viewHolder.up = (Button) convertView.findViewById(R.id.up_buttons);
                viewHolder.down = (Button) convertView.findViewById(R.id.down_buttons);
                viewHolder.delete = (Button) convertView.findViewById(R.id.delete_buttons);
                convertView.setTag(viewHolder);
            }
            mainViewholder = (ViewHolder) convertView.getTag();
            mainViewholder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    mObjects.remove(position);
                    adapter.notifyDataSetChanged();
                }
            });

            mainViewholder.up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if (position > 0 && mObjects.size() > 1) {
                        Collections.swap(mObjects, position, position - 1);
                        adapter.notifyDataSetChanged();
                    }
                }
            });

            mainViewholder.down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    points_moving_loop.removeCallbacks(LoopPointsRunnable);
                    if ((position < (mObjects.size() - 1)) && mObjects.size() > 1) {
                        Collections.swap(mObjects, position, position + 1);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            mainViewholder.data_x.setText(mObjects.get(position).get(0).toString());
            mainViewholder.data_y.setText(mObjects.get(position).get(1).toString());
            mainViewholder.data_z.setText(mObjects.get(position).get(2).toString());
            mainViewholder.data_delay.setText(mObjects.get(position).get(3).toString());
            mainViewholder.data_rot.setText(mObjects.get(position).get(4).toString());
            mainViewholder.data_grip.setText(mObjects.get(position).get(5).toString());
            return convertView;
        }
    }

    public class ViewHolder {
        TextView title_x;
        TextView title_y;
        TextView title_z;
        TextView title_delay;
        TextView delay_dim;
        TextView data_x;
        TextView data_y;
        TextView data_z;
        TextView data_rot;
        TextView data_grip;
        TextView data_delay;
        Button up;
        Button down;
        Button delete;
    }

    public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.MyViewHolder> {
        private final ArrayList<ActionModel> actionList;
        TextView action_name;
        Button edit_button, apply_button, loop_button, delete_button;

        class MyViewHolder extends RecyclerView.ViewHolder {
            MyViewHolder(View view) {
                super(view);
                action_name = view.findViewById(R.id.action_item_name);
                edit_button = view.findViewById(R.id.edit_button);
                edit_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        position_of_points = getAdapterPosition();
                        adapter = new MyListAdaper(context_for_adapter, R.layout.customlayout, data.get(position_of_points));
                        lv.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                });
                delete_button = view.findViewById(R.id.delete_button);
                delete_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        int current_position = getAdapterPosition();
                        if (data.size() > 1) {
                            actionList.remove(current_position);
                            data.remove(current_position);
                            if (current_position <= position_of_points) {
                                if (position_of_points >= 1) {
                                    position_of_points -= 1;
                                    adapter = new MyListAdaper(context_for_adapter, R.layout.customlayout, data.get(position_of_points));
                                    lv.setAdapter(adapter);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            mAdapter.notifyItemRemoved(current_position);
                        }
                    }
                });
                apply_button = view.findViewById(R.id.apply_button);
                apply_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        state = 1;
                        base_points = data.get(getAdapterPosition());
                        current_step = 0;
                        LoopPointsRunnable.run();
                        if (current_step == (base_points.size() - 1)) {
                            current_step = 0;
                            state = 0;
                            points_moving_loop.removeCallbacks(LoopPointsRunnable);
                        }
                    }
                });
                loop_button = view.findViewById(R.id.loop_button);
                loop_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        state = 2;
                        base_points = data.get(getAdapterPosition());
                        current_step = 0;
                        LoopPointsRunnable.run();
                    }
                });
            }
        }

        public ActionAdapter(ArrayList<ActionModel> actionList) {
            this.actionList = actionList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_list, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            ActionModel movie = actionList.get(position);
            action_name.setText(movie.getName());
            //action_full_delay.setText(Integer.toString(movie.getDelay()));
        }

        @Override
        public int getItemCount() {
            return actionList.size();
        }
    }


    public class ActionModel {
        private String action_model_name;
        private int delay;

        public ActionModel() {
        }

        public ActionModel(String title, int delay) {
            this.action_model_name = title;
            this.delay = delay;
        }

        public String getName() {
            return action_model_name;
        }

        public void setName(String action_model_name) {
            this.action_model_name = action_model_name;
        }

        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }
    }
}
