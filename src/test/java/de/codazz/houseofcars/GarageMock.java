package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertSame;

/** @author rstumm2s */
public class GarageMock extends Garage {
    public final ArrayList<Spot> spots = new ArrayList<>();

    public GarageMock() throws FileNotFoundException {
        super(ConfigImpl.load(new FileInputStream(ConfigImplTest.CONFIG)));
        persistence.executor = new ExecutorServiceMock();
    }

    public void reset(final int numTotal, final Map<Spot.Type, Integer> numSpots) {
        spots.clear();
        spots.ensureCapacity(numTotal);
        persistence.<Void>transact((em, __) -> {
            // clear
            em.createQuery("DELETE FROM VehicleTransition").executeUpdate();
            em.createQuery("DELETE FROM Vehicle").executeUpdate();
            em.createQuery("DELETE FROM Spot").executeUpdate();
            em.clear();
            // create spots
            for (int i = 0; i < numTotal; i++) {
                final Spot spot = new Spot(i < numSpots.get(Spot.Type.BIKE) ? Spot.Type.BIKE :
                    i < numSpots.get(Spot.Type.BIKE) + numSpots.get(Spot.Type.HANDICAP) ? Spot.Type.HANDICAP :
                    Spot.Type.CAR);
                spots.add(spot);
                em.persist(spot);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public Vehicle[] park(final Spot.Type type, final Vehicle.State state, int n) {
        final Vehicle[] vehicles = new Vehicle[n];
        for (n -= 1; n >= 0; n--) {
            final Vehicle vehicle = Garage.instance().persistence.transact((em, __) -> {
                final int numPlates = em.createQuery("SELECT COUNT(v) FROM Vehicle v", Long.class).getSingleResult().intValue();
                final String license = String.format("ABXY%04d", numPlates);

                Vehicle v = em.find(Vehicle.class, license);
                if (v == null) {
                    v = new Vehicle(license);
                    em.persist(v);
                }
                return v;
            });
            vehicles[n] = vehicle;

            final boolean cycle = state == Vehicle.State.Away;

            assertSame(Vehicle.State.Away, vehicle.state().state());

            if (cycle || state.ordinal() > Vehicle.State.Away.ordinal()) {
                final Spot spot = spots.stream()
                    .filter(s -> s.type() == type)
                    .findAny().orElseThrow(() -> new IllegalStateException("bug in test code?"));
                spots.remove(spot);
                vehicle.state().new EnteredEvent(spot).fire();
                assertSame(Vehicle.State.LookingForSpot, vehicle.state().state());
            }
            if (cycle || state.ordinal() > Vehicle.State.LookingForSpot.ordinal()) {
                vehicle.state().new ParkedEvent().fire();
                assertSame(Vehicle.State.Parking, vehicle.state().state());
            }
            if (cycle || state.ordinal() > Vehicle.State.Parking.ordinal()) {
                vehicle.state().new FreedEvent().fire();
                assertSame(Vehicle.State.Leaving, vehicle.state().state());
            }
            if (cycle) {
                vehicle.state().new PaidEvent().fire();
                vehicle.state().new LeaveEvent().fire();
                assertSame(Vehicle.State.Away, vehicle.state().state());
            }
        }
        return vehicles;
    }
}
