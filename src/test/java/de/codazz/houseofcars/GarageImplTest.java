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
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author rstumm2s */
public class GarageImplTest {
    static Garage garage;

    static Clock clock;

    @BeforeClass
    public static void setUpClass() throws IOException {
        garage = new GarageImpl(ConfigImpl.load(new FileInputStream(ConfigImplTest.CONFIG))) {
            @Override
            public Optional<Spot> nextFree() {
                return Optional.of(new Spot(Spot.Type.CAR) {
                    @Override
                    public int id() {
                        return 0;
                    }
                });
            }
        };

        clock = Clock.systemDefaultZone();
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
        // create 100 new spots with 10 bike, 15 handicap
        for (int i = 0; i < 100; i++) {
            em.persist(new de.codazz.houseofcars.domain.Spot(i < 10 ? Spot.Type.BIKE : i < 25 ? Spot.Type.HANDICAP : Spot.Type.CAR));
        }
        em.getTransaction().commit();
    }

    @Test
    public void numTotal() {
        assertEquals(100, garage.numTotal());
        park(35);
        assertEquals(100, garage.numTotal());
    }

    @Test
    public void numFree() {
        assertEquals(garage.numTotal(), garage.numFree());

        park(25);
        assertEquals(75, garage.numFree());

        park(75);
        assertEquals(0, garage.numFree());
    }

    @Test
    public void numUsed() {
        assertEquals(0, garage.numUsed());

        park(50);
        assertEquals(50, garage.numUsed());
    }

    @Test
    public void nextFree() {
        assertTrue(garage.nextFree().isPresent());
    }

    private static void park(int n) {
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
            ActivityTestUtil.clock(parking, tick());
            parking.start();
            parking.spot(em.find(Spot.class, n)); // FIXME finds nothing
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
