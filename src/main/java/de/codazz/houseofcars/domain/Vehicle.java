package de.codazz.houseofcars.domain;

import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQueries({
    @NamedQuery(name = "Vehicle.countPresent", query = "SELECT COUNT(v) FROM Vehicle v WHERE v.present = TRUE"),
    @NamedQuery(name = "Vehicle.mayEnter", query = "SELECT COUNT(v) = 0 FROM Vehicle v WHERE v.license = :license AND v.present = TRUE")
})
public class Vehicle extends Entity {
    @Id
    private String license;

    private boolean present;

    /** @deprecated only for JPA */
    @Deprecated
    public Vehicle() {}

    public Vehicle(final String license) {
        this.license = license;
    }

    public String license() {
        return license;
    }

    /** @return whether this vehicle is in the garage */
    public boolean present() {
        return present;
    }

    public void present(final boolean present) {
        this.present = present;
    }
}
