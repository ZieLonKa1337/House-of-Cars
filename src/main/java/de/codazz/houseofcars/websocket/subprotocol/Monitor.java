package de.codazz.houseofcars.websocket.subprotocol;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.view.VehicleStatus;
import de.codazz.houseofcars.websocket.Broadcast;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** @author rstumm2s */
@WebSocket
public class Monitor extends Broadcast {
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

    @Override
    public void connected(final Session session) {
        try {
            send(newUpdate(), session);
            super.connected(session);
        } catch (final IOException ignore) {}
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
