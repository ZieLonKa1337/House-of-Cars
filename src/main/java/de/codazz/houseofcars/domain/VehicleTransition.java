package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** @author rstumm2s */
@javax.persistence.Entity(name = "VehicleTransition")
@NamedQuery(name = "VehicleTransition.since", query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time >= :time")
@NamedQuery(name = "VehicleTransition.previous", query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time < :time" +
    " AND t.vehicle = :vehicle " +
    "ORDER BY t.time DESC")
public class VehicleTransition extends de.codazz.houseofcars.domain.Transition<Vehicle.Event, Vehicle.State, Vehicle.State.Data> {
    public static TypedQuery<Transition> since(final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.since", Transition.class)
            .setParameter("time", time));
    }

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String state;

    @Transient
    private transient Vehicle.State stateInstance;

    /** @deprecated only for JPA */
    @Deprecated
    protected VehicleTransition() {}

    protected VehicleTransition(final Vehicle vehicle, final Vehicle.State state, final Vehicle.State.Data data) {
        super(data);
        this.vehicle = vehicle;
        this.state = state.name();
        stateInstance = state;
    }

    @PostLoad
    private void init() {
        if (data == null) {
            data = new Vehicle.State.Data();
        }
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    @Override
    public Vehicle.State state() {
        if (stateInstance == null) {
            stateInstance = Vehicle.State.valueOf(state);
        }
        return stateInstance;
    }

    /** @return the associated vehicle's
     * previous transition, if any */
    public Optional<VehicleTransition> previous() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.previous", VehicleTransition.class)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("vehicle", vehicle)
            .getResultStream().findFirst());
    }

    /** Sums the prices (fee with duration) of all transitions
     * of this vehicle that have a fee, including the previous
     * up to the first prior transition where a payment was
     * completed (that is {@code paid = TRUE}).
     * @return the current price to pay (in order to settle)
     *     or {@code null} if no current payment exists */
    // XXX could this be done by a view or query?
    public Optional<BigDecimal> price() {
        if (data.paid == null) return Optional.empty();

        class EndHolder {
            /** time of the current next transition (to calculate duration) */
            ZonedDateTime end = time();
        }
        final EndHolder end = new EndHolder();

        final Function<VehicleTransition, BigDecimal> transitionPrice = it ->
            it.data.fee.multiply(
                BigDecimal.valueOf( // duration
                    ((double) it.time().until(end.end, ChronoUnit.SECONDS))
                    / 60 // minutes
                    / 60 // hours, our base pricing unit
                )
            );

        VehicleTransition it = previous().orElseThrow(() -> new AssertionError("no previous transition"));

        if (it.data.paid != null && it.data.paid) {
            /* if this payment is already finished
             * still calculate the price paid
             * by skipping previous paid transitions if any
             * so we don't stop immediately below */
            while (it.data.paid != null && it.data.paid) {
                end.end = it.time();
                it = it.previous().orElseThrow(() -> new AssertionError("cannot have paid for nothing"));
            }
        }

        // sum all fees up to the last payment
        BigDecimal price = BigDecimal.ZERO;
        while (it != null && (it.data.paid == null || !it.data.paid)) {
            if (it.data.fee != null) {
                price = price.add(transitionPrice.apply(it));
            }
            end.end = it.time();
            it = it.previous().orElse(null);
        }
        return Optional.of(price);
    }
}
