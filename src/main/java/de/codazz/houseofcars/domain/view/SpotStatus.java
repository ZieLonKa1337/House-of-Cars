package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/** @author rstumm2s */
@Entity(name = "spot_state")
public final class SpotStatus implements Serializable {
    @Id
    @OneToOne
    private Spot spot;

    @OneToOne
    private Vehicle vehicle;
    private ZonedDateTime since;

    protected SpotStatus() {}

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SpotStatus)) return false;
        final SpotStatus that = (SpotStatus) o;
        return Objects.equals(spot, that.spot) &&
            Objects.equals(vehicle, that.vehicle) &&
            Objects.equals(since, that.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spot, vehicle, since);
    }
}
