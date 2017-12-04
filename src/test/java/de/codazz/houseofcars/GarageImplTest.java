package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.ActivityTestUtil;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.Parking;
import de.codazz.houseofcars.domain.Spot;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class GarageImplTest {
    static final int NUM_TOTAL = 100;
    static final Map<Spot.Type, Integer> NUM_SPOTS = new HashMap<>(); static {
        NUM_SPOTS.put(Spot.Type.BIKE, NUM_TOTAL / 10);
        NUM_SPOTS.put(Spot.Type.HANDICAP, NUM_TOTAL / 6);
        NUM_SPOTS.put(Spot.Type.CAR, NUM_TOTAL - NUM_SPOTS.get(Spot.Type.BIKE) - NUM_SPOTS.get(Spot.Type.HANDICAP));
    }

    static Clock clock = Clock.systemDefaultZone();
    static Garage garage;
    static List<Spot> spots = new ArrayList<>(NUM_TOTAL);

    @BeforeClass
    public static void setUpClass() throws IOException {
        garage = new GarageImpl(ConfigImpl.load(new FileInputStream(ConfigImplTest.CONFIG)));
    }

    @Before
    public void setUp() {
        final EntityManager em = garage.entityManager();
        em.getTransaction().begin();
        // clear
        em.createQuery("DELETE FROM Parking").executeUpdate();
        em.createQuery("DELETE FROM Vehicle").executeUpdate();
        em.createQuery("DELETE FROM Spot").executeUpdate();
        em.clear();
        // create spots
        spots.clear();
        for (int i = 0; i < NUM_TOTAL; i++) {
            final Spot spot = new Spot(i < NUM_SPOTS.get(Spot.Type.BIKE) ? Spot.Type.BIKE :
                    i < NUM_SPOTS.get(Spot.Type.BIKE) + NUM_SPOTS.get(Spot.Type.HANDICAP) ? Spot.Type.HANDICAP :
                    Spot.Type.CAR);
            spots.add(spot);
            em.persist(spot);
        }
        em.getTransaction().commit();
    }

    @Test
    public void numTotal() {
        assertEquals(NUM_TOTAL, garage.numTotal());
        for (final Spot.Type type : Spot.Type.values()) {
            assertEquals(NUM_SPOTS.get(type).intValue(), garage.numTotal(type));
        }

        park(Spot.Type.CAR, NUM_SPOTS.get(Spot.Type.CAR) / 2);
        assertEquals(NUM_TOTAL, garage.numTotal());
        for (final Spot.Type type : Spot.Type.values()) {
            assertEquals(NUM_SPOTS.get(type).intValue(), garage.numTotal(type));
        }
    }

    @Test
    public void numFree() {
        int total = 0;
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) - 1;

            assertEquals(NUM_TOTAL - total, garage.numFree());
            assertEquals(NUM_SPOTS.get(type).intValue(), garage.numFree(type));

            park(type, num);
            total += num;
            assertEquals(1, garage.numFree(type));

            park(type, 1);
            total += 1;
            assertEquals(0, garage.numFree(type));
        }
    }

    @Test
    public void numUsed() {
        int total = 0;
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) - 1;

            assertEquals(total, garage.numUsed());
            assertEquals(0, garage.numUsed(type));

            park(type, num);
            total += num;
            assertEquals(num, garage.numUsed(type));

            park(type, 1);
            total += 1;
            assertEquals(num + 1, garage.numUsed(type));
        }
    }

    @Test
    public void numParking() {
        assertEquals(0, garage.numParking());

        int total = 0;
        final List<Parking> parkings = new ArrayList<>();
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) / 3;

            Collections.addAll(parkings, park(type, false, false, num));
            total += num;
            assertEquals(total, garage.numParking());
        }

        int parked = 0;
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type) / 3;

            garage.entityManager().getTransaction().begin();
            for (int i = 0; i < num; i++) {
                final Parking parking = parkings.remove(parkings.size() - 1);
                parking.park(garage.nextFree(type).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                garage.entityManager().persist(parking);
            }
            garage.entityManager().getTransaction().commit();
            parked += num;
            assertEquals(total - parked, garage.numParking());
        }
    }

    @Test
    public void nextFree() {
        for (final Spot.Type type : Spot.Type.values()) {
            assertTrue(garage.nextFree(type).isPresent());

            park(type, NUM_SPOTS.get(type) - 1);
            assertTrue(garage.nextFree(type).isPresent());

            park(type, 1);
            assertFalse(garage.nextFree(type).isPresent());
        }
    }

    @Test
    public void lifecycle() {
        checkState(NUM_TOTAL, NUM_TOTAL, 0, 0, 0, 0, 0);
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type);
            checkState(type, num, num, 0, num > 0);
        }

        final int divisor = 2;

        // enter garage
        int total = 0;
        final List<Parking> parkings = new ArrayList<>();
        for (final Spot.Type type : Spot.Type.values()) {
            final int numTotal = NUM_SPOTS.get(type), num = numTotal / divisor;

            Collections.addAll(parkings, park(type, false, false, num));
            total += num;

            checkState(NUM_TOTAL, NUM_TOTAL, 0, total, total, 0, total);
            checkState(type, numTotal, numTotal, 0, num > 0);
        }

        // park
        int parked = 0;
        final List<Parking> parkingsParked = new ArrayList<>();
        for (final Spot.Type type : Spot.Type.values()) {
            final int numTotal = NUM_SPOTS.get(type), num = numTotal / divisor;

            garage.entityManager().getTransaction().begin();
            for (int i = 0; i < num; i++) {
                final Parking parking = parkings.remove(parkings.size() - 1);
                ActivityTestUtil.clock(parking, tick());
                parking.park(garage.nextFree(type).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                parkingsParked.add(parking);
                garage.entityManager().persist(parking);
            }
            garage.entityManager().getTransaction().commit();
            parked += num;

            checkState(NUM_TOTAL, NUM_TOTAL - parked, parked, total - parked, total - parked, 0, total);
            checkState(type, numTotal, numTotal - num, num, divisor != 1);
        }

        // leave spot
        int finished = 0;
        final List<Parking> parkingsFinished = new ArrayList<>();
        for (final Spot.Type type : Spot.Type.values()) {
            final int numTotal = NUM_SPOTS.get(type), num = numTotal / divisor;

            garage.entityManager().getTransaction().begin();
            for (int i = 0; i < num; i++) {
                final Parking parking = parkingsParked.stream()
                        .filter(x -> x.spot().get().type() == type)
                        .findFirst().get();
                parkingsParked.remove(parking);
                ActivityTestUtil.clock(parking, tick());
                parking.finish();
                parkingsFinished.add(parking);
                garage.entityManager().persist(parking);
            }
            garage.entityManager().getTransaction().commit();
            finished += num;

            checkState(NUM_TOTAL, NUM_TOTAL - parked + finished, parked - finished, finished, 0, finished, total);
            checkState(type, numTotal, numTotal, 0, numTotal > 0 && divisor != 1);
        }
        assert finished == parked;
        assert parked == total;
        assert parkingsParked.size() == 0;
        assert parkingsFinished.size() == finished;

        // leave garage
        garage.entityManager().getTransaction().begin();
        parkingsFinished.forEach(parking -> {
            parking.vehicle().present(false);
            garage.entityManager().persist(parking);
        });
        garage.entityManager().getTransaction().commit();
        checkState(NUM_TOTAL, NUM_TOTAL, 0, 0, 0, 0, 0);
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type);
            checkState(type, num, num, 0, num > 0 && divisor != 1);
        }
    }

    private static void checkState(
            final int numTotal,
            final int numFree,
            final int numUsed,
            final int numPending,
            final int numParking,
            final int numLeaving,
            final int numVehicles) {
        assertEquals(numTotal, garage.numTotal());
        assertEquals(numFree, garage.numFree());
        assertEquals(numUsed, garage.numUsed());
        assertEquals(numPending, garage.numPending());
        assertEquals(numParking, garage.numParking());
        assertEquals(numLeaving, garage.numLeaving());
        assertEquals(numVehicles, garage.numVehicles());
    }

    private static void checkState(
            final Spot.Type type,
            final int numTotal,
            final int numFree,
            final int numUsed,
            final boolean nextFree) {
        assertEquals(numTotal, garage.numTotal(type));
        assertEquals(numFree, garage.numFree(type));
        assertEquals(numUsed, garage.numUsed(type));
        assertSame(nextFree, garage.nextFree(type).isPresent());
    }

    private static Parking[] park(final Spot.Type type, int n) {
        return park(type, true, false, n);
    }

    private static Parking[] park(final Spot.Type type, final boolean park, final boolean leave, int n) {
        assert park || !leave : "cannot leave without parking first";

        final EntityManager em = garage.entityManager();

        final Parking[] parkings = new Parking[n];
        for (n -= 1; n >= 0; n--) {
            em.getTransaction().begin();

            final int numPlates = em.createQuery("SELECT COUNT(v) FROM Vehicle v", Long.class).getSingleResult().intValue();
            final String license = String.format("AABB%04d", numPlates);
            final Vehicle vehicle; {
                Vehicle v = em.find(Vehicle.class, license);
                if (v == null) {
                    v = new Vehicle(license);
                }
                v.present(true);
                vehicle = v;
                em.persist(vehicle);
            }

            final Parking parking = new Parking(vehicle);
            final Spot spot = spots.stream().filter(s -> s.type() == type).findFirst().orElseThrow(() -> new IllegalStateException("likely bug in test code"));
            spots.remove(spot);
            if (park) {
                ActivityTestUtil.clock(parking, tick());
                parking.park(spot);
                if (leave) {
                    ActivityTestUtil.clock(parking, tick());
                    parking.finish();
                }
            }
            em.persist(parking);

            em.getTransaction().commit();
            parkings[n] = parking;
        }

        return parkings;
    }

    private static Clock tick() {
        return clock = Clock.offset(clock, Duration.ofMinutes(5));
    }
}
