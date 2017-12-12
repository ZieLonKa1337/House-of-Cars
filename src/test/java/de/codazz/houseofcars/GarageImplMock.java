package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
public class GarageImplMock extends GarageImpl {
    public final Map<Spot.Type, Integer> numSpots;
    public final ArrayList<Spot> spots;

    public GarageImplMock() throws FileNotFoundException {
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
}
