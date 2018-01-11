package de.codazz.houseofcars.template;

import java.time.format.TextStyle;
import java.util.Locale;

/** @author rstumm2s */
public class ZonedDateTime {
    public final java.time.ZonedDateTime value;

    public ZonedDateTime(final java.time.ZonedDateTime value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return toString(value);
    }

    public String toString(final java.time.ZonedDateTime duration) {
        return String.format("%d %s %02d:%02d",
            duration.getDayOfMonth(),
            duration.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            duration.getHour(),
            duration.getMinute()
        );
    }
}
