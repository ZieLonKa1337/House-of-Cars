package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.GarageImpl;
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
@NamedQuery(name = "Vehicle.countPresent", query = "SELECT COUNT(v) FROM Vehicle v WHERE v.state != 'Away'")
@NamedQuery(name = "Vehicle.mayEnter", query = "SELECT COUNT(v) = 0 FROM Vehicle v WHERE v.license = :license AND v.state != 'Away'")
public class Vehicle extends Entity {
    private static final Logger log = LoggerFactory.getLogger(Vehicle.class);

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

    @Transient
    private transient Lifecycle lifecycle;

    public Lifecycle state() throws StateMachineException, ClassNotFoundException {
        if (lifecycle == null) {
            lifecycle = new Lifecycle(Class.forName(Lifecycle.class.getName() + "$" + state));
            lifecycle.start();
        }
        return lifecycle;
    }

    public class Lifecycle extends StateMachine<Lifecycle.Event, Void, Void> {
        private de.codazz.houseofcars.domain.Parking parking;

        protected Lifecycle(final Class<?> root) throws StateMachineException {
            super(root, true, null, 4);
        }

        protected class PersistentState {
            @OnEnter // not inherited
            void enter() {
                final String state = getClass().getSimpleName();
                log.info("{} state: {} -> {}", Vehicle.this.license, Vehicle.this.state, state);
                GarageImpl.instance().persistence.<Void>transact((em, __) -> {
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
            /** the vehicle entered the garage */
            public class EnteredEvent extends Event {}
        }

        /** the vehicle is inside the garage, looking for a spot */
        @State
        public class LookingForSpot extends PersistentState {
            private Spot spot;

            @OnEnter
            @Override
            void enter() {
                parking = GarageImpl.instance().persistence.transact((em, __) -> {
                    final de.codazz.houseofcars.domain.Parking p = new de.codazz.houseofcars.domain.Parking(Vehicle.this);
                    em.persist(p);
                    return p;
                });
                super.enter();
            }

            @OnEvent(value = ParkedEvent.class, next = Parking.class)
            void onParked(final ParkedEvent event) {
                spot = event.spot;
            }

            @OnExit
            Spot exit() {
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
        @OnEvent(value = Parking.LeftSpot.class, next = Leaving.class)
        public class Parking extends PersistentState {
            @OnEnter
            void enter(final Spot spot) {
                GarageImpl.instance().persistence.transact((em, __) -> {
                    parking.park(spot);
                    return null;
                });
                super.enter();
            }

            @OnExit
            void exit() {
                GarageImpl.instance().persistence.transact((em, __) -> {
                    parking.free();
                    return null;
                });
            }

            /** the vehicle left its spot, we stop pricing here */
            public class LeftSpot extends Event {}
        }

        /** the vehicle is driving around the garage to leave */
        @State
        @OnEvent(value = Leaving.Left.class, next = Away.class)
        public class Leaving extends PersistentState {
            @OnEnter
            @Override
            void enter() {
                super.enter();
            }

            @OnExit
            void exit() {
                GarageImpl.instance().persistence.<Void>transact((em, __) -> {
                    parking.finish();
                    return null;
                });
            }

            /** the vehicle left the garage through a gate */
            public class Left extends Event {}
        }
    }
}
