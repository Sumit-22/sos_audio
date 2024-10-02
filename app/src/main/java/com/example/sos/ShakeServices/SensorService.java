package com.example.sos.ShakeServices;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.example.sos.R;
import com.example.sos.dbHelper.DbHelper;
import com.example.sos.model.ContactModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


import java.util.List;

public class SensorService extends Service {

    private static final String TAG = "SensorService";
    private static final String CHANNEL_ID = "SensorServiceChannel";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false; // To prevent multiple recordings at the same time
    private FusedLocationProviderClient fusedLocationClient;

    public SensorService() {}

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
        startMyOwnForeground(); // Start the foreground service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();

        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onShake(int count) {
                Log.d(TAG, "Shake detected with count: " + count);
                if (count == 3) {
                    vibrate();
                    handleLocationAndSendSms();
                }
            }
        });

        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void handleLocationAndSendSms() {
        Log.d(TAG, "Fetching location and sending SMS...");

        // Check for permissions
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted.");
            return; // Exit if permissions are not granted
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            SmsManager smsManager = SmsManager.getDefault();
            DbHelper db = new DbHelper(SensorService.this);
            List<ContactModel> contacts = db.getAllContacts();
            Log.d(TAG, "Contacts retrieved: " + contacts.size());

            String message;
            // Retrieve user information from SharedPreferences
            String userInfo = getUserInfo();

            if (location != null) {
                message = "Hey,I am in DANGER! " +
                        "Please urgently reach me out. My coordinates:\n" +
                        "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                Log.d("LAUDE", "Message Length: " + message.length());
            } else {
                message = "I am in DANGER! Here are my details:\n" + userInfo +
                        "\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";
                Log.d("BC", "Message Length: " + message.length());
            }

            for (ContactModel contact : contacts) {
                try {
                    smsManager.sendTextMessage(contact.getPhoneNo(), null, message, null, null);
                    smsManager.sendTextMessage(contact.getPhoneNo(), null, userInfo, null, null);
                    Log.d(TAG, "SMS sent to: " + contact.getPhoneNo());
                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to: " + contact.getPhoneNo(), e);
                }
            }
            Log.d(TAG, "SMS sent to contacts.");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to retrieve location.", e);
            String fallbackMessage = "I am in DANGER! Here are my details:\n" + getUserInfo() +
                    "\nGPS was turned off. Couldn't find location. Call your nearest Police Station.";

            SmsManager smsManager = SmsManager.getDefault();
            DbHelper db = new DbHelper(SensorService.this);
            List<ContactModel> contacts = db.getAllContacts();

            for (ContactModel contact : contacts) {
                try {
                    smsManager.sendTextMessage(contact.getPhoneNo(), null, fallbackMessage, null, null);
                    smsManager.sendTextMessage(contact.getPhoneNo(), null, getUserInfo(), null, null);
                    Log.d(TAG, "Fallback SMS sent to: " + contact.getPhoneNo());
                } catch (Exception ex) {
                    Log.e(TAG, "Error sending fallback SMS to: " + contact.getPhoneNo(), ex);
                }
            }
        });
    }

    public String getUserInfo() {
        SharedPreferences prefs = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String name = prefs.getString("Name", "Unknown");
        String dob = prefs.getString("DOB", "Unknown");
        String bloodGroup = prefs.getString("BloodGroup", "Unknown");
        boolean isDiabetic = prefs.getBoolean("IsDiabetic", false);
        boolean isDisabled = prefs.getBoolean("IsDisabled", false);

        String userInfo = String.format("Here are my details:\nName: %s\nDOB: %s\nBlood Group: %s\nDiabetic: %s\nPhysically Disabled: %s",
                name, dob, bloodGroup, isDiabetic ? "Yes" : "No", isDisabled ? "Yes" : "No");

        // Log user info for debugging
        Log.d(TAG, "User Info: " + userInfo);

        return userInfo;
    }

    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            VibrationEffect vibEff;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibEff = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);
                vibrator.cancel();
                vibrator.vibrate(vibEff);
            } else {
                vibrator.vibrate(200); // Fallback for older versions
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("You are protected.")
                    .setContentText("We are there for you")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            startForeground(1, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed.");
        super.onDestroy();
    }
}
