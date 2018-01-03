package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** @author rstumm2s */
@javax.persistence.Entity
public class Customer extends Entity implements Principal {
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

    /** @return the {@link #id} */
    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
