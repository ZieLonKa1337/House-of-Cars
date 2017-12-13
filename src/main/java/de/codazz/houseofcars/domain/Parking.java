package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.statemachine.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Parking.countLookingForSpot", query = "SELECT COUNT(p) FROM Parking p WHERE p.parked IS NULL")
@NamedQuery(name = "Parking.findLookingForSpot", query = "SELECT p FROM Parking p WHERE p.parked IS NULL AND p.vehicle = :vehicle")
public class Parking extends Activity {
    private static final Logger log = LoggerFactory.getLogger(Parking.class);

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @ManyToOne
    private Spot spot;

    @Column(columnDefinition = "timestamptz")
    private ZonedDateTime parked;

    @Column(columnDefinition = "timestamptz")
    private ZonedDateTime freed;

    /** @deprecated only for JPA */
    @Deprecated
    protected Parking() {}

    public Parking(final Vehicle vehicle) {
        super(null);
        this.vehicle = vehicle;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public Optional<Spot> spot() {
        return Optional.ofNullable(spot);
    }

    /** @return when the vehicle occupied its spot */
    public Optional<ZonedDateTime> parked() {
        return Optional.ofNullable(parked);
    }

    /** @param spot the spot occupied by the vehicle */
    public void park(final Spot spot) {
        if (parked == null) {
            log.trace("{} parked on #{} ({})", vehicle.license(), spot.id(), spot.type());
        } else if (!spot.equals(this.spot)) {
            log.trace("{} reparked from #{} ({}) to #{} ({})", vehicle.license(), this.spot.id(), this.spot.type(), spot.id(), spot.type());
        }
        parked = ZonedDateTime.now(clock);
        this.spot = spot;
    }

    /** @return when the vehicle left its spot */
    public ZonedDateTime freed() {
        return freed;
    }

    public void free() {
        if (parked == null) throw new IllegalStateException();
        freed = ZonedDateTime.now(clock);
        log.trace("{} freed #{} ({})", vehicle.license(), spot.id(), spot.type());
    }

    /** @return when the vehicle entered the garage */
    @Override
    public ZonedDateTime started() {
        return super.started();
    }

    /** @return when the vehicle left its spot */
    @Override
    public Optional<ZonedDateTime> finished() {
        return super.finished();
    }

    /** @throws IllegalStateException if {@link #spot} has not yet been set */
    @Override
    public void finish() {
        if (freed == null) throw new IllegalStateException();
        super.finish();
    }
}
