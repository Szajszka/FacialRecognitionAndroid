package com.example.mlkittest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Result codes: 1 - OK, 2 - permissions not granted, 3 - img capture failed, 4 - camera binding failed
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == 1) {
                        Intent data = result.getData();
                        if (data != null) {
                            HashMap<Integer, Double> distanceHashMap = (HashMap<Integer, Double>) data.getSerializableExtra("Distance_HashMap");
                            if (distanceHashMap != null) {
                                Log.d(TAG, "Received Distance HashMap: " + distanceHashMap.toString());
                                // Store the distance hashmap as needed, e.g., in memory, SharedPreferences, or a local database
                            }

                            File encryptedJSONFile = (File) data.getSerializableExtra("Encrypted_JSON_file");
                            if (encryptedJSONFile != null) {
                                try {
                                    Toast.makeText(getBaseContext(), decryptJSONData(encryptedJSONFile), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            Log.e(TAG, "Intent data is null");
                        }
                    } else {
                        Log.d(TAG, "Exited with code: " + result.getResultCode());
                        // Handle the failure or cancellation case here
                    }
                }
        );


        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            activityResultLauncher.launch(intent);
        });
    }

    public String decryptJSONData(File encryptedJSONFile) throws Exception {
        SecurityUtils securityUtils = SecurityUtilsSingleton.getInstance();
        return securityUtils.decryptJsonString(encryptedJSONFile);
    }
}