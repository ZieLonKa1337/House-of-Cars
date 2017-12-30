package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.view.VehicleStatus;
import de.codazz.houseofcars.websocket.Broadcast;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** @author rstumm2s */
@WebSocket
public class Monitor extends Broadcast {
    private static final Logger log = LoggerFactory.getLogger(Monitor.class);

    public static List<VehicleStatus> states() {
        Garage.instance().persistence.refresh();
        return Garage.instance().persistence.execute(em -> em
            .createQuery("SELECT vs FROM vehicle_state vs ORDER BY vs.since DESC", VehicleStatus.class)
            .getResultList());
    }

    private static volatile Monitor instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static void close() {
        instance = null;
    }

    private static final JsonReader jsonReader = new JsonReader();

    @Override
    public void connected(final Session session) {
        try {
            send(newUpdate(), session);
            super.connected(session);
        } catch (final IOException ignore) {}
    }

    @OnWebSocketMessage
    public void message(final String message) {
        final JsonValue msg = jsonReader.parse(message);
        switch (msg.getString("type")) {
            case "set-state":
                final Vehicle vehicle = Garage.instance().persistence.execute(em -> em.find(Vehicle.class, msg.getString("vehicle")));
                try {
                    switch (Vehicle.State.valueOf(msg.getString("state"))) {
                        case LookingForSpot:
                            // TODO choose spot type based on past transitions or in UI
                            final Optional<Spot> spot = Spot.anyFree();
                            if (spot.isPresent()) {
                                vehicle.state().new EnteredEvent(spot.get()).fire();
                            }
                            break;
                        case Parking:
                            vehicle.state().new ParkedEvent().fire();
                            break;
                        case Leaving:
                            vehicle.state().new LeftSpotEvent().fire();
                            break;
                        case Away:
                            vehicle.state().new LeftEvent().fire();
                            break;
                        default:
                            assert false;
                    }
                } catch (final InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    log.error("failed to modify vehicle state", e);
                }
//                update(); // TODO remove and check
                break;
        }
    }

    public static void update() {
        instance.broadcast(newUpdate());
    }

    private static String newUpdate() {
        return Message.json.get().toJson(
            states().stream()
                .map(VehicleStateMessage::new)
                .toArray(VehicleStateMessage[]::new)
        );
    }

    private static class VehicleStateMessage extends Message {
        public final String
            vehicle_license,
            state,
            since;

        public VehicleStateMessage(final VehicleStatus it) {
            vehicle_license = it.vehicleLicense();
            state = it.state().name();
            since = it.since().map(zdt -> zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).orElse(null);
        }
    }
}
