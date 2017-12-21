package de.codazz.houseofcars.domain.view;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.ZonedDateTime;

/** @author rstumm2s */
@Entity(name = "parking")
final class SpotStatus {
    @Id
    private int spot_id;
    private String vehicle_license;
    private ZonedDateTime since;

    protected SpotStatus() {}
}
