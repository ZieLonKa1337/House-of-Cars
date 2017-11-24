package de.codazz.houseofcars;

import com.esotericsoftware.jsonbeans.Json;

import java.io.InputStream;

/** @author rstumm2s */
public class ConfigImpl extends AbstractConfig {
    private static final Json json = new Json();

    public static Config load(final InputStream input) {
        return json.fromJson(ConfigImpl.class, input);
    }

    public ConfigImpl() {
        super(80, "jdbc:postgresql://localhost:5432/houseofcars", null);
    }
}
