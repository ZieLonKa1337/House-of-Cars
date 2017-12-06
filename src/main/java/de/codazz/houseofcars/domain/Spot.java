package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQueries({
        @NamedQuery(name = "Spot.count", query = "SELECT COUNT(s) FROM Spot s"),
        @NamedQuery(name = "Spot.countType", query = "SELECT COUNT(s) FROM Spot s WHERE s.type = :type"),
        @NamedQuery(name = "Spot.countUsed", query = "SELECT COUNT(p) FROM Parking p WHERE p.finished IS NULL AND p.spot IS NOT NULL AND p.parked IS NOT NULL"),
        @NamedQuery(name = "Spot.countUsedType", query = "SELECT COUNT(p) FROM Parking p WHERE p.finished IS NULL AND p.spot.type = :type"),
        @NamedQuery(name = "Spot.anyFree", query = "SELECT s FROM Spot s WHERE s.type = :type AND s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.spot IS NOT NULL)")
})
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
