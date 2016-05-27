package com.example.jacob.androidvirtualassist;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by Jacob on 20/04/2016.
 * Initialize Firebase with the application context. This must happen before the client is used.
 */
public class AndroidVirtualAssistantApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
