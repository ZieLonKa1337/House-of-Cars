package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.Objects;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Spot.count", query = "SELECT COUNT(s) FROM Spot s")
@NamedQuery(name = "Spot.countType", query = "SELECT COUNT(s) FROM Spot s WHERE s.type = :type")
@NamedQuery(name = "Spot.countUsed", query = "SELECT COUNT(p) FROM Parking p WHERE p.freed IS NULL")
@NamedQuery(name = "Spot.countUsedType", query = "SELECT COUNT(p) FROM Parking p WHERE p.freed IS NULL AND p.spot.type = :type")
@NamedQuery(name = "Spot.anyFree", query = "SELECT s FROM Spot s WHERE s.type = :type AND s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.parked IS NOT NULL AND p.freed IS NULL)")
public class Spot extends Entity {
    @Id
    @GeneratedValue
    @Column(nullable = false)
    private int id;

    @Column(nullable = false)
    private Type type;

    /** @deprecated only for JPA */
    @Deprecated
    protected Spot() {}

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Spot)) return false;
        final Spot spot = (Spot) o;
        return id == spot.id &&
            type == spot.type;
    }

    public enum Type {
        CAR,
        BIKE,
        HANDICAP
    }
}
