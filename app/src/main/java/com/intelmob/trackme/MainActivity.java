package com.intelmob.trackme;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.intelmob.trackme.adapter.WorkoutSessionAdapter;
import com.intelmob.trackme.viewmodel.WorkoutSessionListViewModel;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rvList;
    private WorkoutSessionAdapter mAdapter;
    private WorkoutSessionListViewModel viewModel;

    private Button btnRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvList = findViewById(R.id.main_rvList);
        btnRecord = findViewById(R.id.main_btnRecord);

        mAdapter = new WorkoutSessionAdapter();
        rvList.setAdapter(mAdapter);
        rvList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btnRecord.setOnClickListener(this);

        viewModel = ViewModelProviders.of(this).get(WorkoutSessionListViewModel.class);
        viewModel.getWorkoutSessionList().observe(this, workoutSessions -> mAdapter.addItems(workoutSessions));
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
