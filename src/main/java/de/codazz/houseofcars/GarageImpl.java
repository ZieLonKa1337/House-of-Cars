package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.websocket.subprotocol.Gate;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;
import spark.TemplateViewRoute;
import spark.template.mustache.MustacheTemplateEngine;

import javax.persistence.TypedQuery;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

/** @author rstumm2s */
public class GarageImpl implements Garage, Closeable {
    private static final String CONFIG_FILE = "house-of-cars.json";

    static GarageImpl instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static GarageImpl instance() {
        return instance;
    }

    public final Persistence persistence;

    private final Config config;

    private final TypedQuery<Long>
            numTotal, numTotal_type,
            numVehicles,
            numUsed, numUsed_type,
            numParking;
    private final TypedQuery<Spot> nextFree;

    /** whether the {@link spark.Spark spark} has {@link spark.Service#ignite() ignited} */
    private boolean spark;

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

        persistence = new Persistence(config.jdbcUrl(), config.jdbcUser(), config.jdbcPassword());

        // build queries
        numTotal = persistence.execute(em -> em.createNamedQuery("Spot.count", Long.class));
        numTotal_type = persistence.execute(em -> em.createNamedQuery("Spot.countType", Long.class));
        numUsed = persistence.execute(em -> em.createNamedQuery("Spot.countUsed", Long.class));
        numUsed_type = persistence.execute(em -> em.createNamedQuery("Spot.countUsedType", Long.class));
        numParking = persistence.execute(em -> em.createNamedQuery("Parking.countParking", Long.class));
        numVehicles = persistence.execute(em -> em.createNamedQuery("Vehicle.countPresent", Long.class));
        nextFree = persistence.execute(em -> em.createNamedQuery("Spot.anyFree", Spot.class)
                .setMaxResults(1));
    }

    @Override
    public int numTotal() {
        return persistence.execute(__ -> numTotal.getSingleResult().intValue());
    }

    @Override
    public int numTotal(final Spot.Type type) {
        return persistence.execute(__ -> numTotal_type
                .setParameter("type", type)
                .getSingleResult().intValue());
    }

    @Override
    public int numUsed() {
        return persistence.execute(__ -> numUsed.getSingleResult().intValue());
    }

    @Override
    public int numUsed(final Spot.Type type) {
        return persistence.execute(__ -> numUsed_type
                .setParameter("type", type)
                .getSingleResult().intValue());
    }

    @Override
    public int numParking() {
        return persistence.execute(__ -> numParking.getSingleResult().intValue());
    }

    @Override
    public int numVehicles() {
        return persistence.execute(__ -> numVehicles.getSingleResult().intValue());
    }

    @Override
    public Optional<Spot> nextFree(final Spot.Type type) {
        return persistence.execute(__ -> nextFree
                .setParameter("type", type)
                .getResultStream().findFirst());
    }

    @Override
    public void run() {
        spark = true;

        port(config.port());
        staticFiles.location("/static");

        webSocket("/ws/status", Status.class);
        webSocket("/ws/gate", Gate.class);

        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        get("/", new TemplateViewRoute() {
            final Map<String, Object> templateValues = new HashMap<>();
            final ModelAndView modelAndView = modelAndView(templateValues, "index.html.mustache");

            @Override
            public ModelAndView handle(final Request request, final Response response) {
                templateValues.put("numTotal", numTotal());
                templateValues.put("numUsed", numUsed());
                templateValues.put("numFree", numFree());
                templateValues.put("numVehicles", numVehicles());
                templateValues.put("numPending", numPending());
                templateValues.put("numParking", numParking());
                templateValues.put("numLeaving", numLeaving());
                return modelAndView;
            }
        }, templateEngine);
        get("/vgate", new TemplateViewRoute() {
            final Map<String, Object> templateValues = new HashMap<>(); {
                templateValues.put("spotTypes", Arrays.stream(Spot.Type.values()).map(Spot.Type::name).toArray(String[]::new));
            }
            final ModelAndView modelAndView = modelAndView(templateValues, "vgate.html.mustache");

            @Override
            public ModelAndView handle(final Request request, final Response response) {
                return modelAndView;
            }
        }, templateEngine);
    }

    @Override
    public void close() {
        if (spark) {
            spark = false;
            stop();
        }
        persistence.close();

        if (instance == this) {
            instance = null;
        }
    }

    public static void main(final String[] args) {
        new GarageImpl().run();
    }
}
