package com.example.mlkittest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class PinActivity extends AppCompatActivity {
    private SecurityUtils securityUtils = SecurityUtilsSingleton.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        EditText editTextPin = findViewById(R.id.editTextNumberPin);
        EditText editTextPinConf = findViewById(R.id.editTextNumberPinConfirm);
        Button buttonConfirm = findViewById(R.id.buttonConfirm);

        File pinFile = new File(getFilesDir(), "encryptedPin.txt");
        if (pinFile.exists()) editTextPinConf.setEnabled(false);


        buttonConfirm.setOnClickListener(v -> {
            try {
                String pin = editTextPin.getText().toString();
                if (pinFile.exists()) {
                    String decryptedPin = securityUtils.decryptJsonString(pinFile);
                    if (decryptedPin.equals(pin)) {
                        // PIN is correct

                    } else {
                        Toast.makeText(getApplicationContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String pinConf = editTextPinConf.getText().toString();
                    if (pin.equals(pinConf)) {
                        securityUtils.encryptJsonString(pin, pinFile);

                    } else {
                        Toast.makeText(getApplicationContext(), "PINs do not match", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error setting PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }
}