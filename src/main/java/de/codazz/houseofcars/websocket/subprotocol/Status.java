package de.codazz.houseofcars.websocket.subprotocol;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.websocket.Broadcast;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
@WebSocket
public class Status extends Broadcast {
    private static volatile Status instance; {
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

    /** broadcasts an update to {@code /ws/status}
     * and all below paths {@code /ws/status/*}
     * @see History#update() */
    public static void update() {
        instance.broadcast(instance.newUpdate());
        History.update();
    }

    protected Message newUpdate() {
        return new Message("update") {
            final Map<String, Long> values = new HashMap<>(1 + Spot.Type.values().length, 1); {
                values.put("numFree", Spot.countFree());
                for (final Spot.Type type : Spot.Type.values()) {
                    values.put("numFree-" + type.name(), Spot.countFree(type));
                }
            }
        };
    }
}
