package de.codazz.houseofcars.business;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.EnumStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/** @author rstumm2s */
public class Gate extends EnumStateMachine<Gate.State, Gate.Event, Void> {
    private static final Logger log = LoggerFactory.getLogger(Gate.class);

    public Gate() {
        super(State.Closed, null);
    }

    public enum State implements de.codazz.houseofcars.statemachine.State<Event, Void> {
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
                    vehicle.state().fire(vehicle.state().new EnteredEvent());
                } catch (final NoSuchMethodException | InvocationTargetException e) {
                    log.error("failed to mutate state of vehicle {}", event.license);
                }

                return Closed;
            }
        }
    }

    public static abstract class Event {}

    public static class OpenedEvent extends Event {}

    /** a vehicle entered the garage
     * and the gate has closed again */
    public static class EnteredEvent extends Event {
        public final String license;

        public EnteredEvent(final String license) {
            this.license = license;
        }
    }

    /** the gate sees a vehicle's license
     * and asks whether it is allowed in */
    public boolean requestOpen(final String license) {
        if (Spot.countFree() == 0) return false;
        final boolean permission = Garage.instance().persistence.execute(em -> em
            .createNamedQuery("Vehicle.mayEnter", Boolean.class)
            .setParameter("license", license)
            .getSingleResult());
        log.trace("requested permission for {}: {}", license, permission);
        return permission;
    }
}
