package de.codazz.houseofcars.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(Activity.class);

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
    protected Activity() {}

    protected Activity(ZonedDateTime started) {
        if (started == null) started = ZonedDateTime.now(clock);
        this.started = started;
        log.trace("started {} at {}", getClass().getSimpleName(), started);
    }

    public ZonedDateTime started() {
        return started;
    }

    public Optional<ZonedDateTime> finished() {
        return Optional.ofNullable(finished);
    }

    public void finish() {
        if (started == null || finished != null) throw new IllegalStateException();
        finished = ZonedDateTime.now(clock);
        log.trace("finished {} at {}", getClass().getSimpleName(), finished);
    }
}
