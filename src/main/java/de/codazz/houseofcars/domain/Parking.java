package de.codazz.houseofcars.domain;

import javax.persistence.OneToOne;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
public class Parking extends Activity {
    @OneToOne(optional = false)
    private LicensePlate licensePlate;

    @OneToOne
    private Spot spot;

    /** @deprecated only for JPA */
    @Deprecated
    public Parking() {}

    public Parking(final LicensePlate licensePlate) {
        this.licensePlate = licensePlate;
    }

    public LicensePlate licensePlate() {
        return licensePlate;
    }

    public Optional<Spot> spot() {
        return Optional.ofNullable(spot);
    }

    /** @param spot the spot occupied by the driver */
    public void spot(final Spot spot) {
        this.spot = spot;
    }
}
