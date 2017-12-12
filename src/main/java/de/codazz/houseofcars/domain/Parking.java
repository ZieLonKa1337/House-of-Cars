package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Parking.countParking", query = "SELECT COUNT(p) FROM Parking p WHERE p.parked IS NULL AND p.spot IS NULL")
@NamedQuery(name = "Parking.findParking", query = "SELECT p FROM Parking p WHERE p.parked IS NULL AND p.spot IS NULL AND p.vehicle = :vehicle")
public class Parking extends Activity {
    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @ManyToOne
    private Spot spot;

    @Column(columnDefinition = "timestamptz")
    private ZonedDateTime parked;

    /** @deprecated only for JPA */
    @Deprecated
    public Parking() {}

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
        if (this.spot != null || parked != null) throw new IllegalStateException();
        parked = ZonedDateTime.now(clock);
        this.spot = spot;
    }

    /** @return when the vehicle entered the garage */
    @Override
    public Optional<ZonedDateTime> started() {
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
        if (spot == null) throw new IllegalStateException();
        super.finish();
    }
}
