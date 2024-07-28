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

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.provider.MediaStore;
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
    private ImageCapture imageCapture = null;
    private ExecutorService cameraExecutor;
    private MLKitUtils mlKitUtils;
    private static final String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO};
    private ActivityResultLauncher<String[]> activityResultLauncher =
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
                    Toast.makeText(getBaseContext(),
                            "Permission request denied",
                            Toast.LENGTH_SHORT).show();
                } else {
                    startCamera();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        mlKitUtils = new MLKitUtils(this);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        viewBinding.backButton.setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
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
                        returnResult(3);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        String msg = "Photo capture succeeded: " + savedUri;
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        if (savedUri != null) {
                            mlKitUtils.analyzeImage(CameraActivity.this, savedUri, new MLKitUtils.AnalyzeImageCallback() {
                                @Override
                                public void onSuccess(File encryptedFile) {
                                    Log.d(TAG, "Image analysis succeeded, encrypted file: " + encryptedFile.getAbsolutePath());
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("Encrypted_JSON_file", encryptedFile);
                                    setResult(1, returnIntent);
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Image analysis failed: " + e.getMessage(), e);
                                    returnResult(3);
                                }
                            });
                        } else {
                            Log.e(TAG, "Saved URI is null");
                            returnResult(3);
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
                Preview preview = new Preview.Builder()
                        .build();
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
                returnResult(4);
            }

        }, ContextCompat.getMainExecutor(this));
    }



    private void requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                returnResult(2);
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

    public void returnResult(int resultCode) {
        Intent returnIntent = new Intent();
        setResult(resultCode, returnIntent);
        finish();
    }
}

