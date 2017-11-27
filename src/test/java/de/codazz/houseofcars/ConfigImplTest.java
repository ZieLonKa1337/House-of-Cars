package de.codazz.houseofcars;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/** @author rstumm2s */
public class ConfigImplTest {
    static final String CONFIG = "src/test/resources/config-test.json";

    static Config config;

    @BeforeClass
    public static void setUp() throws IOException {
        config = ConfigImpl.load(new FileInputStream(CONFIG));
    }

    @Test
    public void defaults() {
        final Config config = new ConfigImpl();
        assertEquals(80, config.port());
        assertEquals("jdbc:postgresql://localhost:5432/houseofcars", config.jdbcUrl());
        assertEquals("houseofcars", config.jdbcUser());
        assertNull(config.jdbcPassword());
    }

    @Test
    public void load() {
        assertEquals(5555, config.port());
        assertEquals("jdbc:postgresql://localhost:5432/houseofcars-test", config.jdbcUrl());
        assertEquals("houseofcars-test", config.jdbcUser());
        assertEquals("h-brs", config.jdbcPassword());
    }
}
