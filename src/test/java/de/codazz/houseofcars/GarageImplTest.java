package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.ActivityTestUtil;
import de.codazz.houseofcars.domain.LicensePlate;
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
        em.createQuery("DELETE FROM LicensePlate").executeUpdate();
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
    public void nextFree() {
        for (final Spot.Type type : Spot.Type.values()) {
            assertTrue(garage.nextFree(type).isPresent());

            park(type, NUM_SPOTS.get(type) - 1);
            assertTrue(garage.nextFree(type).isPresent());

            park(type, 1);
            assertFalse(garage.nextFree(type).isPresent());
        }
    }

    private static void park(final Spot.Type type, int n) {
        final EntityManager em = garage.entityManager();

        for (; n > 0; n--) {
            final int numPlates = em.createQuery("SELECT COUNT(p) FROM LicensePlate p", Long.class).getSingleResult().intValue();
            final String plateCode = String.format("AABB%04d", numPlates);
            final LicensePlate licensePlate; {
                LicensePlate plate = em.find(LicensePlate.class, plateCode);
                if (plate == null) {
                    em.getTransaction().begin();
                    em.persist(plate = new LicensePlate(plateCode));
                    em.getTransaction().commit();
                }
                licensePlate = plate;
            }

            em.getTransaction().begin();
            final Parking parking = new Parking(licensePlate);
            final Spot spot = spots.stream().filter(s -> s.type() == type).findFirst().orElseThrow(() -> new IllegalStateException("likely bug in test code!"));
            spots.remove(spot);
            ActivityTestUtil.clock(parking, tick());
            parking.start();
            parking.spot(spot);
            ActivityTestUtil.clock(parking, tick());
            parking.finish();
            em.persist(parking);
            em.getTransaction().commit();
        }
    }

    private static Clock tick() {
        return clock = Clock.offset(clock, Duration.ofMinutes(5));
    }
}
