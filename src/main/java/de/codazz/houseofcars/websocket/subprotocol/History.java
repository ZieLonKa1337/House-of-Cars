package de.codazz.houseofcars.websocket.subprotocol;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.VehicleTransition;
import de.codazz.houseofcars.websocket.Broadcast;
import de.codazz.houseofcars.websocket.TypedMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.TypedQuery;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** @author rstumm2s */
@WebSocket
public class History extends Broadcast {
    public static final Map<String, Object> templateDefaults; static {
        final Map<String, Object> map = new HashMap<>(1);
        map.put("vehicleStates", Vehicle.State.values());
        templateDefaults = Collections.unmodifiableMap(map);
    }

    private static volatile History instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static void close() {
        instance = null;
    }

    private static final Logger log = LoggerFactory.getLogger(History.class);

    private final Graph graph;

    public History() {
        graph = new Graph(
            Arrays.stream(Vehicle.State.values())
                .map(state -> new Dataset(state.name()))
                .toArray(Dataset[]::new));
        update();
    }

    @Override
    public void connected(final Session session) {
        try {
            send(graph, session);
            super.connected(session);
        } catch (final IOException ignore) {}
    }

    /** updates internal state from the database
     * and broadcasts the latest updates */
    public static void update() {
        final Optional<ZonedDateTime> updated;
        synchronized (instance.graph) {
            updated = instance.graph.updated();
        }
        log.trace("updating. last updated {}", updated.isPresent() ? updated.get() : "never");

        final List<GraphUpdate> updates; {
            final StringBuilder jpql = new StringBuilder("SELECT t FROM VehicleTransition t\n"); // XXX gc
            updated.ifPresent(__ -> jpql.append("WHERE t.time > :time\n"));

            updates = Garage.instance().persistence.execute(em -> {
                final TypedQuery<VehicleTransition> query = em.createQuery(jpql.append("ORDER BY t.time").toString(), VehicleTransition.class);
                updated.ifPresent(time -> query.setParameter("time", time));
                return query.getResultList();
            }).stream()
                .map(transition -> {
                    // update all states after every transition
                    // TODO send only one message with all states
                    final GraphUpdate[] transitionUpdates = new GraphUpdate[Vehicle.State.values().length];
                    for (final Vehicle.State state : Vehicle.State.values()) {
                        transitionUpdates[state.ordinal()] = new GraphUpdate(state.name(), new Datapoint(
                            transition.time(), Vehicle.count(state, transition.time())
                        ));
                    }
                    return transitionUpdates;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        }

        synchronized (instance.graph) {
            updates.forEach(it -> instance.graph.update(it.dataset, it.datapoint));
        }
        log.debug("broadcasting {} updates", updates.size());
        instance.broadcast(updates);
    }

    private static class GraphUpdate extends TypedMessage {
        final String dataset;
        final Datapoint datapoint;

        public GraphUpdate(final String dataset, final Datapoint datapoint) {
            super("graph-update");
            this.dataset = dataset;
            this.datapoint = datapoint;
        }
    }

    private static class Graph extends TypedMessage {
        public Graph(final Dataset... datasets) {
            super("graph");
            this.datasets = datasets;
        }

        final Dataset[] datasets;

        public synchronized void update(final String dataset, final Datapoint update) {
            Arrays.stream(datasets)
                .filter(it -> dataset.equals(it.label))
                .forEach(it -> it.update(update));
        }

        /** @return the time of the last datapoint in this graph */
        public synchronized Optional<ZonedDateTime> updated() {
            return Arrays.stream(datasets)
                .map(it -> it.data)
                .filter(it -> !it.isEmpty())
                .map(it -> it.get(it.size() - 1).t)
                .max(ChronoZonedDateTime::compareTo);
        }
    }

    private static class Dataset {
        final String label;
        final List<Datapoint> data = new ArrayList<>();

        public Dataset(final String label) {
            this.label = label;
        }

        public synchronized void update(final Datapoint update) {
            data.add(update);
        }
    }

    private static class Datapoint {
        final transient ZonedDateTime t;
        final String x;
        final int y;

        public Datapoint(final ZonedDateTime t, final int y) {
            this.t = t;
            this.x = t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.y = y;
        }
    }
}
