package de.codazz.houseofcars.websocket.subprotocol;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import com.esotericsoftware.jsonbeans.JsonWriter;
import com.esotericsoftware.jsonbeans.OutputType;
import de.codazz.houseofcars.Config;
import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Vehicle;
import de.codazz.houseofcars.domain.VehicleTransition;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** @author rstumm2s */
@WebSocket
public class Statistics {
    private static final JsonReader jsonReader = new JsonReader();

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {
        final JsonValue msg = jsonReader.parse(message);
        final String
            startString = msg.getString("start"),
            endString = msg.getString("end");
        session.getRemote().sendString(statistics(
            startString != null ? ZonedDateTime.parse(startString) : null,
            endString   != null ? ZonedDateTime.parse(endString)   : null
        ));
    }

    // TODO can the db do some of this?
    private static String statistics(final ZonedDateTime start, final ZonedDateTime end) throws IOException {
        final String qlString; {
            final StringBuilder sb = new StringBuilder("SELECT t FROM VehicleTransition t WHERE TRUE = TRUE ");
            if (start != null || end != null) {
                sb.append("AND ");
            }
            if (start != null && end != null) {
                sb.append("t.time BETWEEN :start AND :end");
            } else if (start != null) {
                sb.append("t.time >= :start");
            } else if (end != null) {
                sb.append("t.time <= :end");
            }
            qlString = sb.toString();
        }
        final Consumer<TypedQuery> setTimeParams = query -> {
            if (start != null) {
                query.setParameter("start", start);
            }
            if (end != null) {
                query.setParameter("end", end);
            }
        };

        final List<VehicleTransition> unpaidTransitions = Garage.instance().persistence.execute(em -> {
            final TypedQuery<VehicleTransition> query =
                em.createQuery(qlString + " AND t.data.paid = FALSE", VehicleTransition.class);
            setTimeParams.accept(query);
            return query.getResultList();
        });

        final Config.Currency currency = Garage.instance().config.currency();

        final JsonWriter jw = new JsonWriter(new StringWriter());
        jw.setOutputType(OutputType.json);
        jw.object()
            .set("revenue", Garage.instance().persistence.execute(em -> {
                final TypedQuery<VehicleTransition> query =
                        em.createQuery(qlString + " AND t.data.paid = TRUE", VehicleTransition.class);
                setTimeParams.accept(query);
                return query.getResultList();
            }).stream() // sum prices
                .map(VehicleTransition::price).filter(Optional::isPresent).map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(currency.scale(), RoundingMode.DOWN)
                    .toPlainString() +
                        currency.name())
            .set("fee-avg", ((Function<List<VehicleTransition>, String>) transitions -> transitions.isEmpty()
                ? "n/a"
                : transitions.stream()
                    .map(VehicleTransition::fee).map(Optional::get)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(transitions.size()), RoundingMode.DOWN)
                        .setScale(currency.scale(), RoundingMode.DOWN)
                        .toPlainString() +
                            currency.name()
            ).apply(Garage.instance().persistence.execute(em -> {
                final TypedQuery<VehicleTransition> query =
                    em.createQuery(qlString + " AND t.data.fee IS NOT NULL", VehicleTransition.class);
                setTimeParams.accept(query);
                return query.getResultList();
            })))
            .set("price-avg", unpaidTransitions.isEmpty()
                ? "n/a"
                : unpaidTransitions.stream()
                    .map(VehicleTransition::price).filter(Optional::isPresent).map(Optional::get)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(unpaidTransitions.size()), RoundingMode.DOWN)
                        .setScale(currency.scale(),  RoundingMode.DOWN)
                        .toPlainString() +
                            currency.name());
        for (final Vehicle.State state : Vehicle.State.values()) {
            jw.set("time-" + state.name(), ((Function<List<VehicleTransition>, String>) transitions -> {
                if (transitions.isEmpty()) return "n/a";

                class DurationHolder { Duration duration = Duration.ZERO; }
                final DurationHolder total = new DurationHolder();
                transitions.forEach(it -> it.next().ifPresent(next ->
                    total.duration = total.duration.plus(Duration.between(it.time(), next.time()))
                ));
                total.duration = total.duration.dividedBy(transitions.size());

                return de.codazz.houseofcars.template.Duration.toString(total.duration);
            }).apply(Garage.instance().persistence.execute(em -> {
                final TypedQuery<VehicleTransition> query = em
                    .createQuery(qlString + " AND t.state = :state ", VehicleTransition.class)
                    .setParameter("state", state);
                setTimeParams.accept(query);
                return query.getResultList();
            })));
        }
        jw.close();
        return jw.getWriter().toString();
    }
}
