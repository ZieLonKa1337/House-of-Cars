package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Config;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.VehicleTransitionListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
@NamedQuery(name = "VehicleTransition.next", query =
    "SELECT t FROM VehicleTransition t " +
    "WHERE t.time > :time" +
    " AND t.vehicle = :vehicle " +
    "ORDER BY t.time")
@EntityListeners(VehicleTransitionListener.class)
public class VehicleTransition extends de.codazz.houseofcars.domain.Transition<Vehicle.Event, Vehicle.State, Vehicle.State.Data> {
    public static TypedQuery<Transition> since(final ZonedDateTime time) {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.since", Transition.class)
            .setParameter("time", time));
    }

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Vehicle.State state;

    @Transient
    private transient BigDecimal price;

    @Transient
    private transient VehicleTransition pricedSince;

    /** @deprecated only for JPA */
    @Deprecated
    protected VehicleTransition() {}

    protected VehicleTransition(final Vehicle vehicle, final Vehicle.State state, final Vehicle.State.Data data) {
        super(data);
        this.vehicle = vehicle;
        this.state = state;
    }

    @PostLoad
    private void init() {
        if (data == null) {
            data = new Vehicle.State.Data();
        }
    }

    @Override
    public Duration duration() {
        return Duration.between(
            time(),
            next().map(VehicleTransition::time)
                .orElseGet(ZonedDateTime::now)
        );
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    @Override
    public Vehicle.State state() {
        return state;
    }

    /** @return the associated vehicle's
     *     previous transition, if any */
    public Optional<VehicleTransition> previous() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.previous", VehicleTransition.class)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("vehicle", vehicle)
            .getResultStream().findFirst());
    }

    /** @return the associated vehicle's
     *     next transition, if any */
    public Optional<VehicleTransition> next() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery("VehicleTransition.next", VehicleTransition.class)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("vehicle", vehicle)
            .getResultStream().findFirst());
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

    /** @return the actual price to bill */
    public Optional<String> billPrice() {
        final Config.Currency currency = Garage.instance().config.currency();
        return price()
            .map(it -> it.setScale(currency.scale(), RoundingMode.DOWN))
            .map(BigDecimal::toPlainString)
            .map(it -> it + currency.name());
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
}
