package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@Entity(name = "vehicle_state")
public final class VehicleStatus {
    @EmbeddedId
    private VehicleStatusPK $;
    @Column(nullable = false)
    private String state;
    private ZonedDateTime since;

    protected VehicleStatus() {}

    public Vehicle vehicle() {
        return $.vehicle;
    }

    public Vehicle.State state() {
        return Vehicle.State.valueOf(state);
    }

    public Optional<ZonedDateTime> since() {
        return Optional.ofNullable(since);
    }
}

/** @author rstumm2s */
@Embeddable
final class VehicleStatusPK implements Serializable {
    @OneToOne(optional = false)
    Vehicle vehicle;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleStatusPK)) return false;
        final VehicleStatusPK that = (VehicleStatusPK) o;
        return Objects.equals(vehicle, that.vehicle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicle);
    }
}
