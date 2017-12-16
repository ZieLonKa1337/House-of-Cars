package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.statemachine.EnumStateMachine;
import de.codazz.houseofcars.websocket.subprotocol.History;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import org.hibernate.classic.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Vehicle.count", query =
    "SELECT COUNT(v) FROM Vehicle v")
@NamedQuery(name = "Vehicle.countPresent", query =
    "SELECT COUNT(v) FROM Vehicle v WHERE v.state != 'Away'")
@NamedQuery(name = "Vehicle.countPending", query =
    "SELECT COUNT(v) FROM Vehicle v WHERE v.state != 'Parking'")
@NamedQuery(name = "Vehicle.countState", query =
    "SELECT COUNT(v) FROM Vehicle v WHERE v.state = :state")
@NamedQuery(name = "Vehicle.mayEnter", query =
    "SELECT COUNT(v) = 0 FROM Vehicle v WHERE v.license = :license AND v.state != 'Away'")
public class Vehicle extends Entity {
    private static final Logger log = LoggerFactory.getLogger(Vehicle.class);

    /** @return the number of known vehicles */
    public static long count() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.count", Long.class)
            .getSingleResult());
    }

    /** @return the number of vehicles that are not {@link State#Away away} */
    public static long countPresent() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countPresent", Long.class)
            .getSingleResult());
    }

    /** @return the number of vehicles that are either
     * {@link State#LookingForSpot looking for a spot} or
     * {@link State#Leaving leaving} */
    public static long countPending() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countPending", Long.class)
            .getSingleResult());
    }

    public static long count(final Vehicle.State state) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countState", Long.class)
            .setParameter("state", state.name())
            .getSingleResult());
    }

    /** @param time when the state was entered
     * @return the number of vehicles in the given state at the given time */
    public static long count(final Vehicle.State state, final ZonedDateTime time) {
        switch (state) {
            case Away: return Garage.instance().persistence.execute(em -> em)
                .createQuery("SELECT COUNT(v) FROM Vehicle v, Parking p WHERE p.vehicle = v AND p.finished <= :time ", Long.class)
                .setParameter("time", time)
                .getSingleResult();
            case LookingForSpot: return Garage.instance().persistence.execute(em -> em)
                .createQuery("SELECT COUNT(v) FROM Vehicle v, Parking p WHERE p.vehicle = v AND p.started <= :time AND p.parked IS NULL", Long.class)
                .setParameter("time", time)
                .getSingleResult();
            case Parking: return Garage.instance().persistence.execute(em -> em)
                .createQuery("SELECT COUNT(v) FROM Vehicle v, Parking p WHERE p.vehicle = v AND p.parked <= :time AND p.freed IS NULL", Long.class)
                .setParameter("time", time)
                .getSingleResult();
            case Leaving: return Garage.instance().persistence.execute(em -> em)
                .createQuery("SELECT COUNT(v) FROM Vehicle v, Parking p WHERE p.vehicle = v AND p.freed <= :time AND p.finished IS NULL", Long.class)
                .setParameter("time", time)
                .getSingleResult();
            default: throw new AssertionError("forgot a state?");
        }
    }

    @Id
    private String license;

    @Column(nullable = false)
    private String state;

    /** @deprecated only for JPA */
    @Deprecated
    protected Vehicle() {}

    public Vehicle(final String license) {
        this.license = license;
        state = State.Away.name();
    }

    public String license() {
        return license;
    }

    /** <strong>only for testing</strong> */
    String persistentState() {
        return state;
    }

    @Transient
    private transient volatile Lifecycle lifecycle;

    /** do not cache! may be a new instance */
    public Lifecycle state() {
        return state(State.valueOf(state));
    }

    /** restore to specified state */
    Lifecycle state(final State state) {
        if (lifecycle == null || lifecycle.state() != state) {
            lifecycle = new Lifecycle(state, new StateData(Parking.find(this).orElse(null)));
        }
        return lifecycle;
    }

    /** marker interface */
    private interface Event {}

    public class Lifecycle extends EnumStateMachine<State, Event, StateData> {
        private Lifecycle(final State state, final StateData data) {
            super(state, data);
            Vehicle.this.state = state.name();
            state.onEnter(data);
            Vehicle.this.state = Garage.instance().persistence.transact((em, __) -> Vehicle.this.state = state.name());
        }

        public abstract class Event extends CheckedEvent implements Vehicle.Event {
            protected Event(final State state) {
                super(state);
            }
        }

        /** the vehicle entered the garage */
        public class EnteredEvent extends Event {
            public EnteredEvent() {
                super(State.Away);
            }
        }

        /** the vehicle parked on a spot */
        public class ParkedEvent extends Event {
            public final Spot spot;

            public ParkedEvent(final Spot spot) {
                super(State.LookingForSpot);
                this.spot = spot;
            }
        }

        /** the vehicle left its spot, we stop pricing here */
        public class LeftSpotEvent extends Event {
            public LeftSpotEvent() {
                super(State.Parking);
            }
        }

        /** the vehicle left the garage through a gate */
        public class LeftEvent extends Event {
            public LeftEvent() {
                super(State.Leaving);
            }
        }
    }

    public enum State implements de.codazz.houseofcars.statemachine.State<Event, StateData> {
        /** the vehicle is outside the garage */
        Away {
            State on(final Lifecycle.EnteredEvent __) {
                return LookingForSpot;
            }
        },
        /** the vehicle is inside the garage, looking for a spot */
        LookingForSpot {
            @Override
            public void onEnter(final StateData data) {
                super.onEnter(data);

                if (data.parking != null) {
                    log.error("previous Parking from {} not properly finished?", data.parking.started());
                }

                data.parking = Garage.instance().persistence.transact((em, __) -> {
                    final de.codazz.houseofcars.domain.Parking p = new de.codazz.houseofcars.domain.Parking(data.vehicle());
                    em.persist(p);
                    return p;
                });
                History.update();
            }

            State on(final Lifecycle.ParkedEvent event) {
                if (data.parking.spot().isPresent()) {
                    log.error("previous Parking on #{} not properly finished?", data.parking.spot().get().id());
                }
                data.spot = event.spot;
                return Parking;
            }
        },
        /** the vehicle is parking on a spot, this is what we price */
        Parking {
            @Override
            public void onEnter(final StateData data) {
                super.onEnter(data);
                Objects.requireNonNull(data.spot);
                Garage.instance().persistence.transact((__, ___) -> {
                    data.parking.park(data.spot);
                    return null;
                });
                Status.update();
            }

            @Override
            public void onExit() {
                Garage.instance().persistence.transact((__, ___) -> {
                    data.parking.free();
                    return null;
                });
                Status.update();
            }

            State on(final Lifecycle.LeftSpotEvent __) {
                return Leaving;
            }
        },
        /** the vehicle is driving around the garage to leave */
        Leaving {
            @Override
            public void onExit() {
                data.parking = Garage.instance().persistence.transact((__, ___) -> {
                    data.parking.finish();
                    return null;
                });
                History.update();
            }

            State on(final Lifecycle.LeftEvent __) {
                return Away;
            }
        };

        StateData data;

        @Override
        public void onEnter(final StateData data) {
            this.data = data;
            Garage.instance().persistence.<Void>transact((__, ___) -> {
                data.vehicle().state = name();
                return null;
            });
        }

        public static State of(final Parking parking) {
            return of(parking, ZonedDateTime.now());
        }

        /** @param time get the state at this time
         * @return the corresponding vehicle state */
        public static State of(final Parking parking, final ZonedDateTime time) {
            if (parking.finished().isPresent() && parking.finished().get().compareTo(time) <= 0) {
                return State.Away;
            }
            if (parking.freed().isPresent() && parking.freed().get().compareTo(time) <= 0) {
                return State.Leaving;
            }
            if (parking.parked().isPresent() && parking.parked().get().compareTo(time) <= 0) {
                return State.Parking;
            }
            return State.LookingForSpot;
        }
    }

    public class StateData {
        Parking parking;
        Spot spot;

        StateData(final Parking parking) {
            this.parking = parking;
            if (parking != null) {
                spot = parking.spot().orElse(null);
            }
        }

        Vehicle vehicle() {
            return Vehicle.this;
        }

        public Optional<Parking> parking() {
            return Optional.ofNullable(parking);
        }

        public Optional<Spot> spot() {
            return Optional.ofNullable(spot);
        }
    }
}
