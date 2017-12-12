package de.codazz.houseofcars.business;

import de.codazz.houseofcars.GarageImpl;
import de.codazz.houseofcars.domain.Parking;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.OnEvent;
import de.codazz.houseofcars.statemachine.RootStateMachine;
import de.codazz.houseofcars.statemachine.State;
import de.codazz.houseofcars.statemachine.StateMachineException;

/** @author rstumm2s */
@State(root = true, end = true)
public class Gate extends RootStateMachine<String, Boolean, Void> {
    /** as state */
    public Gate() throws StateMachineException {}

    /** as state machine */
    public Gate(final boolean lazy) throws StateMachineException {
        super(Gate.class, lazy, null, 2);
    }

    /** the gate sees a vehicle's license
     * and asks whether it is allowed in */
    @OnEvent(value = String.class, next = Open.class)
    boolean onOpenRequest(final String license) {
        return GarageImpl.instance().numFree() > 0 &&
            GarageImpl.instance().persistence.execute(em -> em
                .createNamedQuery("Vehicle.mayEnter", Boolean.class)
                .setParameter("license", license)
                .getSingleResult());
    }

    @State
    public class Open {
        /** the vehicle entered the garage
         * and the gate has closed again */
        @OnEvent(value = String.class, next = Gate.class)
        void onEntered(final String license) {
            GarageImpl.instance().persistence.<Void>submitTransact((em, __) -> {
                Vehicle vehicle = em.find(Vehicle.class, license);
                if (vehicle == null) {
                    vehicle = new Vehicle(license);
                    em.persist(vehicle);
                }
                vehicle.present(true);

                final Parking p = new Parking(vehicle);
                em.persist(p);
                return null;
            });
        }
    }
}
