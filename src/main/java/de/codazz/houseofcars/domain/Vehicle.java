package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.statemachine.EnumStateMachine;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = Vehicle.QUERY_COUNT, query =
    "SELECT COUNT(v) " +
    "FROM Vehicle v")
@NamedQuery(name = Vehicle.QUERY_COUNT_PRESENT, query =
    "SELECT COUNT(t) " +
    "FROM vehicle_state t " +
    "WHERE t.state != 'Away'")
@NamedQuery(name = Vehicle.QUERY_COUNT_PENDING, query =
    "SELECT COUNT(t) " +
    "FROM vehicle_state t " +
    "WHERE t.state = 'LookingForSpot'" +
    " OR t.state = 'Leaving'")
@NamedQuery(name = Vehicle.QUERY_COUNT_STATE, query =
    "SELECT COUNT(t) " +
    "FROM vehicle_state t " +
    "WHERE t.state = :state")
@NamedQuery(name = Vehicle.QUERY_MAY_ENTER, query =
    "SELECT COUNT(t) = 0 " +
    "FROM vehicle_state t " +
    "WHERE t.vehicle.license = :license" +
    " AND t.state != 'Away'")
@NamedQuery(name = Vehicle.QUERY_LAST_TRANSITION, query =
    "SELECT t " +
    "FROM VehicleTransition t " +
    "WHERE t.entity = :entity " +
    "ORDER BY t.time DESC")
@SqlResultSetMapping(name = "count", columns = @ColumnResult(name = "count"))
@NamedNativeQuery(name = Vehicle.QUERY_COUNT_STATE_AT, resultSetMapping = "count", query =
    "SELECT COUNT(*) " +
    "FROM vehicle_state_at(:time) " +
    "WHERE state = :state")
public class Vehicle extends StatefulEntity<Vehicle, Vehicle.Lifecycle, Vehicle.State, Vehicle.State.Data, Vehicle.Event, VehicleTransition> {
    static final String
        QUERY_COUNT           = "Vehicle.count",
        QUERY_COUNT_PRESENT   = "Vehicle.countPresent",
        QUERY_COUNT_PENDING   = "Vehicle.countPending",
        QUERY_COUNT_STATE     = "Vehicle.countState",
        QUERY_COUNT_STATE_AT  = "Vehicle.countStateAt",
        QUERY_MAY_ENTER       = "Vehicle.mayEnter",
        QUERY_LAST_TRANSITION = "Vehicle.lastTransition";

    private static final Logger log = LoggerFactory.getLogger(Vehicle.class);

    /** @return the number of known vehicles */
    public static long count() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT, Long.class)
            .getSingleResult());
    }

    /** @return the number of vehicles that are not {@link State#Away away} */
    public static long countPresent() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_PRESENT, Long.class)
            .getSingleResult());
    }

    /** @return the number of vehicles that are either
     * {@link State#LookingForSpot looking for a spot} or
     * {@link State#Leaving leaving} */
    public static long countPending() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_PENDING, Long.class)
            .getSingleResult());
    }

    public static long count(final State state) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_STATE, Long.class)
            .setParameter("state", state.name())
            .getSingleResult());
    }

    /** @param time when the state was entered
     * @return the number of vehicles in the given state at the given time */
    public static int count(final State state, final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_STATE_AT, BigInteger.class)
            .setParameter("state", state.name())
            .setParameter("time", time)
            .getSingleResult().intValueExact());
    }

    @Id
    private String license;

    @ManyToOne
    private Customer owner;

    /** @deprecated only for JPA */
    @Deprecated
    protected Vehicle() {
        super(VehicleTransition.class, QUERY_LAST_TRANSITION);
    }

    public Vehicle(final String license) {
        super(VehicleTransition.class, QUERY_LAST_TRANSITION);
        this.license = license;
    }

    public String license() {
        return license;
    }

    public Optional<Customer> owner() {
        return Optional.ofNullable(owner);
    }

    public void owner(final Customer owner) {
        this.owner = owner;
    }

    @Override
    protected Lifecycle initLifecycle() {
        return new Lifecycle(new VehicleTransition(this,
            State.Away,
            new State.Data()
        ));
    }

    @Override
    protected Lifecycle restoreLifecycle(final VehicleTransition init) {
        return new Lifecycle(init);
    }

    /** marker interface */
    interface Event {
        void fire();
    }

    public class Lifecycle extends EnumStateMachine<State, State.Data, Event> {
        private Lifecycle(final VehicleTransition init) {
            super(init);
        }

        protected abstract class Event extends CheckedEvent implements Vehicle.Event {
            protected Event(final State state) {
                super(state);
            }

            @Override
            public void fire() {
                Lifecycle.this.fire(this);
            }
        }

        @Override
        public void fire(final Vehicle.Event event) {
            // clone data to avoid modification of past transitions
            data = state().data = state().data.clone();
            super.fire(event);
        }

        @Override
        protected VehicleTransition transition(final State state, final Vehicle.State.Data data) {
            if (state != null) { // outer transition
                log.debug("{}: {} -> {}", license, state(), state);
            } else { // inner transition
                log.debug("{}: {}", license, state());
            }
            try {
                return Garage.instance().persistence.transact((em, __) -> {
                    final VehicleTransition t = new VehicleTransition(Vehicle.this,
                        state != null ? state : state(),
                        data
                    );
                    em.persist(t);
                    return t;
                });
            } finally {
                Status.update();
            }
        }

        /** the vehicle entered the garage */
        public final class EnteredEvent extends Event {
            /** the spot that was recommended to the driver */
            public final Spot recommendedSpot;

            public EnteredEvent(final Spot recommendedSpot) {
                super(State.Away);
                this.recommendedSpot = recommendedSpot;
            }
        }

        /** the vehicle parked on a spot */
        public final class ParkedEvent extends Event {
            public final Spot spot;

            public ParkedEvent(final Spot spot) {
                super(State.LookingForSpot);
                this.spot = spot;
            }

            /** park on the recommended spot */
            public ParkedEvent() {
                this(Lifecycle.this.state().data.recommendedSpot);
            }
        }

        /** the vehicle left its spot, we stop pricing here */
        public final class FreedEvent extends Event {
            public FreedEvent() {
                super(State.Parking);
            }
        }

        /** the parking duration is paid for
         * and the vehicle may now leave */
        public final class PaidEvent extends Event {
            public PaidEvent() {
                super(State.Leaving);
            }
        }

        /** let the vehicle leave */
        public final class LeaveEvent extends Event {
            public LeaveEvent() {
                super(State.Leaving);
                if (!state().data.paid)
                    throw new IllegalStateException("not yet paid!");
            }
        }
    }

    public enum State implements de.codazz.houseofcars.statemachine.State<State.Data, Event> {
        /** the vehicle is outside the garage */
        Away {
            State on(final Lifecycle.EnteredEvent event) {
                data.entered = event;
                return LookingForSpot;
            }
        },
        /** the vehicle is inside the garage, looking for a spot */
        LookingForSpot {
            @Override
            public void onEnter(final Data data) {
                super.onEnter(data);
                if (data.recommendedSpot == null) { // could be restored
                    data.recommendedSpot = data.entered.recommendedSpot;
                    data.entered = null; // gc
                }
            }

            State on(final Lifecycle.ParkedEvent event) {
                data.parked = event;
                return Parking;
            }
        },
        /** the vehicle is parking on a spot, this is what we price */
        Parking {
            @Override
            public void onEnter(final Data data) {
                super.onEnter(data);
                if (data.spot == null) { // could be restored
                    data.spot = data.parked.spot;
                    data.parked = null; // gc
                }
                if (data.fee == null) { // could be restored
                    data.fee = Garage.instance().config.fee().get(data.spot.type());
                }
                if (data.reminder == null) { // could be restored
                    data.reminder = Garage.instance().config.limit().reminder();
                }
                if (data.limit == null) { // could be restored
                    data.limit = Garage.instance().config.limit().overdue();
                }
            }

            State on(final Lifecycle.FreedEvent __) {
                return Leaving;
            }
        },
        /** the vehicle is driving around the garage to leave */
        Leaving {
            @Override
            public void onEnter(final Data data) {
                super.onEnter(data);
                if (data.paid == null) { // could be restored
                    data.paid = false;
                }
            }

            void on(final Lifecycle.PaidEvent __) {
                if (data.paid) throw new IllegalStateException("already paid");
                data.paid = true;
            }

            State on(final Lifecycle.LeaveEvent __) {
                return Away;
            }
        };

        protected volatile Data data;

        @Override
        public void onEnter(final Data data) {
            this.data = data;
        }

        @Override
        public Data onExit() {
            /* null out everything here
             * so we don't need to in each state */
            data.spot = null;
            data.recommendedSpot = null;
            data.fee = null;
            data.paid = null;
            data.reminder = null;
            data.limit = null;
            try {
                return data;
            } finally {
                data = null; // gc
            }
        }

        /** @author rstumm2s */
        @Embeddable
        public static class Data implements Cloneable {
            @Transient
            transient Lifecycle.EnteredEvent entered;
            @Transient
            transient Lifecycle.ParkedEvent parked;

            @ManyToOne
            Spot spot, recommendedSpot;

            /** &lt;currency&gt; per hour */
            BigDecimal fee;
            /** {@code null}: nothing to pay */
            Boolean paid;

            @Column//(columnDefinition = "interval")
            Duration reminder;
            /** a vehicle parking longer than this is overdue */
            @Column(name = "\"limit\""/*, columnDefinition = "interval"*/)
            Duration limit;

            @Override
            public Data clone() {
                try {
                    return (Data) super.clone();
                } catch (final CloneNotSupportedException e) {
                    // never happens
                    throw new AssertionError(e);
                }
            }
        }
    }
}
