package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.websocket.Message;
import de.codazz.houseofcars.websocket.TypedMessage;
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
    public void connected(final Session session) {
        log.debug("{} connected", session.getRemoteAddress());
        final de.codazz.houseofcars.business.Gate state = new de.codazz.houseofcars.business.Gate();
        states.put(session, state);
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        log.debug("{} closed with {} {}", session.getRemoteAddress(), statusCode, reason);
        states.remove(session);
    }

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {
        log.trace("{}: {}", session.getRemoteAddress(), message);
        final Message response = handle(jsonReader.parse(message), states.get(session));
        if (response != null) {
            session.getRemote().sendString(response.toJson());
        }
    }

    protected Message handle(final JsonValue msg, final de.codazz.houseofcars.business.Gate state) {
        switch (msg.getString("type")) {
            case "enter-request": {
                final String license = msg.getString("license");
                return new EnterResponse(
                    license,
                    state.requestEnter(license),
                    Spot.anyFree().map(Spot::id).orElse(-1)
                );
            }
            case "leave-request": {
                final String license = msg.getString("license");
                return new LeaveResponse(
                    license,
                    state.requestLeave(license)
                );
            }
            case "opened":
                state.new OpenedEvent().fire();
                return null;
            case "entered":
                state.new EnteredEvent(
                    msg.getString("license"),
                    msg.getInt("recommendedSpot")
                ).fire();
                return null;
            case "left":
                state.new LeftEvent(
                    msg.getString("license")
                ).fire();
                return null;
            default:
                return null;
        }
    }

    private class EnterResponse extends TypedMessage {
        final String license;
        final boolean permission;
        final int recommendedSpot;

        public EnterResponse(final String license, final boolean permission, final int recommendedSpot) {
            super("enter-response");
            this.license = license;
            this.permission = permission;
            this.recommendedSpot = recommendedSpot;
        }
    }

    private class LeaveResponse extends TypedMessage {
        final String license;
        final boolean permission;

        public LeaveResponse(final String license, final boolean permission) {
            super("leave-response");
            this.license = license;
            this.permission = permission;
        }
    }
}
