//////package com.example.sos.ShakeServices;
//////
//////import android.Manifest;
//////import android.annotation.SuppressLint;
//////import android.app.Notification;
//////import android.app.NotificationChannel;
//////import android.app.NotificationManager;
//////import android.app.Service;
//////import android.content.Context;
//////import android.content.Intent;
//////import android.content.pm.PackageManager;
//////import android.hardware.Sensor;
//////import android.hardware.SensorManager;
//////import android.location.Location;
//////import android.os.Build;
//////import android.os.IBinder;
//////
//////import android.os.VibrationEffect;
//////import android.os.Vibrator;
//////import android.telephony.SmsManager;
//////import android.util.Log;
//////
//////import androidx.annotation.NonNull;
//////import androidx.annotation.RequiresApi;
//////import androidx.core.app.ActivityCompat;
//////import androidx.core.app.NotificationCompat;
//////
//////import com.example.sos.R;
//////import com.example.sos.model.ContactModel;
//////import com.example.sos.dbHelper.DbHelper;
//////import com.google.android.gms.location.FusedLocationProviderClient;
//////import com.google.android.gms.location.LocationServices;
//////import com.google.android.gms.tasks.OnFailureListener;
//////import com.google.android.gms.tasks.OnSuccessListener;
//////
//////import java.util.List;
//////
//////public class SensorService extends Service {
//////
//////    private SensorManager mSensorManager;
//////    private Sensor mAccelerometer;
//////    private ShakeDetector mShakeDetector;
//////    private static final String CHANNEL_ID = "SensorServiceChannel";
//////
//////    public SensorService() {
//////    }
//////
//////    @Override
//////    public IBinder onBind(Intent intent) {
//////        throw new UnsupportedOperationException("Not yet implemented");
//////    }
//////
//////    @Override
//////    public int onStartCommand(Intent intent, int flags, int startId) {
//////        super.onStartCommand(intent, flags, startId);
//////        return START_STICKY;
//////    }
//////
//////    @Override
//////    public void onCreate() {
//////        super.onCreate();
//////        createNotificationChannel(); // Create notification channel for API >= 26
//////
//////        // Start the foreground service
//////        startMyOwnForeground();
//////
//////        // ShakeDetector initialization
//////        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//////        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//////        mShakeDetector = new ShakeDetector();
//////        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
//////
//////            @SuppressLint("MissingPermission")
//////            @Override
//////            public void onShake(int count) {
//////                if (count == 3) {
//////                    vibrate();
//////                    handleLocationAndSendSms();
//////                }
//////            }
//////        });
//////
//////        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
//////    }
//////
//////    // Inside your handleLocationAndSendSms method
//////    private void handleLocationAndSendSms() {
//////        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
//////
//////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//////                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//////            // Request permissions if not granted
//////            return;
//////        }
//////
//////        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
//////            @Override
//////            public void onSuccess(Location location) {
//////                SmsManager smsManager = SmsManager.getDefault();
//////                DbHelper db = new DbHelper(SensorService.this);
//////                List<ContactModel> list = db.getAllContacts();
//////
//////                String message;
//////                if (location != null) {
//////                    message = "Hey, I am in DANGER! Please urgently reach me out. Here are my coordinates:\n" +
//////                            "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
//////                } else {
//////                    message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
//////                }
//////
//////                for (ContactModel c : list) {
//////                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
//////                }
//////            }
//////        }).addOnFailureListener(new OnFailureListener() {
//////            @Override
//////            public void onFailure(@NonNull Exception e) {
//////                Log.d("Check: ", "OnFailure");
//////                String message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
//////                SmsManager smsManager = SmsManager.getDefault();
//////                DbHelper db = new DbHelper(SensorService.this);
//////                List<ContactModel> list = db.getAllContacts();
//////                for (ContactModel c : list) {
//////                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
//////                }
//////            }
//////        });
//////    }
//////
//////    public void vibrate() {
//////        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//////        VibrationEffect vibEff;
//////
//////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//////            vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);
//////            vibrator.cancel();
//////            vibrator.vibrate(vibEff);
//////        } else {
//////            vibrator.vibrate(200);
//////        }
//////    }
//////
//////    @RequiresApi(Build.VERSION_CODES.O)
//////    private void startMyOwnForeground() {
//////        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
//////        NotificationManager manager = getSystemService(NotificationManager.class);
//////        if (manager != null) {
//////            manager.createNotificationChannel(channel);
//////        }
//////
//////        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//////                .setContentTitle("You are protected.")
//////                .setContentText("We are there for you")
//////                .setSmallIcon(R.drawable.ic_launcher_foreground)
//////                .build();
//////
//////        startForeground(1, notification);
//////    }
//////
//////    private void createNotificationChannel() {
//////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//////            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
//////            NotificationManager manager = getSystemService(NotificationManager.class);
//////            if (manager != null) {
//////                manager.createNotificationChannel(channel);
//////            }
//////        }
//////    }
//////
//////    @Override
//////    public void onDestroy() {
//////        Intent broadcastIntent = new Intent();
//////        broadcastIntent.setAction("restartservice");
//////        broadcastIntent.setClass(this, ReactivateService.class);
//////        this.sendBroadcast(broadcastIntent);
//////        super.onDestroy();
//////    }
//////}
////
////
////
////
////package com.example.sos.ShakeServices;
////
////import android.content.Context;
////import android.content.Intent;
////import android.hardware.Sensor;
////import android.hardware.SensorEvent;
////import android.hardware.SensorEventListener;
////import android.hardware.SensorManager;
////import android.media.MediaRecorder;
////import android.os.Environment;
////import android.os.Handler;
////import android.util.Log;
////
////import java.io.File;
////import java.io.IOException;
////
////public class SensorService extends Service implements SensorEventListener {
////
////    private SensorManager sensorManager;
////    private MediaRecorder mediaRecorder;
////    private long lastShakeTime = 0;
////    private static final int SHAKE_THRESHOLD = 800;  // Adjust sensitivity as needed
////
////    @Override
////    public void onCreate() {
////        super.onCreate();
////        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
////        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
////        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
////    }
////
////    @Override
////    public int onStartCommand(Intent intent, int flags, int startId) {
////        return START_STICKY;
////    }
////
////    @Override
////    public void onSensorChanged(SensorEvent event) {
////        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
////            detectShake(event);
////        }
////    }
////
////    private void detectShake(SensorEvent event) {
////        float x = event.values[0];
////        float y = event.values[1];
////        float z = event.values[2];
////
////        float acceleration = (x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
////        long currentTime = System.currentTimeMillis();
////
////        if (acceleration >= SHAKE_THRESHOLD && currentTime - lastShakeTime > 2000) {  // Shake threshold and delay between shakes
////            lastShakeTime = currentTime;
////            Log.d("Shake Detected", "Shake Detected! Starting recording...");
////            startRecording();
////        }
////    }
////
////    private void startRecording() {
////        mediaRecorder = new MediaRecorder();
////        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
////        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
////        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
////
////        // File storage location
////        String audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/shake_audio.3gp";
////        mediaRecorder.setOutputFile(audioFilePath);
////
////        try {
////            mediaRecorder.prepare();
////            mediaRecorder.start();
////
////            // Automatically stop recording after 10 seconds
////            new Handler().postDelayed(this::stopRecording, 10000);  // 10 seconds
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
////
////    private void stopRecording() {
////        try {
////            mediaRecorder.stop();
////            mediaRecorder.release();
////            mediaRecorder = null;
////            Log.d("Recording", "Recording saved successfully!");
////        } catch (RuntimeException stopException) {
////            stopException.printStackTrace();
////        }
////    }
////
////    @Override
////    public void onAccuracyChanged(Sensor sensor, int accuracy) {
////        // Not used
////    }
////
////    @Override
////    public void onDestroy() {
////        super.onDestroy();
////        sensorManager.unregisterListener(this);
////    }
////
////    @Nullable
////    @Override
////    public IBinder onBind(Intent intent) {
////        return null;
////    }
////}
//
//
//package com.example.sos.ShakeServices;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.hardware.Sensor;
//import android.hardware.SensorManager;
//import android.location.Location;
//import android.media.MediaRecorder;
//import android.os.Build;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.VibrationEffect;
//import android.os.Vibrator;
//import android.telephony.SmsManager;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.RequiresApi;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//
//import com.example.sos.R;
//import com.example.sos.dbHelper.DbHelper;
//import com.example.sos.model.ContactModel;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//
//public class SensorService extends Service {
//
//    private SensorManager mSensorManager;
//    private Sensor mAccelerometer;
//    private ShakeDetector mShakeDetector;
//    private static final String CHANNEL_ID = "SensorServiceChannel";
//    private MediaRecorder mediaRecorder;
//    private boolean isRecording = false;  // To prevent multiple recordings at the same time
//
//    public SensorService() {
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        super.onStartCommand(intent, flags, startId);
//        return START_STICKY;
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        createNotificationChannel(); // Create notification channel for API >= 26
//
//        // Start the foreground service
//        startMyOwnForeground();
//
//        // ShakeDetector initialization
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mShakeDetector = new ShakeDetector();
//        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
//
//            @SuppressLint("MissingPermission")
//            @Override
//            public void onShake(int count) {
//                if (count == 3) {
//                    vibrate();
//                    handleLocationAndSendSms();
//
//                    // Start audio recording after shaking
//                    if (!isRecording) {
//                        startRecording();
//                    }
//                }
//            }
//        });
//
//        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
//    }
//
//    // Start recording audio for 10 seconds
//    private void startRecording() {
//        isRecording = true;
//        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//
//        // File storage location
//        String audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/shake_audio_" + System.currentTimeMillis() + ".3gp";
//        mediaRecorder.setOutputFile(audioFilePath);
//
//        try {
//            mediaRecorder.prepare();
//            mediaRecorder.start();
//
//            // Automatically stop recording after 10 seconds
//            new Handler().postDelayed(this::stopRecording, 10000);  // 10 seconds
//        } catch (IOException e) {
//            e.printStackTrace();
//            isRecording = false;
//        }
//    }
//
//    // Stop the recording
//    private void stopRecording() {
//        try {
//            if (mediaRecorder != null) {
//                mediaRecorder.stop();
//                mediaRecorder.release();
//                mediaRecorder = null;
//                isRecording = false;
//                Log.d("Recording", "Recording saved successfully!");
//            }
//        } catch (RuntimeException stopException) {
//            stopException.printStackTrace();
//            isRecording = false;
//        }
//    }
//
//    // Handle sending location and SMS to contacts
//    private void handleLocationAndSendSms() {
//        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Request permissions if not granted
//            return;
//        }
//
//        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
//            @Override
//            public void onSuccess(Location location) {
//                SmsManager smsManager = SmsManager.getDefault();
//                DbHelper db = new DbHelper(SensorService.this);
//                List<ContactModel> list = db.getAllContacts();
//
//                String message;
//                if (location != null) {
//                    message = "Hey, I am in DANGER! Please urgently reach me out. Here are my coordinates:\n" +
//                            "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
//                } else {
//                    message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
//                }
//
//                for (ContactModel c : list) {
//                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d("Check: ", "OnFailure");
//                String message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
//                SmsManager smsManager = SmsManager.getDefault();
//                DbHelper db = new DbHelper(SensorService.this);
//                List<ContactModel> list = db.getAllContacts();
//                for (ContactModel c : list) {
//                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
//                }
//            }
//        });
//    }
//
//    public void vibrate() {
//        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        VibrationEffect vibEff;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);
//            vibrator.cancel();
//            vibrator.vibrate(vibEff);
//        } else {
//            vibrator.vibrate(200);
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private void startMyOwnForeground() {
//        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
//        NotificationManager manager = getSystemService(NotificationManager.class);
//        if (manager != null) {
//            manager.createNotificationChannel(channel);
//        }
//
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("You are protected.")
//                .setContentText("We are there for you")
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .build();
//
//        startForeground(1, notification);
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction("restartservice");
//        broadcastIntent.setClass(this, ReactivateService.class);
//        this.sendBroadcast(broadcastIntent);
//        super.onDestroy();
//    }
//}
//
//
//
//
//
//
//





package com.example.sos.ShakeServices;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.sos.R;
import com.example.sos.dbHelper.DbHelper;
import com.example.sos.model.ContactModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SensorService extends Service {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private static final String CHANNEL_ID = "SensorServiceChannel";
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;  // To prevent multiple recordings at the same time

    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(); // Create notification channel for API >= 26

        // Start the foreground service
        startMyOwnForeground();

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @SuppressLint("MissingPermission")
            @Override
            public void onShake(int count) {
                if (count == 3) {
                    vibrate();
                    handleLocationAndSendSms();

                    // Start audio recording after shaking
                    if (!isRecording) {
                        startRecording();
                    }
                }
            }
        });

        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    // Start recording audio for 10 seconds
    // Start recording audio for 10 seconds
    // Start recording audio for 10 seconds
    // Start recording audio for 10 seconds
    private void startRecording() {
        isRecording = true;

        mediaRecorder = new MediaRecorder();

        // Set audio source to the microphone
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // Output format for the audio file (use MPEG_4 for better compatibility)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // File storage location
        String audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/shake_audio_" + System.currentTimeMillis() + ".mp4";
        mediaRecorder.setOutputFile(audioFilePath);

        // Set audio encoder
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // Set audio encoding bit rate (optional but can improve quality)
        mediaRecorder.setAudioEncodingBitRate(128000);

        // Set audio sampling rate (optional but can improve quality)
        mediaRecorder.setAudioSamplingRate(44100);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

            // Automatically stop recording after 10 seconds
            new Handler().postDelayed(this::stopRecording, 10000);  // 10 seconds
        } catch (IOException e) {
            e.printStackTrace();
            isRecording = false;
        }
    }





    // Stop the recording
    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                Log.d("Recording", "Recording saved successfully!");
            }
        } catch (RuntimeException stopException) {
            stopException.printStackTrace();
            isRecording = false;
        }
    }

    // Handle sending location and SMS to contacts
    private void handleLocationAndSendSms() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                SmsManager smsManager = SmsManager.getDefault();
                DbHelper db = new DbHelper(SensorService.this);
                List<ContactModel> list = db.getAllContacts();

                String message;
                if (location != null) {
                    message = "Hey, I am in DANGER! Please urgently reach me out. Here are my coordinates:\n" +
                            "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                } else {
                    message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
                }

                for (ContactModel c : list) {
                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Check: ", "OnFailure");
                String message = "I am in DANGER! Please urgently reach me out.\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
                SmsManager smsManager = SmsManager.getDefault();
                DbHelper db = new DbHelper(SensorService.this);
                List<ContactModel> list = db.getAllContacts();
                for (ContactModel c : list) {
                    smsManager.sendTextMessage(c.getPhoneNo(), null, message, null, null);
                }
            }
        });
    }

    public void vibrate() {
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect vibEff;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);
            vibrator.cancel();
            vibrator.vibrate(vibEff);
        } else {
            vibrator.vibrate(200);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("You are protected.")
                .setContentText("We are there for you")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, ReactivateService.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }
}
