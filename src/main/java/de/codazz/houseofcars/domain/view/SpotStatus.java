package de.codazz.houseofcars.domain.view;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/** @author rstumm2s */
@Entity(name = "spot_state")
public final class SpotStatus {
    @EmbeddedId
    private PK $;
    @OneToOne
    private Vehicle vehicle;
    private ZonedDateTime since;

    protected SpotStatus() {}

    /** @author rstumm2s */
    @Embeddable
    private static final class PK implements Serializable {
        @OneToOne(optional = false)
        private Spot spot;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            final PK that = (PK) o;
            return Objects.equals(spot, that.spot);
        }

        @Override
        public int hashCode() {
            return Objects.hash(spot);
        }
    }
}
