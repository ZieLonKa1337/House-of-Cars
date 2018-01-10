package de.codazz.houseofcars;

import com.esotericsoftware.jsonbeans.Json;
import de.codazz.houseofcars.domain.Spot;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
public class JsonConfig extends AbstractConfig {
    private static final Json json = new Json();

    public static JsonConfig load(final InputStream input) {
        return json.fromJson(JsonConfig.class, input);
    }

    public JsonConfig() {
        // TODO make JsonConfig generic - move defaults somewhere else
        super(
            80,
            "jdbc:postgresql://localhost:5432/houseofcars", "houseofcars", null,
            null,
            null,
            null
        );
    }

    @Override
    public Map<Spot.Type, BigDecimal> fee() {
        if (_fee == null) {
            try {
                /* if loaded from JSON type parameters are erased
                 * so fee could actually be a Map<String, Float>
                 * if the user used float notation */
                @SuppressWarnings("unchecked")
                final Map<String, Float> floatFee = (Map<String, Float>) fee.clone();

                _fee = new HashMap<>(Spot.Type.values().length, 1);
                floatFee.forEach((key, value) ->
                    _fee.put(Spot.Type.valueOf(key), new BigDecimal(value))
                );
                _fee = Collections.unmodifiableMap(_fee);
            } catch (final ClassCastException ignore) {
                // is correctly typed
            }
        }
        return super.fee();
    }
}
