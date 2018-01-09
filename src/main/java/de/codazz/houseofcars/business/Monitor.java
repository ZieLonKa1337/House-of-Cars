package de.codazz.houseofcars.business;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.VehicleTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/** @author rstumm2s */
public class Monitor implements Runnable, Closeable {
    private static final Logger log = LoggerFactory.getLogger(Monitor.class);

    private final Timer timer = new Timer("Monitor", true);

    private final Consumer<VehicleTransition> startTimers = transition -> {
        transition.vehicle().owner().ifPresent(owner -> transition.reminder()
            // XXX timers expiring when the system is shut down are lost => persistent queue?
            .filter(it -> it.isAfter(ZonedDateTime.now()))
            .ifPresent(limit -> {
                log.trace("will remind {} to pick up {} at {}", owner.getName(), transition.vehicle().license(), limit);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        log.trace("reminding {} to pick up {}", owner.getName(), transition.vehicle().license());
                        de.codazz.houseofcars.websocket.subprotocol.Monitor.update();
                    }
                }, Date.from(limit.toInstant()));
            })
        );

        transition.overdue()
            .filter(it -> it.isAfter(ZonedDateTime.now()))
            .ifPresent(limit -> {
                log.trace("{} must free its spot until {}", transition.vehicle().license(), limit);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        log.warn("{} is overdue!", transition.vehicle().license());
                        de.codazz.houseofcars.websocket.subprotocol.Monitor.update();
                    }
                }, Date.from(limit.toInstant()));
            });
    };

    @Override
    public void run() {
        log.debug("starting timers");
        Garage.instance().persistence.execute(em ->
            em.createQuery(
                "SELECT t " +
                "FROM vehicle_state vs, VehicleTransition t " +
                "WHERE t.time = vs.since",
                VehicleTransition.class
            ).getResultList()
        ).forEach(startTimers);
    }

    @Override
    public void close() {
        timer.cancel();
    }

    public void postPersist(final VehicleTransition transition) {
        startTimers.accept(transition);
    }
}
