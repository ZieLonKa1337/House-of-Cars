package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@MappedSuperclass
public abstract class Activity extends Entity {
    @Id
    @Column(columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime started;

    @Column(columnDefinition = "timestamptz")
    private ZonedDateTime finished;

    /** for testing */
    @Transient
    transient Clock clock = Clock.systemDefaultZone();

    /** @deprecated only for JPA */
    @Deprecated
    public Activity() {}

    protected Activity(ZonedDateTime started) {
        if (started == null) started = ZonedDateTime.now(clock);
        this.started = started;
    }

    public Optional<ZonedDateTime> started() {
        return Optional.ofNullable(started);
    }

    public Optional<ZonedDateTime> finished() {
        return Optional.ofNullable(finished);
    }

    public void finish() {
        if (started == null || finished != null) throw new IllegalStateException();
        finished = ZonedDateTime.now(clock);
    }
}
