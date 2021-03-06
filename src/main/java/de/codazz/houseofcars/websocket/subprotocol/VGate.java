package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonValue;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.VehicleTransition;
import de.codazz.houseofcars.websocket.Message;
import de.codazz.houseofcars.websocket.TypedMessage;
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
    protected Message handle(final JsonValue msg, final de.codazz.houseofcars.business.Gate state) {
        final Message response = super.handle(msg, state);

        /* instead of implementing the spot license readers
         * we will just make the vehicle park on its
         * recommended spot after a while */
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

                        Garage.instance().persistence.execute(em -> em.find(Vehicle.class, license))
                            .state().new ParkedEvent().fire();
                    }
                }, delay);
            } break;
            case "get-state": {
                assert response == null;
                final String _license = msg.getString("license");
                final Vehicle vehicle = Garage.instance().persistence.execute(em ->
                    em.find(Vehicle.class, _license)
                );
                return new TypedMessage("get-state") {
                    final String license = _license;
                    final String state = vehicle != null
                        ? vehicle.state().state().name()
                        : Vehicle.State.Away.name();
                    final Boolean paid = vehicle == null ? null
                        : vehicle.lastTransition()
                            .flatMap(VehicleTransition::paid)
                            .orElse(null);
                };
            }
        }

        return response;
    }
}
