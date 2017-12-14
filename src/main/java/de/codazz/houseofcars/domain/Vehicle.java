package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.statemachine.OnEnter;
import de.codazz.houseofcars.statemachine.OnEvent;
import de.codazz.houseofcars.statemachine.OnExit;
import de.codazz.houseofcars.statemachine.State;
import de.codazz.houseofcars.statemachine.StateMachine;
import de.codazz.houseofcars.statemachine.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

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

    /** @return the number of vehicles that are not {@link Lifecycle.Away away} */
    public static long countPresent() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countPresent", Long.class)
            .getSingleResult());
    }

    /** @return the number of vehicles that are either
     * {@link Lifecycle.LookingForSpot looking for a spot} or
     * {@link Lifecycle.Leaving leaving} */
    public static long countPending() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countPending", Long.class)
            .getSingleResult());
    }

    public static long count(final Class<? extends Lifecycle.PersistentState> state) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.countState", Long.class)
            .setParameter("state", state.getSimpleName())
            .getSingleResult());
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
        state = Lifecycle.Away.class.getSimpleName();
    }

    public String license() {
        return license;
    }

    /** @deprecated only for testing */
    @Deprecated
    String persistentState() {
        return state;
    }

    @Transient
    private transient Lifecycle lifecycle;

    @SuppressWarnings("unchecked")
    public Lifecycle state() throws StateMachineException, ClassNotFoundException {
        return state((Class<? extends Lifecycle.PersistentState>) Class.forName(Lifecycle.class.getName() + "$" + state));
    }

    /** restore to specified state */
    Lifecycle state(final Class<? extends Lifecycle.PersistentState> state) throws StateMachineException {
        this.state = state.getSimpleName();
        if (lifecycle == null || !lifecycle.state().getClass().equals(state)) {
            lifecycle = new Lifecycle(state);

            // call with correct @OnEnter parameter
            if (state.equals(Lifecycle.Parking.class)) {
                lifecycle.start(lifecycle.parking.spot().orElse(null));
            } else {
                lifecycle.start();
            }
        }
        return lifecycle;
    }

    public class Lifecycle extends StateMachine<Lifecycle.Event, Void, Void> {
        private de.codazz.houseofcars.domain.Parking parking = de.codazz.houseofcars.domain.Parking.find(Vehicle.this).orElse(null);

        protected Lifecycle(final Class<?> root) throws StateMachineException {
            super(root, true, null, 4);
        }

        protected abstract class PersistentState {
            @OnEnter // not inherited
            void enter() {
                final String state = getClass().getSimpleName();
                log.info("{} state: {} -> {}", Vehicle.this.license, Vehicle.this.state, state);
                Garage.instance().persistence.<Void>transact((em, __) -> {
                    Vehicle.this.state = state;
                    return null;
                });
            }
        }

        public abstract class Event {}

        /** the vehicle is outside the garage */
        @State(end = true)
        @OnEvent(value = Away.EnteredEvent.class, next = LookingForSpot.class)
        public class Away extends PersistentState {
            @OnEnter
            @Override
            void enter() {
                super.enter();
            }

            /** the vehicle entered the garage */
            public class EnteredEvent extends Event {}
        }

        /** the vehicle is inside the garage, looking for a spot */
        @State
        public class LookingForSpot extends PersistentState {
            private Spot spot = parking == null ? null : parking.spot().orElse(null);

            @OnEnter
            @Override
            void enter() {
                if (parking != null) {
                    log.error("previous Parking from {} not properly finished?", parking.started());
                }
                parking = Garage.instance().persistence.transact((em, __) -> {
                    final de.codazz.houseofcars.domain.Parking p = new de.codazz.houseofcars.domain.Parking(Vehicle.this);
                    em.persist(p);
                    return p;
                });
                super.enter();
            }

            @OnEvent(value = ParkedEvent.class, next = Parking.class)
            void onParked(final ParkedEvent event) {
                if (spot != null) {
                    log.error("previous Parking on #{} not properly finished?", spot.id());
                }
                spot = event.spot;
            }

            @OnExit
            Spot exit() {
                final Spot spot = this.spot;
                this.spot = null;
                return spot;
            }

            /** the vehicle parked on a spot */
            public class ParkedEvent extends Event {
                public final Spot spot;

                public ParkedEvent(final Spot spot) {
                    this.spot = spot;
                }
            }
        }

        /** the vehicle is parking on a spot, this is what we price */
        @State(end = true)
        @OnEvent(value = Parking.LeftSpotEvent.class, next = Leaving.class)
        public class Parking extends PersistentState {
            @OnEnter
            void enter(final Spot spot) {
                Garage.instance().persistence.transact((em, __) -> {
                    parking.park(spot);
                    return null;
                });
                super.enter();
            }

            @OnExit
            void exit() {
                Garage.instance().persistence.transact((em, __) -> {
                    parking.free();
                    return null;
                });
            }

            /** the vehicle left its spot, we stop pricing here */
            public class LeftSpotEvent extends Event {}
        }

        /** the vehicle is driving around the garage to leave */
        @State
        @OnEvent(value = Leaving.LeftEvent.class, next = Away.class)
        public class Leaving extends PersistentState {
            @OnEnter
            @Override
            void enter() {
                super.enter();
            }

            @OnExit
            void exit() {
                parking = Garage.instance().persistence.transact((em, __) -> {
                    parking.finish();
                    return null;
                });
            }

            /** the vehicle left the garage through a gate */
            public class LeftEvent extends Event {}
        }
    }
}
