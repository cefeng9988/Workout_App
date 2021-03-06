package com.example.w5_p3;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView list;
    private TextView txtStepsDisplay, txtModeDisplay;
    private Button btnStart, btnStop;
    private boolean easy, medium, hard, start, stop, songPlayed;
    private int steps = 0, easySig = 350, mediumSig = 600, hardSig = 900;
    private long timeStart, timeEnd;
    final String[] workouts = {"Easy", "Medium", "Hard"};

    private MediaPlayer media;
    private double acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private CameraManager CamManager;
    private String CamID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        list = (ListView) findViewById(R.id.list);
        txtModeDisplay = (TextView) findViewById(R.id.txtModeDisplay);
        txtStepsDisplay = (TextView) findViewById(R.id.txtStepsDisplay);
        //prevent screen from turning during workout and triggering onDestroy()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // initialize acceleration values
        songPlayed = false;
        acceleration = 0.00;
        currentAcceleration = SensorManager.GRAVITY_EARTH;   //We live on Earth.
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        //initialize camera permissions
        CamManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CamID = CamManager.getCameraIdList()[0];  //rear camera is at index 0
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        //start button logic
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(easy == false && medium == false && hard == false){
                    Toast.makeText(MainActivity.this, "Please select a workout", Toast.LENGTH_SHORT).show();
                }else {
                    //start time tracking
                    start = true;
                    stop = false;
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    timeStart = System.currentTimeMillis();
                    enableAccelerometerListening();
                }
            }
        });
        //stop button logic
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWorkout();
            }
        });

        //adapter to bind our workout contents to ListView list
        ArrayAdapter arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, workouts);
        list.setAdapter(arrayAdapter);  //sets our ListView with workout content

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String workout = String.valueOf(parent.getItemAtPosition(position));
                //doing each check here to verify position and workout, the workouts[] may have changes
                if(workout.equals("Easy")){
                    if(start==true){
                        Toast.makeText(MainActivity.this, "Can't change workout in the middle of one", Toast.LENGTH_SHORT).show();
                    }else {
                        easy = true;
                        medium = false;
                        hard = false;
                        txtModeDisplay.setText("Easy Mode");
                    }
                } else if(workout.equals("Medium")){
                    if(start==true){
                        Toast.makeText(MainActivity.this, "Can't change workout in the middle of one", Toast.LENGTH_SHORT).show();
                    }else {
                        medium = true;
                        easy = false;
                        hard = false;
                        txtModeDisplay.setText("Medium Mode");
                    }
                } else if(workout.equals("Hard")){
                    if(start==true){
                        Toast.makeText(MainActivity.this, "Can't change workout in the middle of one", Toast.LENGTH_SHORT).show();
                    }else {
                        hard = true;
                        easy = false;
                        medium = false;
                        txtModeDisplay.setText("Hard Mode");
                    }
                }
            }
        });
    }

    private void stopWorkout() {
        //stop time tracking
        stop = true;
        start = false;
        easy = false;
        medium = false;
        hard = false;
        steps = 0;
        songPlayed = false;
        media.stop();
        txtStepsDisplay.setText("Number of steps: "+steps);
        txtModeDisplay.setText("Choose a workout");
        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
        disableAccelerometerListening();
    }

    //onSensor initialization
    //sensor stays on but only starts counting if accThreshold hits, easy/medium/hard and start is true
            //once that's true, increment steps and have if's to check >20 steps etc...
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSensorChanged(SensorEvent event) {
            txtStepsDisplay = (TextView) findViewById(R.id.txtStepsDisplay);
            // get x, y, and z sensor data from SensorEvent
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // calculates acceleration with last and current acceleration
            lastAcceleration = currentAcceleration;
            currentAcceleration = x * x + y * y + z * z;   //This is a simplified calculation, to be real we would need time and a square root.
            acceleration = Math.sqrt(currentAcceleration *  (currentAcceleration - lastAcceleration)); //sqrt so unit is more reasonable to work with

            // if the acceleration is above a certain threshold
            if (acceleration>easySig && start==true && easy==true) {
                Log.e("TAG", "Easy mode and steps:" + steps);
                steps++;
                txtStepsDisplay.setText("Number of steps: "+steps);
                if(steps>30 && steps<100){
                    //play superman song
                    //songPlayed to check if song is already playing
                    if(!songPlayed) {  //if song has not been played
                        songPlayed = true;
                        media = MediaPlayer.create(MainActivity.this, R.raw.superman);
                        media.start();
                    }
                } else if(steps > 100){
                    timeElapsed();
                    stopWorkout();
                }
            } else if (acceleration>mediumSig && start==true && medium==true) {
                Log.e("TAG", "Medium mode and steps:" + steps);
                steps++;
                txtStepsDisplay.setText("Number of steps: "+steps);
                if(steps>50 && steps<100){
                    //play star wars song
                    //songPlayed to check if song is already playing
                    if(!songPlayed) {  //if song has not been played
                        songPlayed = true;
                        media = MediaPlayer.create(MainActivity.this, R.raw.starwars);
                        media.start();
                    }
                }else if(steps > 100){
                    timeElapsed();
                    stopWorkout();
                }
            } else if (acceleration>hardSig && start==true && hard==true) {
                Log.e("TAG", "Hard mode and steps:" + steps);
                steps++;
                txtStepsDisplay.setText("Number of steps: "+steps);
                if(steps > 20 && steps<50){
                    LightOn();
                }else if(steps > 50 && steps<100){
                    //play Rocky song
                    //songPlayed to check if song is already playing
                    if(!songPlayed) {  //if song has not been played
                        songPlayed = true;
                        media = MediaPlayer.create(MainActivity.this, R.raw.rocky);
                        media.start();
                    }
                    LightOn();
                }else if(steps > 100){
                    timeElapsed();
                    stopWorkout();
                }
            }
            else
                LightOff();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    //Displays toast of time passed from start to end in seconds
    public void timeElapsed(){
        timeEnd = System.currentTimeMillis();
        long timeDelta = timeEnd - timeStart;
        double elapsedSeconds = timeDelta / 1000;
        Toast.makeText(MainActivity.this, "Total time elapsed: " + elapsedSeconds, Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void LightOn()
    {
        try {
            CamManager.setTorchMode(CamID, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void LightOff()
    {
        try {
            CamManager.setTorchMode(CamID, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //stop AccelerometerListening saves battery
    @Override
    protected void onStop() {
        Log.i("TAG", "onStop Triggered.");
        disableAccelerometerListening();
        super.onStop();
    }

    // enable listening for accelerometer events
    private void enableAccelerometerListening() {
        // This is how we get the reference to the device's SensorManager.
        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);    //The last parm specifies the type of Sensor we want to monitor
        //3 parms, The Listener, Sensor Type (accelerometer), and Sampling Frequency.
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);   //don't set this too high, otw you will kill user's battery.
    }

    // disable listening for accelerometer events
    private void disableAccelerometerListening() {
        // get the SensorManager
        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        // stop listening for accelerometer events
        sensorManager.unregisterListener(sensorEventListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

}