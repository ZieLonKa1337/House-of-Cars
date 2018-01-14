package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.MagicCookie;
import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.websocket.TypedMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@WebSocket
public class Customers {
    private static final JsonReader jsonReader = new JsonReader();

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws LoginException, IOException {
        final JsonValue msg = jsonReader.parse(message);
        switch (msg.getString("type")) {
            case "add-vehicle": {
                final Customer customer = fromMagicCookie(session).orElseThrow(() -> new IllegalStateException("not logged in"));
                final Vehicle vehicle = Objects.requireNonNull(Garage.instance().persistence.execute(em -> em.find(Vehicle.class, msg.getString("vehicle"))));
                if (vehicle.owner().map(it -> it.equals(customer)).orElse(false)) {
                    break; // nothing to do - vehicle is already owned by the customer
                }

                if (!vehicle.owner().isPresent()) {
                    Garage.instance().persistence.<Void>transact((__, ___) -> {
                        vehicle.owner(customer);
                        return null;
                    });
                    session.getRemote().sendString(new TypedMessage("changed").toString());
                } else {
                    session.getRemote().sendString(new TypedMessage("merge") {
                        final String license = vehicle.license();
                    }.toString());
                }
            } break;
            case "merge": {
                final Customer customer = fromMagicCookie(session).orElseThrow(() -> new IllegalStateException("not logged in"));
                final Vehicle vehicle = Objects.requireNonNull(Garage.instance().persistence.execute(em -> em.find(Vehicle.class, msg.getString("vehicle"))));
                final Subject owner = Garage.instance().sessions.login(vehicle.license(), msg.getString("pass"));
                if (owner != null) {
                    Garage.instance().persistence.<Void>transact((__, ___) -> {
                        vehicle.owner(customer);
                        return null;
                    });
                }
                session.getRemote().sendString(new TypedMessage("changed").toString());
            } break;
        }
    }

    public static Optional<Subject> subjectFromMagicCookie(final Session session) {
        return session.getUpgradeRequest().getCookies().stream()
            .filter(it -> it.getName().equals("hoc-magic"))
            .findFirst()
            .map(HttpCookie::getValue)
            .map(magic -> Garage.instance().sessions.present()
                .filter(it -> it.getPublicCredentials(MagicCookie.class).stream()
                    .map(cookie -> cookie.value)
                    .anyMatch(magic::equals)
                ).findFirst().orElse(null));
    }

    public static Optional<Customer> fromMagicCookie(final Session session) {
        return subjectFromMagicCookie(session)
            .map(subject -> subject.getPrincipals(Customer.class).iterator().next());
    }
}
