package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.GarageImpl;
import de.codazz.houseofcars.domain.Parking;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.StateMachineException;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.Timer;
import java.util.TimerTask;

/** virtual gate
 * @author rstumm2s */
@WebSocket
public class VGate extends Gate {
    @Override
    protected Message handle(final de.codazz.houseofcars.business.Gate state, final JsonValue msg) throws StateMachineException {
        final Message response = super.handle(state, msg);

        // instead of implementing the spot license readers we will just make the vehicle park after a while
        switch (msg.getString("type")) {
            case "entered": {
                final String license = msg.getString("license");
                final Timer timer = new Timer("VGate Parking " + license, true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        timer.cancel();
                        GarageImpl.instance().persistence.<Void>transact((em, __) -> {
                            final Vehicle vehicle = em.find(Vehicle.class, license);

                            final Spot spot = em
                                    .createQuery("SELECT s FROM Spot s WHERE s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.spot IS NOT NULL)", Spot.class)
                                    .setMaxResults(1)
                                    .getSingleResult();

                            final Parking parking = em
                                    .createNamedQuery("Parking.findParking", Parking.class)
                                    .setMaxResults(1)
                                    .setParameter("vehicle", vehicle)
                                    .getSingleResult();

                            parking.park(spot);
                            return null;
                        });
                        Status.update();
                    }
                }, (int) (Math.random() * 5) * 1000);
            } break;
        }

        return response;
    }
}
