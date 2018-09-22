package com.intelmob.trackme.ui.view;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.intelmob.trackme.R;
import com.intelmob.trackme.WorkoutService;
import com.intelmob.trackme.util.Utils;
import com.intelmob.trackme.ui.viewmodel.WorkoutSessionViewModel;

public class WorkoutActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnPause;
    private Button btnResume;
    private Button btnStop;
    private Group groupResumeStop;

    private TextView tvDistance;
    private TextView tvAvgSpeed;
    private TextView tvDuration;

    private WorkoutSessionViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        btnPause = findViewById(R.id.workout_btnPause);
        btnResume = findViewById(R.id.workout_btnResume);
        btnStop = findViewById(R.id.workout_btnStop);
        groupResumeStop = findViewById(R.id.groupResumeStop);

        tvDistance = findViewById(R.id.workout_info_tvDistance);
        tvAvgSpeed = findViewById(R.id.workout_info_tvAvgSpeed);
        tvDuration = findViewById(R.id.workout_info_tvDuration);

        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        viewModel = ViewModelProviders.of(this).get(WorkoutSessionViewModel.class);
        viewModel.getRecordingWorkoutSession().observe(this, session -> {
            if (session == null) {
                return;
            }

            tvDistance.setText(String.format(getString(R.string.format_distance), session.distance));
            tvAvgSpeed.setText(String.format(getString(R.string.format_avgSpeed), session.avgSpeed));
            tvDuration.setText(Utils.formatDuration(session.duration));
        });

        WorkoutService.startNewRecording(this);
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
                WorkoutService.stopRecording(this);
                finish();
                break;
        }
    }

    private void toggleGroupResumeStop(boolean show) {
        groupResumeStop.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPause.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
