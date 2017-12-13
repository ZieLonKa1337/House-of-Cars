package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.ActivityTestUtil;
import de.codazz.houseofcars.domain.Parking;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
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
    public static final int NUM_TOTAL = 100;
    public static final Map<Spot.Type, Integer> NUM_SPOTS; static {
        final Map<Spot.Type, Integer> spots = new HashMap<>();
        spots.put(Spot.Type.BIKE, NUM_TOTAL / 10);
        spots.put(Spot.Type.HANDICAP, NUM_TOTAL / 6);
        spots.put(Spot.Type.CAR, NUM_TOTAL - spots.get(Spot.Type.BIKE) - spots.get(Spot.Type.HANDICAP));
        NUM_SPOTS = Collections.unmodifiableMap(spots);
    }

    static GarageImplMock garage;

    Clock clock = Clock.systemDefaultZone();

    @BeforeClass
    public static void setUpClass() throws FileNotFoundException {
        garage = new GarageImplMock();
    }

    @AfterClass
    public static void tearDownClass() {
        garage.close();
    }

    @Before
    public void setUp() {
        garage.reset(NUM_TOTAL, NUM_SPOTS);
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

            parked += garage.persistence.transact((em, __) -> {
                for (int i = 0; i < num; i++) {
                    final Parking parking = parkings.remove(parkings.size() - 1);
                    parking.park(garage.nextFree(type).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                }
                return num;
            });
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

            parked += garage.persistence.transact((em, __) -> {
                for (int i = 0; i < num; i++) {
                    final Parking parking = parkings.remove(parkings.size() - 1);
                    ActivityTestUtil.clock(parking, tick());
                    parking.park(garage.nextFree(type).orElseThrow(() -> new IllegalStateException("likely bug in test code")));
                    parkingsParked.add(parking);
                }
                return num;
            });

            checkState(NUM_TOTAL, NUM_TOTAL - parked, parked, total - parked, total - parked, 0, total);
            checkState(type, numTotal, numTotal - num, num, divisor != 1);
        }

        // leave spot
        int finished = 0;
        final List<Parking> parkingsFinished = new ArrayList<>();
        for (final Spot.Type type : Spot.Type.values()) {
            final int numTotal = NUM_SPOTS.get(type), num = numTotal / divisor;

            finished += garage.persistence.transact((em, __) -> {
                for (int i = 0; i < num; i++) {
                    final Parking parking = parkingsParked.stream()
                        .filter(x -> x.spot().get().type() == type)
                        .findFirst().get();
                    parkingsParked.remove(parking);
                    ActivityTestUtil.clock(parking, tick());
                    parking.finish();
                    parkingsFinished.add(parking);
                }
                return num;
            });

            checkState(NUM_TOTAL, NUM_TOTAL - parked + finished, parked - finished, finished, 0, finished, total);
            checkState(type, numTotal, numTotal, 0, numTotal > 0 && divisor != 1);
        }
        assert finished == parked;
        assert parked == total;
        assert parkingsParked.size() == 0;
        assert parkingsFinished.size() == finished;

        // leave garage
        garage.persistence.<Void>transact((em, __) -> {
//            parkingsFinished.forEach(it -> it.vehicle().type(false)); // FIXME
            return null;
        });
        checkState(NUM_TOTAL, NUM_TOTAL, 0, 0, 0, 0, 0);
        for (final Spot.Type type : Spot.Type.values()) {
            final int num = NUM_SPOTS.get(type);
            checkState(type, num, num, 0, num > 0 && divisor != 1);
        }
    }

    public static void checkState(
        final int numTotal,
        final int numFree,
        final int numUsed,
        final int numPending,
        final int numParking,
        final int numLeaving,
        final int numVehicles
    ) {
        assertEquals(numTotal, garage.numTotal());
        assertEquals(numFree, garage.numFree());
        assertEquals(numUsed, garage.numUsed());
        assertEquals(numPending, garage.numPending());
        assertEquals(numParking, garage.numParking());
        assertEquals(numLeaving, garage.numLeaving());
        assertEquals(numVehicles, garage.numVehicles());
    }

    public static void checkState(
        final Spot.Type type,
        final int numTotal,
        final int numFree,
        final int numUsed,
        final boolean nextFree
    ) {
        assertEquals(numTotal, garage.numTotal(type));
        assertEquals(numFree, garage.numFree(type));
        assertEquals(numUsed, garage.numUsed(type));
        assertSame(nextFree, garage.nextFree(type).isPresent());
    }

    private Parking[] park(final Spot.Type type, int n) {
        return park(type, true, false, n);
    }

    private Parking[] park(final Spot.Type type, final boolean park, final boolean leave, int n) {
        assert park || !leave : "cannot leave without parking first";

        final Parking[] parkings = new Parking[n];
        for (n -= 1; n >= 0; n--) {
            final int finalN = n;
            garage.persistence.<Void>transact((em, __) -> {
                final int numPlates = em.createQuery("SELECT COUNT(v) FROM Vehicle v", Long.class).getSingleResult().intValue();
                final String license = String.format("AABB%04d", numPlates);
                final Vehicle vehicle; {
                    Vehicle v = em.find(Vehicle.class, license);
                    if (v == null) {
                        v = new Vehicle(license);
                        em.persist(v);
                    }
                    vehicle = v;
                }

                final Parking parking = new Parking(vehicle);
                final Spot spot = garage.spots.stream().filter(s -> s.type() == type).findFirst().orElseThrow(() -> new IllegalStateException("likely bug in test code"));
                garage.spots.remove(spot);
                if (park) {
                    ActivityTestUtil.clock(parking, tick());
                    parking.park(spot);
                    if (leave) {
                        ActivityTestUtil.clock(parking, tick());
                        parking.finish();
                    }
                }
                em.persist(parking);
                parkings[finalN] = parking;

                return null;
            });
        }

        return parkings;
    }

    private Clock tick() {
        return clock = Clock.offset(clock, Duration.ofMinutes(5));
    }
}
