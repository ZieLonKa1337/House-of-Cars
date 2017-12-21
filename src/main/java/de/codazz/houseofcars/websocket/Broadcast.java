package de.codazz.houseofcars.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/** @author rstumm2s */
public abstract class Broadcast {
    private static final Logger log = LoggerFactory.getLogger(Broadcast.class);

    protected final Collection<Session> sessions = new ArrayList<>();

    @OnWebSocketConnect
    public void connected(final Session session) {
        log.trace("{} connected to {} broadcast", session.getRemoteAddress(), getClass().getSimpleName());
        synchronized (sessions) {
            sessions.add(session);
        }
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        log.trace("{} closed with {} {}", session.getRemoteAddress(), statusCode, reason);
        synchronized (sessions) {
            sessions.remove(session);
        }
    }

    public void broadcast(final Collection<?> messages) {
        synchronized (sessions) {
            log.trace("{} broadcasting {} messages to {} sessions", getClass().getSimpleName(), messages.size(), sessions.size());
            if (sessions.isEmpty()) return;
            messages.forEach(this::broadcast);
        }
    }

    public void broadcast(final Object message) {
        synchronized (sessions) {
            log.trace("{} broadcasting to {} sessions: {}", getClass().getSimpleName(), sessions.size(), message);
            sessions.forEach(session -> {
                try {
                    send(message, session);
                } catch (final IOException e) {
                    log.error("failed to broadcast to {}, removing. {}", session.getRemoteAddress(), e);
                    sessions.remove(session);
                }
            });
        }
    }

    public static void send(final Object message, final Session session) throws IOException {
        session.getRemote().sendString(message.toString());
    }
}
