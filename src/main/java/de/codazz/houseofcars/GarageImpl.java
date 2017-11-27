package de.codazz.houseofcars;

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

    private final TypedQuery<? extends Number> numTotal, numUsed = null, numFree = null;
    private final TypedQuery<Spot> nextFree = null;

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
//        numUsed = entityManager.createQuery("SELECT COUNT(s) FROM Spot s WHERE ", Long.class); // TODO
//        numFree = entityManager.createQuery("SELECT COUNT(s) FROM Spot s WHERE ", Long.class); // TODO
//        nextFree = entityManager.createQuery("SELECT s FROM Spot s WHERE ", Spot.class); // TODO
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
    public int numUsed() {
        return numUsed.getSingleResult().intValue();
    }

    @Override
    public int numFree() {
        return numFree.getSingleResult().intValue();
    }

    @Override
    public Optional<Spot> nextFree() {
        return Optional.ofNullable(nextFree.getSingleResult());
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
//                templateValues.put("numUsed", numUsed()); // TODO
//                templateValues.put("numFree", numFree()); // TODO
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
