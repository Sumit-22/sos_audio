package com.example.sos;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sos.ShakeServices.SensorService;

import java.util.Calendar;

public class activity_user_info extends AppCompatActivity {

    private EditText editTextName, editTextDOB;
    private Spinner spinnerBloodGroup;
    private RadioGroup radioGroupDiabetic, radioGroupDisabled;
    private String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Check if user info is already saved
        if (isUserInfoSaved()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        editTextName = findViewById(R.id.editTextName);
        editTextDOB = findViewById(R.id.editTextDOB);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        radioGroupDiabetic = findViewById(R.id.radioGroupDiabetic);
        radioGroupDisabled = findViewById(R.id.radioGroupDisabled);
        Button buttonSave = findViewById(R.id.buttonSave);

        // Set up the blood group spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);

        // Set up Date Picker for DOB
        editTextDOB.setOnClickListener(v -> showDatePickerDialog());

        buttonSave.setOnClickListener(v -> saveUserInfo());
    }

    private boolean isUserInfoSaved() {
        return getSharedPreferences("UserInfo", MODE_PRIVATE).contains("Name");
    }

    private void showDatePickerDialog() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Set the selected date in the EditText
            String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear; // Month is 0-based
            editTextDOB.setText(formattedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void saveUserInfo() {
        String name = editTextName.getText().toString().trim();
        String dob = editTextDOB.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        boolean isDiabetic = radioGroupDiabetic.getCheckedRadioButtonId() == R.id.radioYesDiabetic;
        boolean isDisabled = radioGroupDisabled.getCheckedRadioButtonId() == R.id.radioYesDisabled;

        // Validate user input
        if (name.isEmpty() || dob.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save user information to SharedPreferences
        getSharedPreferences("UserInfo", MODE_PRIVATE)
                .edit()
                .putString("Name", name)
                .putString("DOB", dob)
                .putString("BloodGroup", bloodGroup)
                .putBoolean("IsDiabetic", isDiabetic)
                .putBoolean("IsDisabled", isDisabled)
                .apply();

        // Notify that the information is saved
        Toast.makeText(this, "Information Saved!", Toast.LENGTH_SHORT).show();

        // Start SensorService
        Intent intent = new Intent(this, SensorService.class);
        startService(intent);

        // Redirect to MainActivity
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
