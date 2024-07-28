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

public class MainActivity extends AppCompatActivity {

    private SecurityUtils securityUtils = SecurityUtilsSingleton.getInstance();
    private static final String TAG = "MainActivity";
    private static final Integer SIMILARITY_THRESHOLD = 200;

    public MainActivity() {
        try {
            securityUtils.generateKey();  // Generate key once during initialization
        } catch (Exception e) {
            Log.e(TAG, "Key generation failed: " + e.getMessage(), e);
        }
    }

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == 1) {
                        Intent data = result.getData();
                        if (data != null) {
                            String pointsDistanceString = data.getStringExtra("Distance_String");
                            if (pointsDistanceString != null) {
                                File encryptedFile = new File(getBaseContext().getFilesDir(), "encryptedFaceJSON.txt");
                                if (encryptedFile.exists()) {
                                    try {
                                        String decryptedJsonString = decryptJSONData(encryptedFile);
                                        FaceComparator faceComparator = new FaceComparator();
                                        Double comparisonValue = faceComparator.calculateEuclideanDistance(decryptedJsonString, pointsDistanceString);
                                        if (comparisonValue <= SIMILARITY_THRESHOLD) {
                                            Toast.makeText(getApplicationContext(), "Faces match", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Faces don't match", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    try {
                                        securityUtils.encryptJsonString(pointsDistanceString, encryptedFile);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Intent data is null");
                        }
                    } else {
                        Log.d(TAG, "Exited with code: " + result.getResultCode());
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
        return securityUtils.decryptJsonString(encryptedJSONFile);
    }
}