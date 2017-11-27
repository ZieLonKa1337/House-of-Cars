package de.codazz.houseofcars.domain;

import javax.persistence.Id;

/** @author rstumm2s */
@javax.persistence.Entity
public class LicensePlate extends Entity {
    @Id
    private String code;

    /** @deprecated only for JPA */
    @Deprecated
    public LicensePlate() {}

    public LicensePlate(final String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
