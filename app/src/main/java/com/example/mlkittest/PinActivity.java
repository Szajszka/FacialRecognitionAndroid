package com.example.mlkittest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
        if (pinFile.exists()) editTextPinConf.setVisibility(View.GONE);

        buttonConfirm.setOnClickListener(v -> {
            try {
                String pin = editTextPin.getText().toString();
                if (pinFile.exists()) {
                    String decryptedPin = securityUtils.decryptJsonString(pinFile);
                    if (decryptedPin.equals(pin)) {
                        returnResult(true);  // Return true if PIN is correct
                    } else {
                        Toast.makeText(PinActivity.this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        returnResult(false);  // Return false if PIN is incorrect
                    }
                } else {
                    String pinConf = editTextPinConf.getText().toString();
                    if (pin.equals(pinConf)) {
                        securityUtils.encryptJsonString(pin, pinFile);
                        returnResult(true);  // Return true if PIN is set successfully
                    } else {
                        Toast.makeText(PinActivity.this, "PINs do not match", Toast.LENGTH_SHORT).show();
                        returnResult(false);  // Return false if PINs do not match
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PinActivity.this, "Error setting PIN", Toast.LENGTH_SHORT).show();
                returnResult(false);  // Return false in case of an error
            }
        });
    }

    private void returnResult(boolean isPinCorrect) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("Pin_Result", isPinCorrect);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}