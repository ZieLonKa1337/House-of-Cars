package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.statemachine.EnumStateMachine;
import de.codazz.houseofcars.statemachine.Transition;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@NamedQuery(name = Reservation.QUERY_COUNT_AT, query =
    "SELECT COUNT(r) " +
    "FROM Reservation r " +
    "WHERE r.start <= :time" +
    " AND r.end >= :time" +
    " AND NOT EXISTS (" +
    "  SELECT t" +
    "  FROM ReservationTransition t" +
    "  WHERE t.entity = r" +
    "   AND t.state = 'Annulled'" +
    " )")
@NamedQuery(name = Reservation.QUERY_COUNT_TYPE_AT, query =
    "SELECT COUNT(r) " +
    "FROM Reservation r " +
    "WHERE r.spotType <= :spotType" +
    " AND r.start <= :time" +
    " AND r.end >= :time" +
    " AND NOT EXISTS (" +
    "  SELECT t" +
    "  FROM ReservationTransition t" +
    "  WHERE t.entity = r" +
    "   AND t.state = 'Annulled'" +
    " )")
@NamedQuery(name = Reservation.QUERY_COUNT_CURRENT, query =
    "SELECT COUNT(r) " +
    "FROM Reservation r " +
    "WHERE r.start <= CURRENT_TIMESTAMP" +
    " AND r.end >= CURRENT_TIMESTAMP" +
    " AND NOT EXISTS (" +
    "  SELECT t" +
    "  FROM ReservationTransition t" +
    "  WHERE t.entity = r" +
    "   AND t.state = 'Annulled'" +
    " )")
@NamedQuery(name = Reservation.QUERY_COUNT_TYPE_CURRENT, query =
    "SELECT COUNT(r) " +
    "FROM Reservation r " +
    "WHERE r.spotType = :spotType" +
    " AND r.start <= CURRENT_TIMESTAMP" +
    " AND r.end >= CURRENT_TIMESTAMP" +
    " AND NOT EXISTS (" +
    "  SELECT t" +
    "  FROM ReservationTransition t" +
    "  WHERE t.entity = r" +
    "   AND t.state = 'Annulled'" +
    " )")
@NamedQuery(name = Reservation.QUERY_LAST_TRANSITION, query =
    "SELECT t " +
    "FROM ReservationTransition t " +
    "WHERE t.entity = :entity " +
    "ORDER BY t.time DESC")
@javax.persistence.Entity
public class Reservation extends StatefulEntity<Reservation, Reservation.Lifecycle, Reservation.State, Void, Reservation.Event, ReservationTransition> {
    public static final String
        QUERY_COUNT_AT           = "Reservation.countAt",
        QUERY_COUNT_TYPE_AT      = "Reservation.countTypeAt",
        QUERY_COUNT_CURRENT      = "Reservation.countCurrent",
        QUERY_COUNT_TYPE_CURRENT = "Reservation.countTypeCurrent",
        QUERY_LAST_TRANSITION    = "Reservation.lastTransition";

    public static long countAt(final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_AT, Long.class)
            .setParameter("time", time)
            .getSingleResult());
    }

    public static long countTypeAt(final ZonedDateTime time, final Spot.Type spotType) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_AT, Long.class)
            .setParameter("spotType", spotType)
            .setParameter("time", time)
            .getSingleResult());
    }

    public static long countCurrent() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_CURRENT, Long.class)
            .getSingleResult());
    }

    public static long countCurrent(final Spot.Type spotType) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_TYPE_CURRENT, Long.class)
            .setParameter("spotType", spotType)
            .getSingleResult());
    }

    private static final Logger log = LoggerFactory.getLogger(Reservation.class);

    @Id
    @GeneratedValue
    private long id;

    @ManyToOne(optional = false)
    private Customer customer;

    @Column(columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime start;

    @Column(name = "\"end\"", columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime end;

    @Enumerated(EnumType.STRING)
    private Spot.Type spotType;

    /** @deprecated only for JPA */
    @Deprecated
    protected Reservation() {
        super(ReservationTransition.class, QUERY_LAST_TRANSITION);
    }

    public Reservation(
        final Customer customer,
        final ZonedDateTime start, final ZonedDateTime end,
        final Spot.Type spotType
    ) {
        super(ReservationTransition.class, QUERY_LAST_TRANSITION);
        this.customer = Objects.requireNonNull(customer);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
        this.spotType = spotType;

        final long max = spotType == null ? Spot.count() : Spot.count(spotType);
        if (countAt(start) + 1 > max)
            throw new IllegalStateException("all spots are reserved at this time");
    }

    public long id() {
        return id;
    }

    public Customer customer() {
        return customer;
    }

    public ZonedDateTime start() {
        return start;
    }

    @Transient
    private transient volatile de.codazz.houseofcars.template.ZonedDateTime startTemplate;

    public de.codazz.houseofcars.template.ZonedDateTime startTemplate() {
        if (startTemplate == null) {
            startTemplate = new de.codazz.houseofcars.template.ZonedDateTime(start);
        }
        return startTemplate;
    }

    public ZonedDateTime end() {
        return end;
    }

    @Transient
    private transient volatile de.codazz.houseofcars.template.ZonedDateTime endTemplate;

    public de.codazz.houseofcars.template.ZonedDateTime endTemplate() {
        if (endTemplate == null) {
            endTemplate = new de.codazz.houseofcars.template.ZonedDateTime(end);
        }
        return endTemplate;
    }

    public Optional<Spot.Type> spotType() {
        return Optional.ofNullable(spotType);
    }

    @Override
    protected Lifecycle initLifecycle() {
        return new Lifecycle(new ReservationTransition(this,
            State.Active
        ));
    }

    @Override
    protected Lifecycle restoreLifecycle(final ReservationTransition init) {
        return new Lifecycle(init);
    }

    public interface Event {
        void fire();
    }

    public class Lifecycle extends EnumStateMachine<State, Void, Event> {
        private Lifecycle(final ReservationTransition init) {
            super(init);
        }

        @Override
        protected Transition<Reservation.Event, State, Void> transition(final State state, final Void __) {
            if (state != null) { // outer transition
                log.debug("{}: {} -> {}", id, state(), state);
            } else { // inner transition
                log.debug("{}: {}", id, state());
            }
            try {
                return Garage.instance().persistence.transact((em, ___) -> {
                    final ReservationTransition t = new ReservationTransition(Reservation.this, state);
                    em.persist(t);
                    return t;
                });
            } finally {
                Status.update();
            }
        }

        protected abstract class Event extends CheckedEvent implements Reservation.Event {
            protected Event(final State state) {
                super(state);
            }

            @Override
            public void fire() {
                Lifecycle.this.fire(this);
            }
        }

        public final class AnnullEvent extends Event {
            public AnnullEvent() {
                super(State.Active);
            }
        }
    }

    public enum State implements de.codazz.houseofcars.statemachine.State<Void, Event> {
        Active,
        Annulled;

        State on(final Lifecycle.AnnullEvent __) {
            return Annulled;
        }

        /** utility for logic-less templates */
        public boolean active() {
            return this == Active;
        }
    }
}
