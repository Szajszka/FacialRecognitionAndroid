package com.example.mlkittest;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.HashMap;

public class FaceComparatorTest {

    private FaceComparator faceComparator;

    @Before
    public void setUp() {
        faceComparator = new FaceComparator();
    }

    @Test
    public void testCalculateEuclideanDistance_ExactMatch() throws JSONException {
        String decryptedJsonString = "{\"0\": 1.0, \"1\": 2.0, \"2\": 3.0}";
        String readJsonString = "{\"0\": 1.0, \"1\": 2.0, \"2\": 3.0}";

        Double expectedDistance = 0.0; // Euclidean distance between identical points is 0
        Double actualDistance = faceComparator.calculateEuclideanDistance(decryptedJsonString, readJsonString);

        assertEquals(expectedDistance, actualDistance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_DifferentValues() throws JSONException {
        String decryptedJsonString = "{\"0\": 1.0, \"1\": 2.0, \"2\": 3.0}";
        String readJsonString = "{\"0\": 4.0, \"1\": 5.0, \"2\": 6.0}";

        // Expected Euclidean distance calculation: sqrt((1-4)^2 + (2-5)^2 + (3-6)^2)
        Double expectedDistance = Math.sqrt(Math.pow(1-4, 2) + Math.pow(2-5, 2) + Math.pow(3-6, 2));
        Double actualDistance = faceComparator.calculateEuclideanDistance(decryptedJsonString, readJsonString);

        assertEquals(expectedDistance, actualDistance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_EmptyJson() throws JSONException {
        String decryptedJsonString = "{}";
        String readJsonString = "{}";

        Double expectedDistance = 0.0; // No points to compare
        Double actualDistance = faceComparator.calculateEuclideanDistance(decryptedJsonString, readJsonString);

        assertEquals(expectedDistance, actualDistance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_PartiallyMatchingKeys() throws JSONException {
        String decryptedJsonString = "{\"0\": 1.0, \"1\": 2.0}";
        String readJsonString = "{\"0\": 1.0, \"1\": 3.0}";

        // Only key "1" differs: sqrt((2-3)^2)
        Double expectedDistance = Math.sqrt(Math.pow(2-3, 2));
        Double actualDistance = faceComparator.calculateEuclideanDistance(decryptedJsonString, readJsonString);

        assertEquals(expectedDistance, actualDistance, 0.0001);
    }

    @Test
    public void testCalculateEuclideanDistance_InvalidJson() {
        String decryptedJsonString = "{\"0\": 1.0, \"1\": 2.0}";
        String readJsonString = "{invalid_json}"; // intentionally malformed JSON

        // Expect an exception due to invalid JSON format
        assertThrows(JSONException.class, () -> {
            faceComparator.calculateEuclideanDistance(decryptedJsonString, readJsonString);
        });
    }

    @Test
    public void testSumAndSqrtHashMap() {
        HashMap<Integer, Double> distanceHM = new HashMap<>();
        distanceHM.put(1, 4.0);
        distanceHM.put(2, 9.0);
        distanceHM.put(3, 16.0);

        Double expectedSumAndSqrt = Math.sqrt(4.0 + 9.0 + 16.0);
        Double actualSumAndSqrt = faceComparator.sumAndSqrtHashMap(distanceHM);

        assertEquals(expectedSumAndSqrt, actualSumAndSqrt, 0.0001);
    }
}
