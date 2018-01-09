package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.VehicleTransition;

import javax.persistence.PostPersist;

/** @author rstumm2s */
public class VehicleTransitionListener {
    @PostPersist
    public void postPersist(final VehicleTransition transition) {
        Garage.instance().monitor.postPersist(transition);
    }
}
