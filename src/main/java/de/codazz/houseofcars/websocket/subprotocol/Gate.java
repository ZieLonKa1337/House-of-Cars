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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @author rstumm2s */
@WebSocket
public class Gate {
    private static final Logger log = LoggerFactory.getLogger(Gate.class);

    private static final JsonReader jsonReader = new JsonReader();

    private final Map<Session, de.codazz.houseofcars.business.Gate> states = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void connected(final Session session) throws StateMachineException {
        log.debug("{} connected", session.getRemoteAddress());
        final de.codazz.houseofcars.business.Gate state = new de.codazz.houseofcars.business.Gate(false);
        state.start();
        states.put(session, state);
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        log.debug("{} closed with {} {}", session.getRemoteAddress(), statusCode, reason);
        states.remove(session);
    }

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException, StateMachineException {
        log.trace("{}: {}", session.getRemoteAddress(), message);
        final Message response = handle(jsonReader.parse(message), states.get(session));
        if (response != null) {
            session.getRemote().sendString(response.toJson());
        }
    }

    protected Message handle(final JsonValue msg, final de.codazz.houseofcars.business.Gate state) throws StateMachineException {
        switch (msg.getString("type")) {
            case "open-request":
                assert state.state().getClass().equals(de.codazz.houseofcars.business.Gate.class);
                return new OpenResponse(state.requestOpen(msg.getString("license")));
            case "opened":
                state.onEvent(((de.codazz.houseofcars.business.Gate) state.state()).new OpenedEvent());
                return null;
            case "entered":
                state.onEvent(((de.codazz.houseofcars.business.Gate.Open) state.state()).new EnteredEvent(msg.getString("license")));
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
