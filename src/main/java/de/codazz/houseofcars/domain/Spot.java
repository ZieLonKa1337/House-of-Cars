package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = "Spot.count", query =
    "SELECT COUNT(s) FROM Spot s")
@NamedQuery(name = "Spot.countType", query =
    "SELECT COUNT(s) FROM Spot s WHERE s.type = :type")
@NamedQuery(name = "Spot.countFree", query =
    "SELECT COUNT(s) FROM Spot s WHERE s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.parked IS NOT NULL AND p.freed IS NULL)")
@NamedQuery(name = "Spot.countFreeType", query =
    "SELECT COUNT(s) FROM Spot s WHERE s.type = :type AND s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.parked IS NOT NULL AND p.freed IS NULL)")
@NamedQuery(name = "Spot.countUsed", query =
    "SELECT COUNT(p) FROM Parking p WHERE p.freed IS NULL")
@NamedQuery(name = "Spot.countUsedType", query =
    "SELECT COUNT(p) FROM Parking p WHERE p.freed IS NULL AND p.spot.type = :type")
@NamedQuery(name = "Spot.anyFree", query =
    "SELECT s FROM Spot s WHERE s.type = :type AND s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.parked IS NOT NULL AND p.freed IS NULL)")
public class Spot extends Entity {
    public static long count() {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.count", Long.class)
            .getSingleResult();
    }

    public static long count(final Type type) {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.countType", Long.class)
            .setParameter("type", type)
            .getSingleResult();
    }

    public static long countFree() {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.countFree", Long.class)
            .getSingleResult();
    }

    public static long countFree(final Type type) {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.countFreeType", Long.class)
            .setParameter("type", type)
            .getSingleResult();
    }

    public static long countUsed() {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.countUsed", Long.class)
            .getSingleResult();
    }

    public static long countUsed(final Type type) {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.countUsedType", Long.class)
            .setParameter("type", type)
            .getSingleResult();
    }

    public static Optional<Spot> anyFree(final Type type) {
        return Garage.instance().persistence.execute(em -> em)
            .createNamedQuery("Spot.anyFree", Spot.class)
            .setParameter("type", type)
            .setMaxResults(1)
            .getResultStream().findFirst();
    }

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
