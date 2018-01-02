package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/** @author rstumm2s */
@Entity(name = "spot_state")
final class SpotStatus {
    @EmbeddedId
    private SpotStatusPK $;
    @OneToOne
    private Vehicle vehicle;
    private ZonedDateTime since;

    protected SpotStatus() {}
}

/** @author rstumm2s */
@Embeddable
final class SpotStatusPK implements Serializable {
    @OneToOne(optional = false)
    private Spot spot;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SpotStatusPK)) return false;
        final SpotStatusPK that = (SpotStatusPK) o;
        return Objects.equals(spot, that.spot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spot);
    }
}
