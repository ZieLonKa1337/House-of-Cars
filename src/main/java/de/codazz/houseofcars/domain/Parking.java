package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Parking.findCurrent", query =
    "SELECT p FROM Parking p WHERE p.vehicle = :vehicle AND p.finished IS NULL")
public class Parking extends Activity {
    private static final Logger log = LoggerFactory.getLogger(Parking.class);

    /** @return the current {@link Parking Parking} activity of the given vehicle */
    public static Optional<Parking> find(final Vehicle vehicle) {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Parking.findCurrent", Parking.class)
            .setMaxResults(1)
            .setParameter("vehicle", vehicle)
            .getResultStream().findFirst();
    }

    public static List<Parking> findSince(
        final ZonedDateTime started,
        final ZonedDateTime parked,
        final ZonedDateTime freed,
        final ZonedDateTime finished
    ) {
        final StringBuilder jpql = new StringBuilder("SELECT p FROM Parking p WHERE 1 = 1 ");

        if (started != null) {
            jpql.append("AND p.started >= :started ");
        }
        if (parked != null) {
            jpql.append("AND p.parked >= :parked ");
        }
        if (freed != null) {
            jpql.append("AND p.freed >= :freed ");
        }
        if (finished != null) {
            jpql.append("AND p.finished >= :finished ");
        }

        final TypedQuery<Parking> query = Garage.instance().persistence.execute(em -> em)
            .createQuery(jpql.toString(), Parking.class);

        if (started != null) {
            query.setParameter("started", started);
        }
        if (parked != null) {
            query.setParameter("parked", parked);
        }
        if (freed != null) {
            query.setParameter("freed", freed);
        }
        if (finished != null) {
            query.setParameter("finished", finished);
        }

        return query.getResultList();
    }

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
    public Optional<ZonedDateTime> freed() {
        return Optional.ofNullable(freed);
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

    public ZonedDateTime since() {
        return since(Vehicle.State.of(this));
    }

    // TODO refactor to [StateMachine/Activity].lastEvent() with properly persistent state machines
    /** @return when the given state was entered */
    public ZonedDateTime since(final Vehicle.State state) {
        switch (state) {
            case Away: return finished().get();
            case LookingForSpot: return started();
            case Parking: return parked().get();
            case Leaving: return freed().get();
            default: throw new AssertionError("forgot a state?");
        }
    }
}
