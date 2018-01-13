package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQuery;

/** @author rstumm2s */
@NamedQuery(name = ReservationTransition.QUERY_NEXT, query =
    "SELECT t " +
    "FROM ReservationTransition t " +
    "WHERE t.time >= :time" +
    " AND t.entity = :entity " +
    "ORDER BY t.time")
@NamedQuery(name = ReservationTransition.QUERY_PREVIOUS, query =
    "SELECT t " +
    "FROM ReservationTransition t " +
    "WHERE t.time >= :time" +
    " AND t.entity = :entity " +
    "ORDER BY t.time")
@Entity
public class ReservationTransition extends StatefulEntityTransition<ReservationTransition, Reservation, Reservation.Event, Reservation.State, Void> {
    static final String
        QUERY_NEXT     = "ReservationTransition.next",
        QUERY_PREVIOUS = "ReservationTransition.previous";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Reservation.State state;

    /** @deprecated only for JPA */
    @Deprecated
    protected ReservationTransition() {
        super(ReservationTransition.class, QUERY_NEXT, QUERY_PREVIOUS);
    }

    public ReservationTransition(final Reservation reservation, final Reservation.State state) {
        super(
            ReservationTransition.class, QUERY_NEXT, QUERY_PREVIOUS,
            reservation, null
        );
        this.state = state;
    }

    @Override
    public Reservation.State state() {
        return state;
    }
}
