package de.codazz.houseofcars;

import de.codazz.houseofcars.service.Monitor;
import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Spot;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.service.Sessions;
import de.codazz.houseofcars.websocket.subprotocol.Gate;
import de.codazz.houseofcars.websocket.subprotocol.History;
import de.codazz.houseofcars.websocket.subprotocol.Statistics;
import de.codazz.houseofcars.websocket.subprotocol.Status;
import de.codazz.houseofcars.websocket.subprotocol.VGate;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.TemplateEngine;
import spark.template.mustache.MustacheTemplateEngine;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/** @author rstumm2s */
public class Garage implements Runnable, Closeable {
    static {
        System.setProperty("java.security.auth.login.config", Garage.class.getClassLoader().getResource("jaas.conf").toString());
    }

    private static final String CONFIG_FILE = "house-of-cars.json";

    static volatile Garage instance; {
        if (instance != null) throw new IllegalStateException();
        instance = this;
    }

    public static Garage instance() {
        return instance;
    }

    public final Config config;

    public final Persistence persistence;
    public final Monitor monitor;
    public final Sessions sessions;

    /** whether the {@link spark.Spark spark} has {@link spark.Service#ignite() ignited} */
    private boolean spark;

    public Garage() {
        this(null);
    }

    public Garage(Config config) {
        if (config == null) {
            config = new JsonConfig();
            try {
                config = JsonConfig.load(new FileInputStream(CONFIG_FILE)).merge(config, false);
            } catch (final FileNotFoundException ignore) {}
        }
        this.config = config;

        persistence = new Persistence(config.jdbcUrl(), config.jdbcUser(), config.jdbcPassword());

        // check that all spot types have a fee configured
        for (final Spot.Type type : Spot.Type.values()) {
            if (Spot.count(type) > 0 && config.fee().get(type) == null) {
                throw new IllegalStateException("no fee configured for " + type + "spots!");
            }
        }

        monitor = new Monitor();
        sessions = new Sessions();
    }

    @Override
    public void run() {
        spark = true;

        port(config.port());
        staticFiles.location("/static");

        webSocket("/ws/status", Status.class);
        webSocket("/ws/status/history", History.class);
        webSocket("/ws/status/monitor", de.codazz.houseofcars.websocket.subprotocol.Monitor.class);
        webSocket("/ws/statistics", Statistics.class);
        webSocket("/ws/gate", Gate.class);
        webSocket("/ws/vgate", VGate.class);

        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        get("/", new Route() {
            final ThreadLocal<ModelAndView> modelAndView = ThreadLocal.withInitial(() ->
                modelAndView(new HashMap<>(Status.templateDefaults), "index.html.mustache")
            );

            @Override
            public Object handle(final Request request, final Response response) throws LoginException {
                final ModelAndView modelAndView = this.modelAndView.get();
                @SuppressWarnings("unchecked")
                final Map<String, Object> templateValues = (Map<String, Object>) modelAndView.getModel();

                synchronized (this) {
                    final Session session = request.session(false);
                    if (session != null) {
                        if (request.queryParams().contains("logout")) {
                            final Subject subject = session.attribute("subject");
                            sessions.logout(subject);
                            session.invalidate();
                        } else if (session.attributes().contains("ui")) {
                            templateValues.put("session", session.attribute("ui"));
                        }
                    }
                }

                try {
                    return templateEngine.render(modelAndView);
                } finally {
                    templateValues.remove("session");
                }
            }
        });
        post("/", (request, response) -> { // login
            if (request.session(false) != null) return null;
            final String
                license = request.queryParams("license"),
                pass    = request.queryParams("pass");

            final Vehicle vehicle = persistence.execute(em -> em.find(Vehicle.class, license));
            if (vehicle != null) {
                // find or create customer
                final Customer customer = vehicle.owner().orElseGet(() -> sessions.register(license, pass));

                try { // authenticate
                    request.session().attribute("subject", sessions.login(license, pass));
                } catch (final LoginException ignore) { /* login failed */ }

                if (request.session(false) != null) { // login successful
                    persistence.<Void>execute(em -> {
                        em.refresh(customer);
                        return null;
                    });

                    final Map<String, Object> ui = new HashMap<>();
                    ui.put("customer", customer);
                    request.session().attribute("ui", ui);
                }
            } // TODO show message that only known vehicles may be registered

            response.redirect("/");
            return null;
        });
        get("/dashboard", new Route() {
            final Map<String, Object> templateValues = new HashMap<>(History.templateDefaults.size() + de.codazz.houseofcars.websocket.subprotocol.Monitor.templateDefaults.size(), 1); {
                templateValues.putAll(History.templateDefaults);
                templateValues.putAll(de.codazz.houseofcars.websocket.subprotocol.Monitor.templateDefaults);
            }
            final ModelAndView modelAndView = modelAndView(templateValues, "dashboard.html.mustache");

            @Override
            public Object handle(final Request request, final Response response) {
                return templateEngine.render(modelAndView);
            }
        });
        get("/vgate", new Route() {
            final ModelAndView modelAndView = modelAndView(Status.templateDefaults, "vgate.html.mustache");

            @Override
            public Object handle(final Request request, final Response response) {
                return templateEngine.render(modelAndView);
            }
        });

        monitor.run();
    }

    @Override
    public void close() {
        if (spark) {
            spark = false;
            stop();

            Status.close();
            History.close();
            de.codazz.houseofcars.websocket.subprotocol.Monitor.close();
        }
        persistence.close();
        monitor.close();

        if (instance == this) {
            instance = null;
        }
    }

    public static void main(final String[] args) {
        final Garage garage = new Garage();
        garage.run();
        Runtime.getRuntime().addShutdownHook(new Thread(garage::close, "dispose resources"));
    }
}
