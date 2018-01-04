package de.codazz.houseofcars;

import java.math.BigDecimal;

/** @author rstumm2s */
public interface Config {
    int port();
    String jdbcUrl();
    String jdbcUser();
    String jdbcPassword();
    BigDecimal fee();

    /** @param other the instance to merge into this
     * @param intoOther if {@code true}, merge this into {@code other}
     * @return a new or identical instance */
    default Config merge(final Config other, final boolean intoOther) {
        if (equals(other)) return this;

        int port;
        String jdbcUrl, jdbcUser, jdbcPassword;
        BigDecimal fee;

        final Config innerOther;
        if (intoOther) {
            port = other.port();
            jdbcUrl = other.jdbcUrl();
            jdbcUser = other.jdbcUser();
            jdbcPassword = other.jdbcPassword();
            fee = other.fee();
            innerOther = this;
        } else {
            port = port();
            jdbcUrl = jdbcUrl();
            jdbcUser = jdbcUser();
            jdbcPassword = jdbcPassword();
            fee = fee();
            innerOther = other;
        }
        if (port == 0) port = innerOther.port();
        if (jdbcUrl == null) jdbcUrl = innerOther.jdbcUrl();
        if (jdbcUser == null) jdbcUser = innerOther.jdbcUser();
        if (jdbcPassword == null) jdbcPassword = innerOther.jdbcPassword();
        if (fee == null) fee = innerOther.fee();

        return new AbstractConfig(port, jdbcUrl, jdbcUser, jdbcPassword, fee) {};
    }
}
