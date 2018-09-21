package com.intelmob.trackme.util;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatterBuilder;

public class Utils {

    public static String formatDuration(int timeInSeconds) {
        Duration duration = new Duration(timeInSeconds * 1000);
        return new PeriodFormatterBuilder()
                .appendHours()
                .printZeroRarelyFirst()
                .appendSeparatorIfFieldsBefore(":")
                .minimumPrintedDigits(2)
                .printZeroAlways()
                .appendMinutes()
                .appendSuffix(":")
                .printZeroAlways()
                .minimumPrintedDigits(2)
                .appendSeconds()
                .toFormatter().print(duration.toPeriod());
    }
}
