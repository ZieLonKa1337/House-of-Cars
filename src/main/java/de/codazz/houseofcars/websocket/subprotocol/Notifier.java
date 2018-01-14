package de.codazz.houseofcars.websocket.subprotocol;

import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @author rstumm2s */
@WebSocket
public class Notifier {
    private static volatile Notifier instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static void close() {
        instance = null;
    }

    private final Map<Session, Subject> sessions = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void connected(final Session session) {
        Customers.subjectFromMagicCookie(session).ifPresent(it -> sessions.put(session, it));
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        sessions.remove(session);
    }

    /** @param person the credential */
    public static void push(final Principal person, final Notification notification) {
        instance.sessions.entrySet().stream()
            .filter(it -> person.implies(it.getValue()))
            .map(Map.Entry::getKey)
            .forEach(session -> {
                try {
                    session.getRemote().sendString(notification.toString());
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public static class Notification extends Message {
        private final String title, body;

        public Notification(final String title, final String body) {
            this.title = title;
            this.body = body;
        }
    }
}
