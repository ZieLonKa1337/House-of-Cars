package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.statemachine.StateMachineException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.codazz.houseofcars.domain.Vehicle$LifecycleTest.comesAfter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** @author rstumm2s */
public class GarageMock extends Garage {
    public final Map<Spot.Type, Integer> numSpots;
    public final ArrayList<Spot> spots;

    public GarageMock() throws FileNotFoundException {
        super(ConfigImpl.load(new FileInputStream(ConfigImplTest.CONFIG)));
        persistence.executor = new ExecutorServiceMock();
        this.numSpots = new HashMap<>();
        spots = new ArrayList<>();
    }

    public void reset(final int numTotal, final Map<Spot.Type, Integer> numSpots) {
        spots.clear();
        spots.ensureCapacity(numTotal);
        persistence.<Void>transact((em, __) -> {
            // clear
            for (String clazz : new PerstistenceUnitInfoImpl(null, null).getManagedClassNames()) {
                clazz = clazz.substring(clazz.lastIndexOf('.') + 1);
                em.createQuery("DELETE FROM " + clazz).executeUpdate();
            }
            em.clear();
            // create spots
            spots.clear();
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
    public Vehicle[] park(final Spot.Type type, final Class state, int n) throws ClassNotFoundException, StateMachineException {
        final Vehicle[] vehicles = new Vehicle[n];
        for (n -= 1; n >= 0; n--) {
            final Vehicle vehicle = Garage.instance().persistence.transact((em, __) -> {
                final int numPlates = em.createQuery("SELECT COUNT(v) FROM Vehicle v", Long.class).getSingleResult().intValue();
                final String license = String.format("AABB%04d", numPlates);

                Vehicle v = em.find(Vehicle.class, license);
                if (v == null) {
                    v = new Vehicle(license);
                    em.persist(v);
                }
                return v;
            });
            vehicles[n] = vehicle;

            final Vehicle.Lifecycle lifecycle = vehicle.state();
            lifecycle.start();

            if (comesAfter(state, Vehicle.Lifecycle.Away.class)) { // Away -> LookingForSpot
                assertFalse(lifecycle.onEvent(((Vehicle.Lifecycle.Away) lifecycle.state()).new EnteredEvent()).isPresent());
                assertEquals(Vehicle.Lifecycle.LookingForSpot.class, lifecycle.state().getClass());
            }
            if (comesAfter(state, Vehicle.Lifecycle.LookingForSpot.class)) { // LookingForSpot -> Parking
                final Spot spot = Garage.instance().persistence.execute(em -> em
                    .createNamedQuery("Spot.anyFree", Spot.class)
                    .setMaxResults(1)
                    .setParameter("type", type)
                    .getSingleResult());
                assertFalse(lifecycle.onEvent(((Vehicle.Lifecycle.LookingForSpot) lifecycle.state()).new ParkedEvent(spot)).isPresent());
                assertEquals(Vehicle.Lifecycle.Parking.class, lifecycle.state().getClass());
            }
            if (comesAfter(state, Vehicle.Lifecycle.Parking.class)) { // Parking -> Leaving
                assertFalse(lifecycle.onEvent(((Vehicle.Lifecycle.Parking) lifecycle.state()).new LeftSpotEvent()).isPresent());
                assertEquals(Vehicle.Lifecycle.Leaving.class, lifecycle.state().getClass());
            }
            if (comesAfter(state, Vehicle.Lifecycle.Leaving.class)) { // Leaving -> Away
                assertFalse(lifecycle.onEvent(((Vehicle.Lifecycle.Leaving) lifecycle.state()).new LeftEvent()).isPresent());
                assertEquals(Vehicle.Lifecycle.Away.class, lifecycle.state().getClass());
            }
        }
        return vehicles;
    }
}
