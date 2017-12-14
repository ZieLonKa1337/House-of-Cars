package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.websocket.subprotocol.Gate;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import de.codazz.houseofcars.websocket.subprotocol.VGate;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;
import spark.TemplateViewRoute;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/** @author rstumm2s */
public class Garage implements Runnable, Closeable {
    private static final String CONFIG_FILE = "house-of-cars.json";

    static volatile Garage instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static Garage instance() {
        return instance;
    }

    public final Persistence persistence;

    private final Config config;

    /** whether the {@link spark.Spark spark} has {@link spark.Service#ignite() ignited} */
    private boolean spark;

    public Garage() {
        this(null);
    }

    public Garage(Config config) {
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
    }

    @Override
    public void run() {
        spark = true;

        port(config.port());
        staticFiles.location("/static");

        webSocket("/ws/status", Status.class);
        webSocket("/ws/gate", Gate.class);
        webSocket("/ws/vgate", VGate.class);

        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        get("/", new TemplateViewRoute() {
            final Map<String, Object> templateValues = new HashMap<>();
            final ModelAndView modelAndView = modelAndView(templateValues, "index.html.mustache");

            @Override
            public ModelAndView handle(final Request request, final Response response) {
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
        new Garage().run();
    }
}
