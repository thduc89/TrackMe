package com.intelmob.trackme;

import android.app.Activity;
import android.content.Intent;

public class NavigationController {

    public static void showWorkoutPage(Activity callerActivity) {
        Intent intent = new Intent(callerActivity, WorkoutActivity.class);
        callerActivity.startActivity(intent);
        callerActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
