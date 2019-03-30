package com.example.baibu.running;

import android.app.Application;
import android.content.Context;

import com.baidu.mapapi.SDKInitializer;

public class MyAppLication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

    }
    public static Context getContext(){
        return context;
    }

}
