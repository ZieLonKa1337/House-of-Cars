package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/** @author rstumm2s */
@javax.persistence.Entity
public class Spot extends Entity {
    @Id
    @GeneratedValue
    @Column(nullable = false)
    private int id;

    @Column(nullable = false)
    private Type type;

    /** @deprecated only for JPA */
    @Deprecated
    public Spot() {}

    public Spot(final Type type) {
        this.type = type;
    }

    /** @return garage-local ID */
    public int id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public enum Type {
        CAR,
        BIKE,
        HANDICAP
    }
}
