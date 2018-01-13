package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import java.util.Optional;

/** @author rstumm2s */
@javax.persistence.Entity
@NamedQuery(name = Spot.QUERY_COUNT, query =
    "SELECT COUNT(s) " +
    "FROM Spot s")
@NamedQuery(name = Spot.QUERY_COUNT_TYPE, query =
    "SELECT COUNT(s) " +
    "FROM Spot s " +
    "WHERE s.type = :type")
@NamedQuery(name = Spot.QUERY_COUNT_FREE, query =
    "SELECT COUNT(t) " +
    "FROM spot_state t " +
    "WHERE t.vehicle IS NULL")
@NamedQuery(name = Spot.QUERY_COUNT_FREE_TYPE, query =
    "SELECT COUNT(s) " +
    "FROM Spot s, spot_state t " +
    "WHERE s.type = :type" +
    " AND t.spot = s" +
    " AND t.vehicle IS NULL")
@NamedQuery(name = Spot.QUERY_COUNT_USED, query =
    "SELECT COUNT(t) " +
    "FROM spot_state t " +
    "WHERE t.vehicle IS NOT NULL")
@NamedQuery(name = Spot.QUERY_COUNT_USED_TYPE, query =
    "SELECT COUNT(s) " +
    "FROM Spot s, spot_state t " +
    "WHERE s.type = :type" +
    " AND t.spot = s" +
    " AND t.vehicle IS NOT NULL")
@NamedQuery(name = Spot.QUERY_ANY_FREE, query =
    "SELECT s " +
    "FROM Spot s, spot_state t " +
    "WHERE s.type = :type" +
    " AND t.spot = s" +
    " AND t.vehicle IS NULL")
public class Spot extends Entity {
    static final String
        QUERY_COUNT = "Spot.count",
        QUERY_COUNT_TYPE = "Spot.countType",
        QUERY_COUNT_FREE = "Spot.countFree",
        QUERY_COUNT_FREE_TYPE = "Spot.countFreeType",
        QUERY_COUNT_USED = "Spot.countUsed",
        QUERY_COUNT_USED_TYPE = "Spot.countUsedType",
        QUERY_ANY_FREE = "Spot.anyFree";

    public static long count() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT, Long.class)
            .getSingleResult());
    }

    public static long count(final Type type) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_TYPE, Long.class)
            .setParameter("type", type)
            .getSingleResult());
    }

    public static long countFree() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_FREE, Long.class)
            .getSingleResult());
    }

    public static long countFree(final Type type) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_FREE_TYPE, Long.class)
            .setParameter("type", type)
            .getSingleResult());
    }

    public static long countUsed() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_USED, Long.class)
            .getSingleResult());
    }

    public static long countUsed(final Type type) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_COUNT_USED_TYPE, Long.class)
            .setParameter("type", type)
            .getSingleResult());
    }

    public static Optional<Spot> anyFree() {
        for (Type type : Type.values()) {
            final Optional<Spot> spot = anyFree(type);
            if (spot.isPresent()) return spot;
        }
        return Optional.empty();
    }

    public static Optional<Spot> anyFree(final Type type) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_ANY_FREE, Spot.class)
            .setParameter("type", type)
            .setMaxResults(1)
            .getResultStream().findFirst());
    }

    @Id
    @GeneratedValue
    private int id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
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
