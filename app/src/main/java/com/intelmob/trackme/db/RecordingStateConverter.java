
package com.intelmob.trackme.db;

import android.arch.persistence.room.TypeConverter;

public class RecordingStateConverter {

    @TypeConverter
    public static RecordingState fromValue(int value) {
        return RecordingState.values()[value];
    }

    @TypeConverter
    public static int fromState(RecordingState state) {
        return state.ordinal();
    }
}
