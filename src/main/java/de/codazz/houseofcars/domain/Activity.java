package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@MappedSuperclass
public abstract class Activity extends Entity {
    @Id
    @Column(columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime created;

    @Column(columnDefinition = "timestamptz")
    private ZonedDateTime started, finished;

    @Transient
    transient Clock clock;

    @PrePersist
    public void prePersist() {
        /* TODO this will usually be later than started or finished
         * so it's not really the creation time / logical inconsistency => remove? */
        if (created == null) created = ZonedDateTime.now(clock);
    }

    public Optional<ZonedDateTime> started() {
        return Optional.ofNullable(started);
    }

    public Optional<ZonedDateTime> finished() {
        return Optional.ofNullable(finished);
    }

    public ZonedDateTime created() {
        return created;
    }

    public void start() {
        if (started != null) throw new IllegalStateException();
        started = ZonedDateTime.now(clock);
    }

    public void finish() {
        if (started == null || finished != null) throw new IllegalStateException();
        finished = ZonedDateTime.now(clock);
    }
}
