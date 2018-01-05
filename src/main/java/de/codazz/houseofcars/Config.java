package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
public interface Config {
    int port();
    String jdbcUrl();
    String jdbcUser();
    String jdbcPassword();
    Map<Spot.Type, BigDecimal> fee();

    /** @param other the instance to merge into this instance
     * @param preserveOther if {@code true}, entries from
     *     the other instance are preserved instead of this
     * @return a new or identical instance */
    default Config merge(final Config other, final boolean preserveOther) {
        if (equals(other)) return this;

        int port;
        String jdbcUrl, jdbcUser, jdbcPassword;
        final Map<Spot.Type, BigDecimal> fee = new HashMap<>();

        final Config innerOther;
        if (preserveOther) {
            port = other.port();
            jdbcUrl = other.jdbcUrl();
            jdbcUser = other.jdbcUser();
            jdbcPassword = other.jdbcPassword();
            fee.putAll(fee());
            fee.putAll(other.fee());
            innerOther = this;
        } else {
            port = port();
            jdbcUrl = jdbcUrl();
            jdbcUser = jdbcUser();
            jdbcPassword = jdbcPassword();
            fee.putAll(other.fee());
            fee.putAll(fee());
            innerOther = other;
        }
        if (port == 0) port = innerOther.port();
        if (jdbcUrl == null) jdbcUrl = innerOther.jdbcUrl();
        if (jdbcUser == null) jdbcUser = innerOther.jdbcUser();
        if (jdbcPassword == null) jdbcPassword = innerOther.jdbcPassword();
        // fee already merged

        return new AbstractConfig(port, jdbcUrl, jdbcUser, jdbcPassword, fee);
    }
}
