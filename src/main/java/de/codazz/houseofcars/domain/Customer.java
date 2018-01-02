package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.Set;

/** @author rstumm2s */
@javax.persistence.Entity
public class Customer extends Entity {
    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private byte[] pass = new byte[] {}; // TODO set and check!

    @OneToMany(mappedBy = "owner")
    private Set<Vehicle> vehicles = Collections.emptySet();

    public long id() {
        return id;
    }

    public Set<Vehicle> vehicles() {
        return Collections.unmodifiableSet(vehicles);
    }
}
