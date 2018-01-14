package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** @author rstumm2s */
@NamedQuery(name = Customer.QUERY_RESERVATIONS, query =
    "SELECT r " +
    "FROM Reservation r " +
    "WHERE r.end >= CURRENT_TIMESTAMP" +
    " AND r.customer = :customer " +
    "ORDER BY r.start, r.end")
@javax.persistence.Entity
public class Customer extends Entity implements Principal {
    static final String QUERY_RESERVATIONS = "Customer.reservations";

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false, length = 60)
    private String pass;

    @OneToMany(mappedBy = "owner")
    private Set<Vehicle> vehicles = Collections.emptySet();

    /** @deprecated only for JPA */
    @Deprecated
    protected Customer() {}

    public Customer(final String pass, final Vehicle... vehicles) {
        pass(pass);
        for (final Vehicle vehicle : vehicles) {
            if (vehicle.owner().isPresent())
                throw new IllegalStateException(vehicle.license() + " already has owner " + vehicle.owner().get().id);
            vehicle.owner(this);
        }
    }

    public long id() {
        return id;
    }

    public String pass() {
        return pass;
    }

    public void pass(final String pass) {
        if (pass.length() == 0) throw new IllegalArgumentException();
        this.pass = Objects.requireNonNull(pass);
    }

    public Set<Vehicle> vehicles() {
        return Collections.unmodifiableSet(vehicles);
    }

    public Vehicle[] lruVehicles() {
        return vehicles().stream()
            .sorted((a, b) -> {
                final VehicleTransition
                    tA = a.lastTransition().orElse(null),
                    tB = b.lastTransition().orElse(null);
                if (tA == null && tB == null) return 0;
                if (tA == null) return -1;
                if (tB == null) return 1;
                return tB.time().compareTo(tA.time());
            })
            .toArray(Vehicle[]::new);
    }

    public List<Reservation> reservations() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(QUERY_RESERVATIONS, Reservation.class)
            .setParameter("customer", this)
            .getResultList());
    }

    /** @return the {@link #id} */
    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        final Customer customer = (Customer) o;
        return id == customer.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
