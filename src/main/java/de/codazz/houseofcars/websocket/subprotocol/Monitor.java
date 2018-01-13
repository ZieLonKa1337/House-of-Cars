package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.VehicleTransition;
import de.codazz.houseofcars.domain.view.VehicleStatus;
import de.codazz.houseofcars.websocket.Broadcast;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author rstumm2s */
@WebSocket
public class Monitor extends Broadcast {
    public static final Map<String, Object> templateDefaults; static {
        final Map<String, Object> map = new HashMap<>(1);
        map.put("vehicleStates", Vehicle.State.values());
        templateDefaults = Collections.unmodifiableMap(map);
    }

    public static List<VehicleStatus> states() {
        Garage.instance().persistence.refresh(); // TODO remove? move to newUpdate()?
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

        final Vehicle vehicle; {
            final String license = msg.getString("vehicle");
            vehicle = license != null
                ? Garage.instance().persistence.execute(em -> em.find(Vehicle.class, license))
                : null;
        }
        switch (msg.getString("type")) {
            case "set-state":
                assert vehicle != null;
                switch (Vehicle.State.valueOf(msg.getString("state"))) {
                    case LookingForSpot:
                        // TODO choose spot type based on past transitions or in UI
                        Spot.anyFree().ifPresent(spot ->
                            vehicle.state().new EnteredEvent(spot).fire()
                        );
                        break;
                    case Parking:
                        vehicle.state().new ParkedEvent().fire();
                        break;
                    case Leaving:
                        vehicle.state().new FreedEvent().fire();
                        break;
                    case Away:
                        vehicle.state().new LeaveEvent().fire();
                        break;
                    default:
                        assert false;
                }
                break;
            case "pay":
                assert vehicle != null;
                vehicle.state().new PaidEvent().fire();
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
            since,
            price;
        public final Boolean
            paid,
            reminded,
            overdue;

        public VehicleStateMessage(final VehicleStatus it) {
            vehicle_license = it.vehicle().license();
            state = it.state().name();

            since = it.since()
                .map(since -> since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElse(null);

            final VehicleTransition transition = it.vehicle().lastTransition().orElse(null);
            if (transition != null) {
                paid = transition.paid().orElse(null);
                price = transition.priceTemplate().map(Object::toString).orElse(null);
                if (transition.entity().owner().isPresent()) {
                    reminded = transition.reminder()
                        .map(limit -> limit.isBefore(ZonedDateTime.now()))
                        .orElse(false);
                } else {
                    reminded = null;
                }
                if (transition.overdue().isPresent()) {
                    overdue = transition.overdue()
                        .map(limit -> limit.isBefore(ZonedDateTime.now()))
                        .orElse(false);
                } else {
                    overdue = null;
                }
            } else {
                paid = null;
                price = null;
                reminded = null;
                overdue = null;
            }
        }
    }
}
