package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@Entity(name = "vehicle_state")
public final class VehicleStatus {
    @Id
    private String vehicle_license;
    private String state;
    private ZonedDateTime since;

    protected VehicleStatus() {}

    public String vehicleLicense() {
        return vehicle_license;
    }

    public Vehicle.State state() {
        return Vehicle.State.valueOf(state);
    }

    public Optional<ZonedDateTime> since() {
        return Optional.ofNullable(since);
    }
}
