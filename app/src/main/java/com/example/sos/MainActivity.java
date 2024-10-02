package com.example.sos;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.sos.ShakeServices.ReactivateService;
import com.example.sos.ShakeServices.SensorService;
import com.example.sos.adapter.CustomAdapter;
import com.example.sos.model.ContactModel;
import com.example.sos.dbHelper.DbHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002;
    private static final int PICK_CONTACT = 1;

    // create instances of various classes to be used
    Button button1;
    ListView listView;
    DbHelper db;
    List<ContactModel> list;
    CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check for runtime permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.RECORD_AUDIO,  // Audio permission
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,  // Storage permission
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    100);
        }

        // this is a special permission required only by devices using
        // Android Q and above. The Access Background Permission is responsible
        // for populating the dialog with "ALLOW ALL THE TIME" option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 100);
        }

        // check for BatteryOptimization,
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                askIgnoreOptimization();
            }
        }

        // start the service
        SensorService sensorService = new SensorService();
        Intent intent = new Intent(this, sensorService.getClass());
        if (!isMyServiceRunning(sensorService.getClass())) {
            startService(intent);
        }


        button1 = findViewById(R.id.Button1);
        listView = (ListView) findViewById(R.id.ListView);
        db = new DbHelper(this);
        list = db.getAllContacts();
        customAdapter = new CustomAdapter(this, list);
        listView.setAdapter(customAdapter);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // calling of getContacts()
                if (db.count() != 5) {
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, PICK_CONTACT);
                } else {
                    Toast.makeText(MainActivity.this, "Can't Add more than 5 Contacts", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to check if the service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service status", "Running");
                return true;
            }
        }
        Log.i("Service status", "Not running");
        return false;
    }

    @Override
    protected void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, ReactivateService.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            // Make sure the number of permissions matches the grantResults array length
            if (grantResults.length >= 6) {
                boolean locationGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean smsGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean contactsGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean audioGranted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean storageGranted = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                boolean readStorageGranted = grantResults[5] == PackageManager.PERMISSION_GRANTED;

                if (!locationGranted || !smsGranted || !contactsGranted || !audioGranted || !storageGranted || !readStorageGranted) {
                    Toast.makeText(this, "Permissions Denied!\nCan't use the App!", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle the case where not all permissions were requested or granted
                Toast.makeText(this, "Some permissions are missing. Please grant all permissions to use the app.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK && data != null) {
            Uri contactData = data.getData();
            if (contactData != null) {
                Cursor c = getContentResolver().query(contactData, null, null, null, null);
                if (c != null && c.moveToFirst()) {

                    // Check if the column exists before accessing it
                    int idIndex = c.getColumnIndex(ContactsContract.Contacts._ID);
                    int hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    int displayNameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

                    if (idIndex != -1 && hasPhoneIndex != -1 && displayNameIndex != -1) {
                        String id = c.getString(idIndex);
                        String hasPhone = c.getString(hasPhoneIndex);
                        String phone = null;

                        if ("1".equals(hasPhone)) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{id},
                                    null
                            );
                            if (phones != null && phones.moveToFirst()) {
                                int phoneIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                if (phoneIndex != -1) {
                                    phone = phones.getString(phoneIndex);
                                }
                                phones.close();
                            }
                        }

                        String name = c.getString(displayNameIndex);
                        db.addcontact(new ContactModel(0, name, phone));
                        list = db.getAllContacts();
                        customAdapter.refresh(list);
                    } else {
                        Toast.makeText(this, "Unable to find required contact details", Toast.LENGTH_SHORT).show();
                    }

                    c.close();
                } else {
                    Toast.makeText(this, "No contact found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // this method prompts the user to remove any
    // battery optimisation constraints from the App
    private void askIgnoreOptimization() {

        @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST);

    }

}