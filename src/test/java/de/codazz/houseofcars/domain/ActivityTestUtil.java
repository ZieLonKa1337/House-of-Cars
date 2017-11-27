package de.codazz.houseofcars.domain;

import java.time.Clock;

/** @author rstumm2s */
public class ActivityTestUtil {
    public static void clock(final Activity activity, final Clock clock) {
        activity.clock = clock;
    }
}
