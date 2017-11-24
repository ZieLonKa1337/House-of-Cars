package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/** @author rstumm2s */
@MappedSuperclass
public abstract class Entity {
    @Version
    @Column(name = "OPTLOCK", nullable = false)
    private long version;
}
