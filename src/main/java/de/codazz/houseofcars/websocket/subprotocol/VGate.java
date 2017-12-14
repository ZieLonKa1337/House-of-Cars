package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.StateMachineException;
import de.codazz.houseofcars.websocket.Message;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/** virtual gate
 * @author rstumm2s */
@WebSocket
public class VGate extends Gate {
    private static final Logger log = LoggerFactory.getLogger(VGate.class);

    @Override
    protected Message handle(final JsonValue msg, final de.codazz.houseofcars.business.Gate state) throws StateMachineException {
        final Message response = super.handle(msg, state);

        // instead of implementing the spot license readers we will just make the vehicle park after a while
        switch (msg.getString("type")) {
            case "entered": {
                final int delay = (int) ((Math.random() * 5) * 1000);
                final String license = msg.getString("license");
                log.debug("parking {} in {}ms", license, delay);
                final Timer timer = new Timer("VGate Parking " + license, true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        timer.cancel();

                        final Vehicle vehicle = Garage.instance().persistence.execute(em -> em.find(Vehicle.class, license));
                        final Spot spot = Garage.instance().persistence.execute(em -> em // TODO select more probable spot based on spot count by type, or select type via UI
                            .createQuery("SELECT s FROM Spot s WHERE s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.spot IS NOT NULL AND p.freed IS NULL)", Spot.class)
                            .setMaxResults(1)
                            .getSingleResult());

                        try {
                            vehicle.state().onEvent(((Vehicle.Lifecycle.LookingForSpot) vehicle.state().state()).new ParkedEvent(spot));
                        } catch (final StateMachineException | ClassNotFoundException e) {
                            log.error("failed to update state of {}", vehicle.license());
                            throw new RuntimeException(e);
                        }

                        Status.update();
                    }
                }, delay);
            } break;
        }

        return response;
    }
}
