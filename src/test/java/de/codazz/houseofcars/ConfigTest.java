package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** @author rstumm2s */
public class ConfigTest {
    @Test
    public void merge() {
        final Map<Spot.Type, BigDecimal>
            fee1 = new HashMap<>(),
            fee2 = new HashMap<>();
        fee1.put(Spot.Type.CAR, BigDecimal.valueOf(1));
        fee2.put(Spot.Type.CAR, BigDecimal.valueOf(2));

        final Config
            c1 = new AbstractConfig(
                1,
                "jdbc:1", "1", "1",
                new AbstractConfig.AbstractCurrency("1", 1),
                fee1,
                new AbstractConfig.AbstractLimit(Duration.ofHours(1), Duration.ofDays(1))
            ),
            c2 = new AbstractConfig(
                2,
                "jdbc:2", "2", "2",
                new AbstractConfig.AbstractCurrency("2", 2),
                fee2,
                new AbstractConfig.AbstractLimit(Duration.ofHours(2), Duration.ofDays(2))
            );

        final Config
            m1_2  = c1.merge(c2, false),
            m1_2i = c1.merge(c2, true),
            m2_1  = c2.merge(c1, false),
            m2_1i = c2.merge(c1, true);

        // m1_2
        assertEquals(c1.port(),         m1_2.port());
        assertEquals(c1.jdbcUrl(),      m1_2.jdbcUrl());
        assertEquals(c1.jdbcUser(),     m1_2.jdbcUser());
        assertEquals(c1.jdbcPassword(), m1_2.jdbcPassword());
        assertEquals(c1.currency(),     m1_2.currency());
        assertEquals(c1.fee(),          m1_2.fee());
        assertEquals(c1.limit(),        m1_2.limit());

        // m2_1
        assertEquals(c2.port(),         m2_1.port());
        assertEquals(c2.jdbcUrl(),      m2_1.jdbcUrl());
        assertEquals(c2.jdbcUser(),     m2_1.jdbcUser());
        assertEquals(c2.jdbcPassword(), m2_1.jdbcPassword());
        assertEquals(c2.currency(),     m2_1.currency());
        assertEquals(c2.fee(),          m2_1.fee());
        assertEquals(c2.limit(),        m2_1.limit());

        // symmetry
        assertEquals(m2_1, m1_2i);
        assertEquals(m1_2, m2_1i);
    }
}
