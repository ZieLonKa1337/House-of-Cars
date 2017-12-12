package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.statemachine.StateMachineException;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
@WebSocket
public class Gate {
    private final Map<Session, de.codazz.houseofcars.business.Gate> states = new HashMap<>();

    @OnWebSocketConnect
    public void connected(final Session session) throws StateMachineException {
        final de.codazz.houseofcars.business.Gate state = new de.codazz.houseofcars.business.Gate(false);
        state.start();
        synchronized (states) {
            states.put(session, state);
        }
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        synchronized (states) {
            states.remove(session);
        }
    }

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException, StateMachineException {
        final de.codazz.houseofcars.business.Gate state;
        synchronized (states) {
            state = states.get(session);
        }
        final Message response = handle(state, new JsonReader().parse(message));
        if (response != null) {
            session.getRemote().sendString(response.toJson());
        }
    }

    protected Message handle(final de.codazz.houseofcars.business.Gate state, final JsonValue msg) throws StateMachineException {
        switch (msg.getString("type")) {
            case "open-request":
                return new OpenResponse(state.onEvent(msg.getString("license")).get());
            case "entered":
                state.onEvent(msg.getString("license"));
                return null;
            default:
                return null;
        }
    }

    private class OpenResponse extends Message {
        final boolean permission;

        public OpenResponse(final boolean permission) {
            super("open-response");
            this.permission = permission;
        }
    }
}
