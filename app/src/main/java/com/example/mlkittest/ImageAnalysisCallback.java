package com.example.mlkittest;

public interface ImageAnalysisCallback {
    void onSuccess(String pointsDistanceString);
    void onFailure(Exception e);
}