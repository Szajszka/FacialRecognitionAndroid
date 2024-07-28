package com.example.mlkittest;

import android.content.Context;

public class SecurityUtilsSingleton {
    private static SecurityUtils instance;

    public static SecurityUtils getInstance() {
        if (instance == null) {
            instance = new SecurityUtils();
        }
        return instance;
    }
}
