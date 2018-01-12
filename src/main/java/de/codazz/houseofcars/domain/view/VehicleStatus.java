package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@Entity(name = "vehicle_state")
public final class VehicleStatus implements Serializable {
    @Id
    @OneToOne
    private Vehicle vehicle;

    @Column(nullable = false)
    private String state;
    private ZonedDateTime since;

    protected VehicleStatus() {}

    public Vehicle vehicle() {
        return vehicle;
    }

    public Vehicle.State state() {
        return Vehicle.State.valueOf(state);
    }

    public Optional<ZonedDateTime> since() {
        return Optional.ofNullable(since);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleStatus)) return false;
        final VehicleStatus that = (VehicleStatus) o;
        return Objects.equals(vehicle, that.vehicle) &&
            Objects.equals(state, that.state) &&
            Objects.equals(since, that.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicle, state, since);
    }
}
