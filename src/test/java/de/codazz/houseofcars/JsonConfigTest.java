package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class JsonConfigTest {
    static final String CONFIG = "src/test/resources/config-test.json";

    static Config config;

    @BeforeClass
    public static void setUp() throws IOException {
        config = JsonConfig.load(new FileInputStream(CONFIG));
    }

    @Test
    public void defaults() {
        final Config config = new JsonConfig();

        assertEquals(80, config.port());

        assertEquals("jdbc:postgresql://localhost:5432/houseofcars", config.jdbcUrl());
        assertEquals("houseofcars"                                 , config.jdbcUser());
        assertNull(config.jdbcPassword());

        assertEquals(0, config.fee().size());
    }

    @Test
    public void load() {
        assertEquals(5555, config.port());

        assertEquals("jdbc:postgresql://localhost:5432/houseofcars-test", config.jdbcUrl());
        assertEquals("houseofcars-test"                                 , config.jdbcUser());
        assertEquals("h-brs"                                            , config.jdbcPassword());

        assertEquals(3, config.fee().size());
        assertEquals(Spot.Type.CAR     .ordinal() + 1, config.fee().get(Spot.Type.CAR)     .intValueExact());
        assertEquals(Spot.Type.BIKE    .ordinal() + 1, config.fee().get(Spot.Type.BIKE)    .intValueExact());
        assertEquals(Spot.Type.HANDICAP.ordinal() + 1, config.fee().get(Spot.Type.HANDICAP).intValueExact());
    }
}
