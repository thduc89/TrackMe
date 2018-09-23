
package com.intelmob.trackme.ui.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.intelmob.trackme.R;
import com.intelmob.trackme.SubscriberImpl;
import com.intelmob.trackme.WorkoutService;
import com.intelmob.trackme.db.WorkoutSession;
import com.intelmob.trackme.ui.viewmodel.WorkoutSessionViewModel;
import com.intelmob.trackme.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class WorkoutActivity extends AppCompatActivity
        implements View.OnClickListener, OnMapReadyCallback {

    private static final int LOCATION_RC = 999;
    private static final int DEFAULT_ZOOM_LEVEL = 14;
    private static final int DEFAULT_LINE_WIDTH = 12;
    private static final int DEFAULT_BOUNDS_PADDING = 50;

    private ImageButton btnPause;
    private ImageButton btnResume;
    private ImageButton btnStop;
    private Group groupResumeStop;

    private TextView tvDistance;
    private TextView tvAvgSpeed;
    private TextView tvDuration;

    private View loadingView;

    private GoogleMap mMap;

    private int mWorkoutSessionId = -1;
    private boolean startMarkerAdded;
    private List<LatLng> mTravelRoutes;

    private WorkoutSessionViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.workout_mapView);
        mapFragment.getMapAsync(this);

        btnPause = findViewById(R.id.workout_btnPause);
        btnResume = findViewById(R.id.workout_btnResume);
        btnStop = findViewById(R.id.workout_btnStop);
        groupResumeStop = findViewById(R.id.groupResumeStop);

        tvDistance = findViewById(R.id.workout_info_tvDistance);
        tvAvgSpeed = findViewById(R.id.workout_info_tvAvgSpeed);
        tvDuration = findViewById(R.id.workout_info_tvDuration);

        loadingView = findViewById(R.id.loadingView);

        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mViewModel = ViewModelProviders.of(this)
                .get(WorkoutSessionViewModel.class);
        mViewModel.getRecordingWorkoutSession().observe(this, session -> {
            if (session == null) {
                return;
            }

            mWorkoutSessionId = session.id;

            tvDistance.setText(String.format(
                    getString(R.string.format_distance), session.distance));
            tvAvgSpeed.setText(String.format(
                    getString(R.string.format_avgSpeed), session.avgSpeed));
            tvDuration.setText(Utils.formatDuration(session.duration));

            if (session.travelRoutes != null && session.travelRoutes.size() > 0) {
                if (mTravelRoutes == null) {
                    mTravelRoutes = new ArrayList<>();
                    mTravelRoutes.addAll(session.travelRoutes);
                }

                if (!startMarkerAdded) {
                    startMarkerAdded = true;

                    LatLng firstPoint = mTravelRoutes.get(0);
                    addMarker(firstPoint.latitude, firstPoint.longitude);
                    moveCameraTo(firstPoint.latitude, firstPoint.longitude, DEFAULT_ZOOM_LEVEL,
                            true, null);
                }

                if (mTravelRoutes.size() < session.travelRoutes.size()) {
                    int fromIndex = mTravelRoutes.size();
                    int toIndex = session.travelRoutes.size();

                    mTravelRoutes.addAll(session.travelRoutes.subList(fromIndex, toIndex));

                    for (int i = fromIndex; i < toIndex - 1; i++) {
                        LatLng startPoint = mTravelRoutes.get(i);
                        LatLng endPoint = mTravelRoutes.get(i + 1);
                        drawRoute(startPoint.latitude, startPoint.longitude, endPoint.latitude,
                                endPoint.longitude);
                    }

                    if (toIndex - fromIndex == 1) {
                        moveCameraToBounds(mTravelRoutes.subList(fromIndex, toIndex), true, null);
                    }
                }
            }
        });

        checkAndRequestLocationPermissionIfNeed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.workout_btnPause:
                WorkoutService.pauseRecording(this);
                toggleGroupResumeStop(true);
                break;
            case R.id.workout_btnResume:
                WorkoutService.resumeRecording(this);
                toggleGroupResumeStop(false);
                break;
            case R.id.workout_btnStop:
                Flowable.just(1)
                        .subscribeOn(Schedulers.io())
                        .map(integer -> mViewModel.getRecordingWorkoutSessionSync())
                        .filter(workoutSession -> workoutSession != null
                                && workoutSession.travelRoutes != null
                                && workoutSession.travelRoutes.size() > 0)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SubscriberImpl<WorkoutSession>() {
                            @Override
                            public void onNext(WorkoutSession workoutSession) {
                                if (workoutSession.travelRoutes.size() > 1) {
                                    moveCameraToBounds(workoutSession.travelRoutes, false,
                                            new GoogleMap.CancelableCallback() {
                                                @Override
                                                public void onFinish() {
                                                    takeMapSnapshotAndStopRecording();
                                                }

                                                @Override
                                                public void onCancel() {

                                                }
                                            });

                                } else {
                                    LatLng point = workoutSession.travelRoutes.get(0);
                                    moveCameraTo(point.latitude, point.longitude,
                                            DEFAULT_ZOOM_LEVEL, false,
                                            new GoogleMap.CancelableCallback() {
                                                @Override
                                                public void onFinish() {
                                                    takeMapSnapshotAndStopRecording();
                                                }

                                                @Override
                                                public void onCancel() {

                                                }
                                            });
                                }
                            }
                        });
                break;
        }
    }

    private void takeMapSnapshotAndStopRecording() {
        loadingView.setVisibility(View.VISIBLE);

        mMap.snapshot(bitmap -> {
            File sd = getFilesDir();
            File dest = new File(sd, String.valueOf(mWorkoutSessionId));

            try {
                FileOutputStream out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("Error", e.getLocalizedMessage(), e);
                Toast.makeText(this, R.string.take_snapshot_fail, Toast.LENGTH_SHORT).show();
            }

            loadingView.setVisibility(View.GONE);

            WorkoutService.stopRecording(this);
            finish();
        });
    }

    private void toggleGroupResumeStop(boolean show) {
        groupResumeStop.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPause.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (isLocationPermissionGranted()) {
            onMapReadyAndLocationPermissionGranted();
        }
    }

    private void onLocationPermissionGranted() {
        if (mMap != null) {
            onMapReadyAndLocationPermissionGranted();
        }
    }

    @SuppressLint("MissingPermission")
    private void onMapReadyAndLocationPermissionGranted() {
        mMap.setMyLocationEnabled(true);

        WorkoutService.startLocationUpdates(this);
        WorkoutService.startNewRecording(this);
    }

    private void addMarker(double lat, double lon) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));
    }

    private void moveCameraTo(double lat, double lon, int zoomLevel, boolean animate,
            GoogleMap.CancelableCallback cancelableCallback) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                        .target(new LatLng(lat, lon))
                        .zoom(zoomLevel)
                        .build());
        if (animate) {
            mMap.animateCamera(cameraUpdate, cancelableCallback);
        } else {
            mMap.moveCamera(cameraUpdate);
            if (cancelableCallback != null) {
                cancelableCallback.onFinish();
            }
        }
    }

    private void moveCameraToBounds(List<LatLng> latLngs, boolean animate,
            GoogleMap.CancelableCallback cancelableCallback) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,
                DEFAULT_BOUNDS_PADDING);

        if (animate) {
            mMap.animateCamera(cameraUpdate, DEFAULT_ZOOM_LEVEL, cancelableCallback);
        } else {
            mMap.moveCamera(cameraUpdate);
            if (cancelableCallback != null) {
                cancelableCallback.onFinish();
            }
        }
    }

    private void drawRoute(double startLat, double startLon, double endLat, double endLon) {
        PolylineOptions options = new PolylineOptions();
        options.add(new LatLng(startLat, startLon), new LatLng(endLat, endLon));
        options.startCap(new RoundCap());
        options.endCap(new RoundCap());
        options.width(DEFAULT_LINE_WIDTH);
        options.color(getResources().getColor(R.color.colorAccent));
        options.geodesic(true);

        mMap.addPolyline(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_RC) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted();
            } else {
                showLocationPermissionDescriptionDialog();
            }
        }
    }

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                LOCATION_RC);
    }

    private void checkAndRequestLocationPermissionIfNeed() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                showLocationPermissionDescriptionDialog();
            } else {
                askForLocationPermission();
            }
        } else {
            onLocationPermissionGranted();
        }
    }

    private void showLocationPermissionDescriptionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notice)
                .setMessage(R.string.location_permission_description)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> askForLocationPermission())
                .show();
    }
}
