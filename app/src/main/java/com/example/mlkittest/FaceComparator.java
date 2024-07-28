package com.example.mlkittest;

import android.graphics.PointF;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FaceComparator {

    public Double calculateEuclideanDistance(String decryptedJsonString, String readJsonString) throws JSONException {
        JSONObject decryptedJson = new JSONObject(decryptedJsonString);
        JSONObject readJson = new JSONObject(readJsonString);
        HashMap<Integer, Double> distanceHM = new HashMap<>();

        Iterator<String> keys = readJson.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            try {
                Integer intKey = Integer.parseInt(key);
                Double decryptedValue = decryptedJson.getDouble(key);
                Double readValue = readJson.getDouble(key);
                distanceHM.put(intKey, Math.pow(decryptedValue - readValue, 2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return sumAndSqrtHashMap(distanceHM);
    }
    public Double sumAndSqrtHashMap(HashMap<Integer, Double> distanceHM) {
        Double sum = 0.0;
        for (Map.Entry<Integer, Double> e : distanceHM.entrySet()) {
            sum += e.getValue();
        }
        return Math.sqrt(sum);
    }
}
