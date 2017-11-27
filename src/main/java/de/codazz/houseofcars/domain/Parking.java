package de.codazz.houseofcars.domain;

import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.time.ZonedDateTime;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
public class Parking extends Activity {
    @OneToOne(optional = false)
    private LicensePlate licensePlate;

    @OneToOne
    @JoinColumn(unique = true)
    private Spot spot;

    /** @deprecated only for JPA */
    @Deprecated
    public Parking() {}

    public Parking(final LicensePlate licensePlate) {
        super(ZonedDateTime.now());
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
