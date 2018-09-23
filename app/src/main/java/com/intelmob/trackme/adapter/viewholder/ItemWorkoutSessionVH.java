package com.intelmob.trackme.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.intelmob.trackme.R;

public class ItemWorkoutSessionVH extends RecyclerView.ViewHolder {

    public ImageView ivMap;
    public TextView tvDistance;
    public TextView tvAvgSpeed;
    public TextView tvDuration;

    public ItemWorkoutSessionVH(View itemView) {
        super(itemView);

        ivMap = itemView.findViewById(R.id.item_workout_session_ivMap);
        tvDistance = itemView.findViewById(R.id.workout_info_tvDistance);
        tvAvgSpeed = itemView.findViewById(R.id.workout_info_tvSpeed);
        tvDuration = itemView.findViewById(R.id.workout_info_tvDuration);
    }
}
