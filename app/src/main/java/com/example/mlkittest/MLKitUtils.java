package com.example.mlkittest;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.gson.Gson;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MLKitUtils {

    private static final String TAG = "MLKitUtils";
    private SecurityUtils securityUtils;

    public interface AnalyzeImageCallback {
        void onSuccess(File encryptedFile);
        void onFailure(Exception e);
    }

    public MLKitUtils(Context context) {
        securityUtils = new SecurityUtils();
        try {
            securityUtils.generateKey();  // Generate key once during initialization
        } catch (Exception e) {
            Log.e(TAG, "Key generation failed: " + e.getMessage(), e);
        }
    }

    FirebaseVisionFaceDetectorOptions highAccOpt = new FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build();

    public void analyzeImage(Context context, Uri imageUri, AnalyzeImageCallback callback) {
        FirebaseApp.initializeApp(context);
        Log.d(TAG, "Starting image analysis for URI: " + imageUri);
        try {
            FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context, imageUri);
            FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccOpt);

            detector.detectInImage(image)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            FirebaseVisionFace face = faces.get(0);
                            HashMap<Integer, PointF> pointsHM = getLandmarkPointsHMap(context, face);
                            HashMap<Integer, Double> pointsDistanceHM = new HashMap<>();
                            for (Map.Entry<Integer, PointF> e : pointsHM.entrySet()) {
                                PointF current = e.getValue();
                                pointsDistanceHM.put(e.getKey(), calculateMidpointDistance(current.x, current.y, pointsHM));
                            }
                            pointsDistanceHM.remove(6);
                            String pointsDistanceString = new Gson().toJson(pointsDistanceHM);

                            Log.i(TAG, "Hashmap: " + pointsDistanceString);

                            try {
                                File encryptedFile = new File(context.getFilesDir(), "encryptedFaceJSON.txt");
                                securityUtils.encryptJsonString(pointsDistanceString, encryptedFile);
                                Log.i(TAG, "Encrypted JSON saved to: " + encryptedFile.getAbsolutePath());
                                callback.onSuccess(encryptedFile);
                            } catch (Exception e) {
                                Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
                                callback.onFailure(e);
                            }
                        } else {
                            callback.onFailure(new Exception("No faces detected"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                        callback.onFailure(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Image analysis failed: " + e.getMessage(), e);
            callback.onFailure(e);
        }
    }

    private HashMap<Integer, PointF> getLandmarkPointsHMap(Context context, FirebaseVisionFace face) {
        HashMap<Integer, PointF> pointsHM = new HashMap<>();

        int[] landmarkTypes = { // Landmarks loaded in an order based on id not to waste log(n) for sorting
                FirebaseVisionFaceLandmark.MOUTH_BOTTOM, //id 0
                FirebaseVisionFaceLandmark.LEFT_CHEEK, //id 1
                FirebaseVisionFaceLandmark.LEFT_EAR, //id 3
                FirebaseVisionFaceLandmark.LEFT_EYE, //id 4
                FirebaseVisionFaceLandmark.MOUTH_LEFT, //id 5
                FirebaseVisionFaceLandmark.NOSE_BASE, // Midpoint of the nose, id 6
                FirebaseVisionFaceLandmark.RIGHT_CHEEK, //id 7
                FirebaseVisionFaceLandmark.RIGHT_EAR, //id 9
                FirebaseVisionFaceLandmark.RIGHT_EYE,  //id 10
                FirebaseVisionFaceLandmark.MOUTH_RIGHT  //id 11
        };

        for (int landmarkType : landmarkTypes) {
            FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkType);
            if (landmark != null) {
                pointsHM.put(landmarkType, new PointF(landmark.getPosition().getX(), landmark.getPosition().getY()));
            }
        }
        return pointsHM;
    }

    private Double calculateMidpointDistance(float x, float y, HashMap<Integer, PointF> pointsHM) {
        PointF noseBasePoint = new PointF(Objects.requireNonNull(pointsHM.get(6)));
        return Math.sqrt(Math.pow((noseBasePoint.x - x), 2) + Math.pow((noseBasePoint.y - y), 2));
    }
}

