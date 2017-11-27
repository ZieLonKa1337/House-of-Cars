package de.codazz.houseofcars;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** @author rstumm2s */
public class ConfigTest {
    @Test
    public void merge() {
        final Config c1 = new AbstractConfig(1, "jdbc:1", "1", "1") {},
                c2 = new AbstractConfig(2, "jdbc:2", "2", "2") {};

        final Config
                m1_2 = c1.merge(c2, false),
                m1_2i = c1.merge(c2, true),
                m2_1 = c2.merge(c1, false),
                m2_1i = c2.merge(c1, true);

        // m1_2
        assertEquals(c1.port(), m1_2.port());
        assertEquals(c1.jdbcUrl(), m1_2.jdbcUrl());
        assertEquals(c1.jdbcUser(), m1_2.jdbcUser());
        assertEquals(c1.jdbcPassword(), m1_2.jdbcPassword());

        // m2_1
        assertEquals(c2.port(), m1_2i.port());
        assertEquals(c2.jdbcUrl(), m1_2i.jdbcUrl());
        assertEquals(c2.jdbcUser(), m1_2i.jdbcUser());
        assertEquals(c2.jdbcPassword(), m1_2i.jdbcPassword());

        // symmetry
        assertEquals(m2_1, m1_2i);
        assertEquals(m1_2, m2_1i);
    }
}
