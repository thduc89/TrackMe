package com.intelmob.trackme;

import android.app.Service;
import android.arch.lifecycle.LifecycleService;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class WorkoutService extends LifecycleService {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
