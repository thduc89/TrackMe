
package com.intelmob.trackme;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.RecordingState;
import com.intelmob.trackme.db.model.TravelPoint;
import com.intelmob.trackme.db.model.WorkoutSession;
import com.intelmob.trackme.ui.view.WorkoutActivity;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WorkoutService extends LifecycleService {

    private static final int HOUR_IN_MILLIS = 60 * 60 * 1000;

    private static final String ACTION_START_NEW_RECORDING = "action_start_new_recording";
    private static final String ACTION_PAUSE_RECORDING = "action_pause_recording";
    private static final String ACTION_RESUME_RECORDING = "action_resume_recording";
    private static final String ACTION_STOP_RECORDING = "action_stop_recording";
    private static final String EXTRA_SAVE_PROGRESS = "extra_save_progress";

    /**
     * Testing purpose only
     */
    private static final String ACTION_ADD_LOCATION = "action_add_location";
    private static final String EXTRA_LOCATION = "extra_location";

    public static void startNewRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_START_NEW_RECORDING);
        context.startService(intent);
    }

    public static void pauseRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_PAUSE_RECORDING);
        context.startService(intent);
    }

    public static void resumeRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_RESUME_RECORDING);
        context.startService(intent);
    }

    public static void stopRecording(Context context, boolean saveProgress) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        intent.putExtra(EXTRA_SAVE_PROGRESS, saveProgress);
        context.startService(intent);
    }

    public static void addLocation(Context context, Location location) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_ADD_LOCATION);
        intent.putExtra(EXTRA_LOCATION, new Gson().toJson(location));
        context.startService(intent);
    }

    private AppDatabase appDatabase;

    private FusedLocationProviderClient mLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult == null) {
                return;
            }

            Location location = locationResult.getLocations().get(0);
            addNewTravelPointFromLocation(location);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        appDatabase = AppDatabase.getDatabase(this.getApplication());

        mLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setSmallestDisplacement(10);
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopDurationCounter();
        stopLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_START_NEW_RECORDING:
                    internalStartNewRecording();
                    showNotification();
                    break;
                case ACTION_PAUSE_RECORDING:
                    internalPauseRecording();
                    break;
                case ACTION_RESUME_RECORDING:
                    internalResumeRecording();
                    break;
                case ACTION_STOP_RECORDING:
                    boolean saveProgress = intent.getBooleanExtra(EXTRA_SAVE_PROGRESS, false);
                    internalStopRecording(saveProgress);
                    break;
                case ACTION_ADD_LOCATION:
                    if (BuildConfig.DEBUG) {
                        Location location = new Gson().fromJson(
                                intent.getStringExtra(EXTRA_LOCATION), Location.class);
                        addNewTravelPointFromLocation(location);
                    }
                    break;
            }
        }

        return START_STICKY;
    }

    private void addNewTravelPointFromLocation(Location location) {
        double currentLat = location.getLatitude();
        double currentLon = location.getLongitude();
        long timestamp = location.getTime();

        getRecordingWorkoutSession()
                .map(workoutSession -> {
                    if (workoutSession.travelPoints == null) {
                        workoutSession.travelPoints = new ArrayList<>();
                    }

                    TravelPoint newPoint = new TravelPoint(new LatLng(currentLat, currentLon),
                            timestamp, 0);

                    int size = workoutSession.travelPoints.size();
                    if (size > 0) {
                        TravelPoint lastPoint = workoutSession.travelPoints.get(size - 1);
                        float[] results = new float[3];
                        Location.distanceBetween(currentLat, currentLon,
                                lastPoint.latLng.latitude, lastPoint.latLng.longitude,
                                results);
                        float distance = results[0];
                        float distanceKM = distance / 1000;
                        workoutSession.distance += distanceKM;

                        long deltaTimeInMillis = newPoint.timestamp - lastPoint.timestamp;
                        float deltaTimeInHour = (float) deltaTimeInMillis / HOUR_IN_MILLIS;

                        newPoint.speedKPH = distanceKM / deltaTimeInHour;
                    }

                    workoutSession.travelPoints.add(newPoint);
                    appDatabase.workoutModel().updateWorkoutSession(workoutSession);

                    return true;
                }).subscribe(new SubscriberImpl<>());
    }

    private void internalStartNewRecording() {
        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    recordingSession.recordingState = RecordingState.RECORDING;
                    recordingSession.distance = 0;
                    recordingSession.avgSpeed = 0;
                    recordingSession.duration = 0;
                    recordingSession.travelPoints = null;
                    appDatabase.workoutModel().updateWorkoutSession(recordingSession);

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        startDurationCounter();
                    }
                });
    }

    private void internalPauseRecording() {
        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    recordingSession.recordingState = RecordingState.PAUSED;
                    appDatabase.workoutModel().updateWorkoutSession(recordingSession);

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        stopLocationUpdates();
                        stopDurationCounter();
                        stopForeground(true);
                    }
                });
    }

    private void internalResumeRecording() {
        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    recordingSession.recordingState = RecordingState.RECORDING;
                    appDatabase.workoutModel().updateWorkoutSession(recordingSession);

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        startLocationUpdates();
                        startDurationCounter();
                        showNotification();
                    }
                });
    }

    private void internalStopRecording(boolean saveProgress) {
        stopDurationCounter();

        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    if (saveProgress) {
                        recordingSession.recordingState = RecordingState.NONE;
                        if (recordingSession.travelPoints != null
                                && recordingSession.travelPoints.size() > 0) {

                            int count = 0;
                            float speed = 0;
                            for (TravelPoint point : recordingSession.travelPoints) {
                                if (point.speedKPH > 0) {
                                    count++;
                                    speed += point.speedKPH;
                                }
                            }
                            if (count > 0)
                                recordingSession.avgSpeed = speed / count;
                        }
                        appDatabase.workoutModel().updateWorkoutSession(recordingSession);
                    } else {
                        appDatabase.workoutModel().deleteWorkoutSession(recordingSession);
                    }

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        stopLocationUpdates();
                        stopForeground(true);
                        stopSelf();
                    }
                });
    }

    private Flowable<WorkoutSession> getRecordingWorkoutSession() {
        return Flowable.just(appDatabase)
                .subscribeOn(Schedulers.io())
                .map(db -> {
                    WorkoutSession recordingSession = db.workoutModel()
                            .getRecordingWorkoutSessionSync();
                    if (recordingSession == null) {
                        recordingSession = new WorkoutSession();
                        recordingSession.dateCreated = System.currentTimeMillis();
                        recordingSession.recordingState = RecordingState.RECORDING;
                        appDatabase.workoutModel().addWorkoutSession(recordingSession);

                        recordingSession = appDatabase.workoutModel()
                                .getRecordingWorkoutSessionSync();
                    }
                    return recordingSession;
                });
    }

    private Disposable disposable;

    public void startDurationCounter() {
        stopDurationCounter();

        disposable = Flowable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map(timeInterval -> appDatabase.workoutModel().getRecordingWorkoutSessionSync())
                .map(recordingWorkoutSession -> {
                    recordingWorkoutSession.duration++;
                    appDatabase.workoutModel().updateWorkoutSession(recordingWorkoutSession);
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new SubscriberImpl<>());
    }

    public void stopDurationCounter() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void showNotification() {
        String title = getString(R.string.app_name);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "1")
                .setContentTitle(title)
                .setTicker(title)
                .setContentText("Running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(12345, notification);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mLocationProviderClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        if (mLocationProviderClient != null)
            mLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
