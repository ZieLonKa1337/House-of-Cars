package de.codazz.houseofcars;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author rstumm2s */
public class GarageImplTest {
    static Garage garage;

    @BeforeClass
    public static void setUp() throws IOException {
        garage = new GarageImpl(ConfigImpl.load(new FileInputStream(ConfigImplTest.CONFIG))) {
            @Override
            public int numUsed() {
                return 0;
            }

            @Override
            public int numFree() {
                return 100;
            }

            @Override
            public Optional<Spot> nextFree() {
                return Optional.of(new Spot() {
                    @Override
                    public int id() {
                        return 0;
                    }

                    @Override
                    public Type type() {
                        return Type.CAR;
                    }
                });
            }
        };

        // set up database
        final EntityManager em = garage.entityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Spot").executeUpdate();
        for (int i = 0; i < 100; i++) {
            em.persist(new de.codazz.houseofcars.domain.Spot(i < 10 ? Spot.Type.BIKE : i < 25 ? Spot.Type.HANDICAP : Spot.Type.CAR));
        }
        em.getTransaction().commit();
    }

    @Test
    public void numFree_empy_total() {
        assertEquals(garage.numFree(), garage.numTotal());
    }

    @Test
    public void numUsed_empty_0() {
        assertEquals(garage.numUsed(), 0);
    }

    @Test
    public void nextFree_empty_spot() {
        assertTrue(garage.nextFree().isPresent());
    }
}
