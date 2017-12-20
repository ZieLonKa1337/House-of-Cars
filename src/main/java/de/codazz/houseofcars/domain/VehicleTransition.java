package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "VehicleTransition.since", query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time >= :time")
@NamedQuery(name = "VehicleTransition.previous", query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time < :time" +
    " AND t.vehicle = :vehicle")
public class VehicleTransition extends Transition<Vehicle.Event, Vehicle.State, Vehicle.State.Data> {
    public static TypedQuery<VehicleTransition> since(final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.since", VehicleTransition.class)
            .setParameter("time", time));
    }

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String state;

    @Transient
    private transient Vehicle.State stateInstance;

    /** @deprecated only for JPA */
    @Deprecated
    protected VehicleTransition() {}

    protected VehicleTransition(final Vehicle vehicle, final Vehicle.State state, final Vehicle.State.Data data) {
        super(data);
        this.vehicle = vehicle;
        this.state = state.name();
        stateInstance = state;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    @Override
    public Vehicle.State state() {
        if (stateInstance == null) {
            stateInstance = Vehicle.State.valueOf(state);
        }
        return stateInstance;
    }

    /** @return the associated vehicle's
     * previous transition, if any */
    public Optional<VehicleTransition> previous() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.previous", VehicleTransition.class)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("vehicle", vehicle)
            .getResultStream().findFirst());
    }
}
