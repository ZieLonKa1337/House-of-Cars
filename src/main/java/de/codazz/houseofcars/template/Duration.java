package de.codazz.houseofcars.template;

import java.util.Objects;

/** @author rstumm2s */
public class Duration {
    public final java.time.Duration value;

    public Duration(final java.time.Duration value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return toString(value);
    }

    public static String toString(final java.time.Duration duration) {
        final long mins = duration.toMinutes();
        return String.format("%d:%02d",
            mins / 60, // h
            mins % 60  // m
        );
    }
}
