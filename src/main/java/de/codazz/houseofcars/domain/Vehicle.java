package de.codazz.houseofcars.domain;

import javax.persistence.Id;

/** @author rstumm2s */
@javax.persistence.Entity
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
