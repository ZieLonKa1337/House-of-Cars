package de.codazz.houseofcars.websocket.subprotocol;

import de.codazz.houseofcars.GarageImpl;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/** @author rstumm2s */
@WebSocket
public class Status {
    private static final Collection<Session> sessions = new ArrayList<>();

    @OnWebSocketConnect
    public void connected(final Session session) throws IOException {
        update(session);
        synchronized (sessions) {
            sessions.add(session);
        }
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        synchronized (sessions) {
            sessions.remove(session);
        }
    }

    public static void update() {
        final Message update = newUpdate();
        for (final Session session : sessions) {
            try {
                session.getRemote().sendString(update.toJson());
            } catch (final IOException ignore) {}
        }
    }

    private static void update(final Session session) throws IOException {
        session.getRemote().sendString(newUpdate().toJson());
    }

    private static Message newUpdate() {
        return new Message("update") {
            final Map<String, Integer> values = new HashMap<>(); {
                values.put("numFree", GarageImpl.instance().numFree());
                for (final Spot.Type type : Spot.Type.values()) {
                    values.put("numFree-" + type.name(), GarageImpl.instance().numFree(type));
                }
            }
        };
    }
}
