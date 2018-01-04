package de.codazz.houseofcars;

import com.esotericsoftware.jsonbeans.Json;
import de.codazz.houseofcars.domain.Spot;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** @author rstumm2s */
public class ConfigImpl extends AbstractConfig {
    private static final Json json = new Json();

    public static Config load(final InputStream input) {
        return json.fromJson(ConfigImpl.class, input);
    }

    public ConfigImpl() {
        super(
            80,
            "jdbc:postgresql://localhost:5432/houseofcars", "houseofcars", null,
            new HashMap<>(Spot.Type.values().length, 1)
        );
    }

    @Override
    public Map<Spot.Type, BigDecimal> fee() {
        if (_fee == null) {
            _fee = new HashMap<>(Spot.Type.values().length, 1);

            /* if loaded from JSON type parameters are erased
             * so fee could actually be a Map<String, Float>
             * if the user used float notation */
            final BiConsumer<String, Object> parseEntry = (key, value) -> {
                final BigDecimal amount;
                if (value instanceof Float) {
                    amount = new BigDecimal((Float) value);
                } else {
                    assert value instanceof String;
                    amount = new BigDecimal((String) value);
                }
                _fee.put(Spot.Type.valueOf(key), amount);
            };
            try {
                @SuppressWarnings("unchecked") final Map<String, Float> floatFee = (Map<String, Float>) fee.clone();
                floatFee.forEach(parseEntry);
            } catch (final ClassCastException wasCorrectlyTyped) {
                fee.forEach(parseEntry);
            }

            _fee = Collections.unmodifiableMap(_fee);
        }
        return super.fee();
    }
}
