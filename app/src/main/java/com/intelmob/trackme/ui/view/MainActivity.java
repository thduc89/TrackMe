
package com.intelmob.trackme.ui.view;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.intelmob.trackme.NavigationController;
import com.intelmob.trackme.R;
import com.intelmob.trackme.SubscriberImpl;
import com.intelmob.trackme.adapter.WorkoutSessionAdapter;
import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.WorkoutSession;
import com.intelmob.trackme.ui.viewmodel.WorkoutSessionListViewModel;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvList;
    private WorkoutSessionAdapter mAdapter;
    private WorkoutSessionListViewModel viewModel;

    private Button btnRecord;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvList = findViewById(R.id.main_rvList);
        btnRecord = findViewById(R.id.main_btnRecord);
        tvEmpty = findViewById(R.id.main_tvEmpty);

        mAdapter = new WorkoutSessionAdapter();
        rvList.setAdapter(mAdapter);

        btnRecord.setOnClickListener(this);

        viewModel = ViewModelProviders.of(this).get(WorkoutSessionListViewModel.class);
        viewModel.getWorkoutSessionList().observe(this, workoutSessions -> {
            tvEmpty.setVisibility(
                    workoutSessions == null || workoutSessions.size() == 0 ? View.VISIBLE
                            : View.GONE);
            mAdapter.setItems(workoutSessions);
        });

        Flowable.just(1)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    AppDatabase appDatabase = AppDatabase.getDatabase(this);
                    WorkoutSession workoutSession = appDatabase.workoutModel()
                            .getRecordingWorkoutSessionSync();
                    if (workoutSession != null) {
                        return true;
                    }
                    return false;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean isRecording) {
                        if (isRecording) {
                            btnRecord.performClick();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_btnRecord:
                NavigationController.showWorkoutPage(this);
                break;
        }
    }
}
