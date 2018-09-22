package com.intelmob.trackme.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.intelmob.trackme.R;
import com.intelmob.trackme.adapter.viewholder.ItemWorkoutSessionVH;
import com.intelmob.trackme.db.WorkoutSession;
import com.intelmob.trackme.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSessionAdapter extends RecyclerView.Adapter<ItemWorkoutSessionVH> {

    private List<WorkoutSession> items = new ArrayList<>();

    @NonNull
    @Override
    public ItemWorkoutSessionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_session, parent, false);
        return new ItemWorkoutSessionVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemWorkoutSessionVH holder, int position) {
        WorkoutSession session = items.get(position);

        Context context = holder.itemView.getContext();
        holder.tvDistance.setText(String.format(context.getString(R.string.format_distance), session.distance));
        holder.tvAvgSpeed.setText(String.format(context.getString(R.string.format_avgSpeed), session.avgSpeed));
        holder.tvDuration.setText(Utils.formatDuration(session.duration));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<WorkoutSession> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }
}
