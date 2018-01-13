package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

/** @author rstumm2s */
@MappedSuperclass
public abstract class Transition<Event, State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data> extends Entity implements de.codazz.houseofcars.statemachine.Transition<Event, State, Data> {
    /** only for testing */
    private static Clock clock = Clock.systemDefaultZone();

    /** @param duration {@code null} to reset
     * @deprecated only for testing */
    @Deprecated
    static void tick(final Duration duration) {
        if (duration == null) {
            clock = Clock.systemDefaultZone();
        } else {
            clock = Clock.offset(clock, duration);
        }
    }

    @Id
    @Column(columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime time = ZonedDateTime.now(clock);

    @Embedded
    protected Data data;

    /** @deprecated only for JPA */
    @Deprecated
    protected Transition() {}

    protected Transition(final Data data) {
        this.data = data;
    }

    @Override
    public Data data() {
        return data;
    }

    public ZonedDateTime time() {
        return time;
    }
}
