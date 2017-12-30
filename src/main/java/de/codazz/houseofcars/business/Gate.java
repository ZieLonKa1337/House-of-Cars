package de.codazz.houseofcars.business;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.EnumStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/** @author rstumm2s */
public class Gate extends EnumStateMachine<Gate.State, Void, Gate.Event> {
    private static final Logger log = LoggerFactory.getLogger(Gate.class);

    public Gate() {
        super(State.Closed, null);
    }

    public enum State implements de.codazz.houseofcars.statemachine.State<Void, Event> {
        Closed {
            State on(final OpenedEvent __) {
                return Open;
            }
        },
        Open {
            State on(final EnteredEvent event) {
                final Vehicle vehicle = Garage.instance().persistence.transact((em, __) -> {
                    Vehicle v = em.find(Vehicle.class, event.license);
                    if (v == null) {
                        log.trace("new vehicle: {}", event.license);
                        v = new Vehicle(event.license);
                        em.persist(v);
                    }
                    log.trace("{} entered", event.license);
                    return v;
                });

                try {
                    vehicle.state().new EnteredEvent(
                        Garage.instance().persistence.execute(em -> em.find(Spot.class, event.recommendedSpot))
                    ).fire();
                } catch (final InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    log.error("failed to mutate state of vehicle {}", event.license);
                }

                return Closed;
            }
        }
    }

    /** marker interface */
    public interface Event {}

    private abstract class GateEvent extends CheckedEvent implements Event {
        protected GateEvent(final State state) {
            super(state);
        }

        public void fire() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Gate.this.fire(this);
        }
    }

    /** the gate has opened so a vehicle can drive in */
    public class OpenedEvent extends GateEvent {
        public OpenedEvent() {
            super(State.Closed);
        }
    }

    /** a vehicle entered the garage
     * and the gate has closed again */
    public class EnteredEvent extends GateEvent {
        public final String license;
        public final int recommendedSpot;

        public EnteredEvent(final String license, final int recommendedSpot) {
            super(State.Open);
            this.license = license;
            this.recommendedSpot = recommendedSpot;
        }
    }

    /** the gate sees a vehicle's license
     * and asks whether it is allowed in */
    public boolean requestOpen(final String license) {
        final boolean permission = Spot.countFree() != 0 &&
             Garage.instance().persistence.execute(em -> em
                .createNamedQuery("Vehicle.mayEnter", Boolean.class)
                .setParameter("license", license)
                .getSingleResult());
        log.trace("permission for {}: {}", license, permission);
        return permission;
    }
}
