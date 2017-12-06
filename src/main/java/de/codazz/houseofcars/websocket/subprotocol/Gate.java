package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.GarageImpl;
import de.codazz.houseofcars.domain.Parking;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/** @author rstumm2s */
@WebSocket
public class Gate {
    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {
        final JsonValue msg = new JsonReader().parse(message);
        switch (msg.getString("type")) {
            case "open-request": {
                /* the gate sees a vehicle's license
                 * and asks whether it is allowed in */
                session.getRemote().sendString(new OpenResponse(
                        GarageImpl.instance().persistence.execute(em -> em
                                .createNamedQuery("Vehicle.mayEnter", Boolean.class)
                                .setParameter("license", msg.getString("license"))
                                .getSingleResult()) &&
                        GarageImpl.instance().numFree() > 0
                ).toJson());
            } break;
            case "entered": {
                /* the vehicle entered the garage
                 * and the gate has closed again */
                final Parking parking = GarageImpl.instance().persistence.transact((em, __) -> {
                    Vehicle vehicle = em.find(Vehicle.class, msg.getString("license"));
                    if (vehicle == null) {
                        vehicle = new Vehicle(msg.getString("license"));
                        em.persist(vehicle);
                    }
                    vehicle.present(true);

                    final Parking p = new Parking(vehicle);
                    em.persist(p);
                    return p;
                });

                if (msg.getBoolean("simulate")) {
                    /* instead of implementing the spot license readers
                     * we will just make the vehicle park after a while */
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            GarageImpl.instance().persistence.<Void>transact((em, __) -> {
                                final TypedQuery<Spot> nextFreeAny = em
                                        .createQuery("SELECT s FROM Spot s WHERE s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.spot IS NOT NULL)", Spot.class)
                                        .setMaxResults(1);
                                parking.park(nextFreeAny.getSingleResult());
                                return null;
                            });
                            Status.update();
                        }
                    }, (int) (Math.random() * 5) * 1000);
                }
            } break;
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
