package com.example.mlkittest;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.core.content.ContextCompat;
import com.example.mlkittest.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import android.util.Log;

import androidx.camera.core.ImageCaptureException;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding viewBinding;
    ImageCapture imageCapture = null;
    ExecutorService cameraExecutor;
    MLKitUtils mlKitUtils;
    private static final String TAG = "FaceFinder";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO};
    ActivityResultLauncher<String[]> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // Handle Permission granted/rejected
                boolean permissionGranted = true;
                for (String key : permissions.keySet()) {
                    if (key != null && permissions.get(key) != null && Boolean.FALSE.equals(permissions.get(key))) {
                        if (isRequiredPermission(key)) {
                            permissionGranted = false;
                            break;
                        }
                    }
                }
                if (!permissionGranted) {
                    Toast.makeText(getBaseContext(), "Permission request denied", Toast.LENGTH_SHORT).show();
                } else {
                    startCamera();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        mlKitUtils = new MLKitUtils();  // Initialize mlKitUtils

        // Set up the listeners for take photo and video capture buttons
        viewBinding.buttonCompareFace.setOnClickListener(v -> takePhoto());
        viewBinding.buttonSaveFace.setOnClickListener(v -> takePhoto());
        viewBinding.buttonSaveFace.setVisibility(View.GONE);
        viewBinding.backButton.setOnClickListener(v -> finish());

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        Intent intent = getIntent();
        boolean pinResult = intent.getBooleanExtra("Pin_result", false);
        if (pinResult) {
            viewBinding.buttonSaveFace.setVisibility(View.VISIBLE);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    void takePhoto() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                        returnResultCanceled();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        Log.d(TAG, "Photo capture succeeded");

                        if (savedUri != null) {
                            mlKitUtils.analyzeImage(CameraActivity.this, savedUri, new ImageAnalysisCallback() {
                                @Override
                                public void onSuccess(String pointsDistanceString) {
                                    Log.d(TAG, "Image analysis succeeded");
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("Distance_String", pointsDistanceString);
                                    setResult(RESULT_OK, returnIntent);
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Image analysis failed", e);
                                    returnResultCanceled();
                                }
                            });
                        } else {
                            Log.e(TAG, "Saved image is null");
                            returnResultCanceled();
                        }
                    }
                }
        );
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Select front camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        new LifecycleOwner() {
                            @NonNull
                            @Override
                            public Lifecycle getLifecycle() {
                                return CameraActivity.this.getLifecycle();
                            }
                        },
                        cameraSelector,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
                returnResultCanceled();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    void requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }

    boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private boolean isRequiredPermission(String permission) {
        for (String requiredPermission : REQUIRED_PERMISSIONS) {
            if (requiredPermission.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    public void returnResultCanceled() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }
}
