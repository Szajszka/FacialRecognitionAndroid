package com.example.mlkittest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private SecurityUtils securityUtils = SecurityUtilsSingleton.getInstance();
    private static final String TAG = "MainActivity";
    private boolean pinResult = false;
    private static final Integer SIMILARITY_THRESHOLD = 200;

    public MainActivity() {
        try {
            securityUtils.generateKey();  // Generate key once during initialization
        } catch (Exception e) {
            Log.e(TAG, "Key generation failed: " + e.getMessage(), e);
        }
    }

    private ActivityResultLauncher<Intent> cameraResultLauncher;
    private ActivityResultLauncher<Intent> pinResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageViewHiddenImg = findViewById(R.id.imageViewHiddenImg);
        imageViewHiddenImg.setVisibility(View.GONE);

        pinResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        pinResult = result.getData().getBooleanExtra("Pin_Result", false);
                    } else {
                        Toast.makeText(this, "Pin not correct", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.hasExtra("Distance_String")) {
                            String pointsDistanceString = data.getStringExtra("Distance_String");
                            if (pointsDistanceString != null) {
                                File encryptedFile = new File(getBaseContext().getFilesDir(), "encryptedFaceJSON.txt");
                                if (encryptedFile.exists()) {
                                    try {
                                        String decryptedJsonString = securityUtils.decryptJsonString(encryptedFile);
                                        FaceComparator faceComparator = new FaceComparator();
                                        Double comparisonValue = faceComparator.calculateEuclideanDistance(decryptedJsonString, pointsDistanceString);
                                        if (comparisonValue <= SIMILARITY_THRESHOLD) {
                                            Toast.makeText(getApplicationContext(), "Faces match", Toast.LENGTH_SHORT).show();
                                            imageViewHiddenImg.setVisibility(View.VISIBLE);
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
                        }
                    }
                }
        );

        Button buttonOpenCamera = findViewById(R.id.buttonOpenCamera);
        buttonOpenCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            intent.putExtra("Pin_result", pinResult);
            cameraResultLauncher.launch(intent);
        });

        Button buttonEnterPin = findViewById(R.id.buttonInputPin);
        buttonEnterPin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PinActivity.class);
            pinResultLauncher.launch(intent);
        });
    }
}