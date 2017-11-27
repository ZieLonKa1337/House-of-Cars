package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;
import spark.template.mustache.MustacheTemplateEngine;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

/** @author rstumm2s */
public class GarageImpl implements Garage {
    private static final String CONFIG_FILE = "house-of-cars.json";

    private final Config config;
    private final EntityManager entityManager;

    private final TypedQuery<? extends Number> numTotal, numUsed, numUsed_type, numTotal_type;
    private final TypedQuery<Spot> nextFree;

    public GarageImpl() {
        this(null);
    }

    public GarageImpl(Config config) {
        if (config == null) {
            config = new ConfigImpl();
            try {
                final Config userConfig = ConfigImpl.load(new FileInputStream(CONFIG_FILE)),
                        defaults = new ConfigImpl();
                config = userConfig.merge(defaults, false);
            } catch (final FileNotFoundException ignore) {}
        }
        this.config = config;

        // connect to persistence database
        final Map<String, Object> persistenceFactoryProp = new HashMap<>();
        persistenceFactoryProp.put(AvailableSettings.JPA_JDBC_URL, config.jdbcUrl());
        persistenceFactoryProp.put(AvailableSettings.JPA_JDBC_DRIVER, "org.postgresql.Driver");
        persistenceFactoryProp.put(AvailableSettings.JPA_JDBC_USER, config.jdbcUser());
        persistenceFactoryProp.put(AvailableSettings.JPA_JDBC_PASSWORD, config.jdbcPassword());
        entityManager = new HibernatePersistenceProvider()
                .createContainerEntityManagerFactory(
                        new PerstistenceUnitInfoImpl(
                                "HouseOfCars",
                                HibernatePersistenceProvider.class.getName()),
                        persistenceFactoryProp)
                .createEntityManager();

        // build queries
        numTotal = entityManager.createQuery("SELECT COUNT(s) FROM Spot s", Long.class);
        numTotal_type = entityManager.createQuery("SELECT COUNT(s) FROM Spot s WHERE s.type = :type", Long.class);
        numUsed = entityManager.createQuery("SELECT COUNT(p) FROM Parking p", Long.class);
        numUsed_type = entityManager.createQuery("SELECT COUNT(p) FROM Parking p WHERE p.spot.type = :type", Long.class);
        nextFree = entityManager.createQuery("SELECT s FROM Spot s WHERE s.type = :type AND s NOT IN (SELECT DISTINCT p.spot FROM Parking p WHERE p.spot IS NOT NULL)", Spot.class)
                .setMaxResults(1);
    }

    @Override
    public EntityManager entityManager() {
        return entityManager;
    }

    @Override
    public int numTotal() {
        return numTotal.getSingleResult().intValue();
    }

    @Override
    public int numTotal(final Spot.Type type) {
        numTotal_type.setParameter("type", type);
        return numTotal_type.getSingleResult().intValue();
    }

    @Override
    public int numUsed() {
        return numUsed.getSingleResult().intValue();
    }

    @Override
    public int numUsed(final Spot.Type type) {
        numUsed_type.setParameter("type", type);
        return numUsed_type.getSingleResult().intValue();
    }

    @Override
    public Optional<Spot> nextFree(final Spot.Type type) {
        nextFree.setParameter("type", type);
        return nextFree.getResultStream().findFirst();
    }

    @Override
    public void run() {
        port(config.port());
        get("/", new TemplateViewRoute() {
            final Map<String, Object> templateValues = new HashMap<>();
            final ModelAndView modelAndView = modelAndView(templateValues, "index.html.mustache");

            @Override
            public ModelAndView handle(final Request request, final Response response) throws Exception {
                templateValues.put("numTotal", numTotal());
                templateValues.put("numUsed", numUsed());
                templateValues.put("numFree", numFree());
                return modelAndView;
            }
        }, new MustacheTemplateEngine());
    }

    public static void main(final String[] args) {
        new GarageImpl() {
            @Override
            protected void finalize() throws Throwable {
                close();
            }
        }.run();
    }
}
