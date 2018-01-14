package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.VehicleTransitionListener;
import de.codazz.houseofcars.service.Monitor;
import de.codazz.houseofcars.template.Price;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Function;

/** @author rstumm2s */
@javax.persistence.Entity(name = "VehicleTransition")
@NamedQuery(name = VehicleTransition.QUERY_SINCE, query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time >= :time")
@NamedQuery(name = VehicleTransition.QUERY_NEXT, query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time > :time" +
    " AND t.entity = :entity " +
    "ORDER BY t.time")
@NamedQuery(name = VehicleTransition.QUERY_PREVIOUS, query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time < :time" +
    " AND t.entity = :entity " +
    "ORDER BY t.time DESC")
@EntityListeners(VehicleTransitionListener.class)
public class VehicleTransition extends StatefulEntityTransition<VehicleTransition, Vehicle, Vehicle.Event, Vehicle.State, Vehicle.State.Data> {
    final static String
        QUERY_NEXT     = "VehicleTransition.next",
        QUERY_PREVIOUS = "VehicleTransition.previous",
        QUERY_SINCE    = "VehicleTransition.since";

    public static TypedQuery<Transition> since(final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_SINCE, Transition.class)
            .setParameter("time", time));
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Vehicle.State state;

    @Transient
    private transient BigDecimal price;

    @Transient
    private transient VehicleTransition pricedSince;

    /** @deprecated only for JPA */
    @Deprecated
    protected VehicleTransition() {
        super(VehicleTransition.class, QUERY_NEXT, QUERY_PREVIOUS);
    }

    protected VehicleTransition(final Vehicle vehicle, final Vehicle.State state, final Vehicle.State.Data data) {
        super(
            VehicleTransition.class, QUERY_NEXT, QUERY_PREVIOUS,
            vehicle, data
        );
        this.state = state;
    }

    @PostLoad
    private void init() {
        if (data == null) {
            data = new Vehicle.State.Data();
        }
    }

    @Override
    public Vehicle.State state() {
        return state;
    }

    public Optional<BigDecimal> fee() {
        return Optional.ofNullable(data.fee);
    }

    public Optional<Boolean> paid() {
        return Optional.ofNullable(data.paid);
    }

    /** Sums the prices (fee * duration) of past
     * transitions of this vehicle that have a fee,
     * up to the first encountered where a payment was
     * completed (that is {@code paid = TRUE}).
     * @return the current price to pay (in order to settle)
     *     or empty if no current payment exists */
    // XXX could this be done by views or queries?
    public Optional<BigDecimal> price() {
        // TODO adapt so that current price can be calculated while still parking (but don't break usages)
        if (data.paid == null) return Optional.empty();

        if (price != null) {
            return Optional.of(price);
        }

        class EndHolder {
            /** time of the current next transition (to calculate duration) */
            ZonedDateTime end = time();
        }
        final EndHolder end = new EndHolder();

        final Function<VehicleTransition, BigDecimal> transitionPrice = it -> it.data.fee.multiply(
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

        pricedSince = it;
        return Optional.of(this.price = price);
    }

    /** All transitions in between (inclusive)
     * are included in the {@link #price()}.
     * @return the last transition included in
     * the {@link #price()} calculation */
    public Optional<VehicleTransition> pricedSince() {
        price(); // ensure pricedSince is set
        return Optional.ofNullable(pricedSince);
    }

    @Transient
    private transient volatile Price priceTemplate;

    public Optional<Price> priceTemplate() {
        return price().map(value -> {
            if (priceTemplate == null) {
                priceTemplate = new Price(value);
            }
            return priceTemplate;
        });
    }

    /** @return when the reminder timer expires, if any */
    public Optional<ZonedDateTime> reminder() {
        return Optional.ofNullable(data.reminder)
            .map(it -> time().plus(it));
    }

    /** @return when the limit timer expires, if any */
    public Optional<ZonedDateTime> overdue() {
        return Optional.ofNullable(data.limit)
            .map(it -> time().plus(it));
    }

    @Transient
    private transient volatile de.codazz.houseofcars.template.ZonedDateTime overdueTemplate;

    public Optional<de.codazz.houseofcars.template.ZonedDateTime> overdueTemplate() {
        return overdue().map(value -> {
            if (overdueTemplate == null) {
                overdueTemplate = new de.codazz.houseofcars.template.ZonedDateTime(value);
            }
            return overdueTemplate;
        });
    }
}
