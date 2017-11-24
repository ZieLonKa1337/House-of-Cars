package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/** @author rstumm2s */
@Entity
public class Spot extends de.codazz.houseofcars.domain.Entity implements de.codazz.houseofcars.Spot {
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

    @Override
    public int id() {
        return id;
    }

    @Override
    public Type type() {
        return type;
    }
}
