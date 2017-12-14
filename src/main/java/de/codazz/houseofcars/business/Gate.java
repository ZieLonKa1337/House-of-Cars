package de.codazz.houseofcars.business;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.OnEvent;
import de.codazz.houseofcars.statemachine.RootStateMachine;
import de.codazz.houseofcars.statemachine.State;
import de.codazz.houseofcars.statemachine.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author rstumm2s */
@State(root = true, end = true)
@OnEvent(value = Gate.OpenedEvent.class, next = Gate.Open.class)
public class Gate extends RootStateMachine<Gate.Event, Void, Void> {
    private static final Logger log = LoggerFactory.getLogger(Gate.class);

    /** as state */
    public Gate() throws StateMachineException {}

    /** as state machine */
    public Gate(final boolean lazy) throws StateMachineException {
        super(Gate.class, lazy, null, 2);
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

    public abstract class Event {}

    /** the gate is open */
    public class OpenedEvent extends Event {}

    @State
    public class Open {
        @OnEvent(value = EnteredEvent.class, next = Gate.class)
        void onEntered(final EnteredEvent event) {
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
                vehicle.state().onEvent(((Vehicle.Lifecycle.Away) vehicle.state().state()).new EnteredEvent());
            } catch (final StateMachineException | ClassNotFoundException e) {
                log.error("failed to mutate state of vehicle {}", event.license);
                throw new RuntimeException(e);
            }
        }

        /** a vehicle entered the garage
         * and the gate has closed again */
        public class EnteredEvent extends Event {
            public final String license;

            public EnteredEvent(final String license) {
                this.license = license;
            }
        }
    }
}
