package de.codazz.houseofcars.service;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.VehicleTransition;
import de.codazz.houseofcars.websocket.subprotocol.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** @author rstumm2s */
public class Monitor implements Runnable, Closeable {
    private static final Logger log = LoggerFactory.getLogger(Monitor.class);

    private final Timer timer = new Timer("Monitor", true);
    private final Map<VehicleTransition, Set<TimerTask>> tasks = new ConcurrentHashMap<>(16, .9f, 1);
    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private final Consumer<VehicleTransition> startTimers = transition -> {
        transition.entity().owner().ifPresent(owner -> transition.reminder()
            // XXX timers expiring when the system is shut down are lost => persistent queue?
            .filter(it -> it.isAfter(ZonedDateTime.now()))
            .ifPresent(limit -> {
                log.trace("will remind {} to pick up {} at {}", owner.getName(), transition.entity().license(), limit);
                timer.schedule(new TimerTask(transition, () -> {
                    log.trace("reminding {} to pick up {}", owner.getName(), transition.entity().license());
                    de.codazz.houseofcars.websocket.subprotocol.Monitor.update();
                    Notifier.push(owner, new Notifier.Notification(
                        transition.entity().license() + "'s spot expires " + de.codazz.houseofcars.template.ZonedDateTime.toString(limit),
                        "Pick up before limit to avoid extra cost"
                    ));
                }), Date.from(limit.toInstant()));
            })
        );

        transition.overdue()
            .filter(it -> it.isAfter(ZonedDateTime.now()))
            .ifPresent(limit -> {
                log.trace("{} must free its spot until {}", transition.entity().license(), limit);
                final TimerTask task = new TimerTask(transition, () -> {
                    log.warn("{} is overdue!", transition.entity().license());
                    de.codazz.houseofcars.websocket.subprotocol.Monitor.update();
                    transition.entity().owner().ifPresent(owner ->
                        Notifier.push(owner, new Notifier.Notification(
                            transition.entity().license() + " is overdue!",
                            "Should have picked up until " + de.codazz.houseofcars.template.ZonedDateTime.toString(limit)
                        ))
                    );
                });
                timer.schedule(task, Date.from(limit.toInstant()));
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

    /** @see javax.persistence.PostPersist */
    public void postPersist(final VehicleTransition transition) {
        scheduler.execute(() -> {
            Garage.instance().persistence.refresh();

            // cancel current timers
            transition.previous().ifPresent(previous -> {
                final Set<TimerTask> tasks = this.tasks.remove(previous);
                if (tasks != null) {
                    log.trace("cancelling timers from {} at {}", transition.entity().license(), transition.time());
                    tasks.forEach(TimerTask::cancel);
                    tasks.clear();
                }
            });

            startTimers.accept(transition);
        });
    }

    private final class TimerTask extends java.util.TimerTask {
        final VehicleTransition origin;
        final Runnable task;

        public TimerTask(final VehicleTransition origin, final Runnable task) {
            this.origin = origin;
            this.task = task;

            tasks.compute(origin, (__, tasks) -> {
                if (tasks == null) {
                    tasks = Collections.synchronizedSet(new HashSet<>());
                }
                tasks.add(this);
                return tasks;
            });
        }

        @Override
        public void run() {
            task.run();
            tasks.compute(origin, (__, tasks) -> {
                assert tasks != null;
                tasks.remove(this);
                if (tasks.isEmpty()) {
                    tasks = null;
                }
                return tasks;
            });
        }
    }
}
