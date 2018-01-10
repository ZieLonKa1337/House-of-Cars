package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** @author rstumm2s */
public interface Config {
    int port();
    String jdbcUrl();
    String jdbcUser();
    String jdbcPassword();
    Currency currency();
    Map<Spot.Type, BigDecimal> fee();
    Limit limit();

    /** @param other the instance to merge into this instance
     * @param preserveOther if {@code true}, entries from
     *     the other instance are preserved instead of this
     * @return a new or identical instance */
    default Config merge(final Config other, final boolean preserveOther) {
        if (equals(other)) return this;

        int port;
        String jdbcUrl, jdbcUser, jdbcPassword;
        String currencyName; Integer currencyScale;
        final Map<Spot.Type, BigDecimal> fee = new HashMap<>();
        Duration limitReminder, limitOverdue;

        final Config innerOther;
        if (preserveOther) {
            port = other.port();
            jdbcUrl = other.jdbcUrl();
            jdbcUser = other.jdbcUser();
            jdbcPassword = other.jdbcPassword();
            currencyName = other.currency().name();
            currencyScale = other.currency().scale();
            fee.putAll(fee());
            fee.putAll(other.fee());
            limitReminder = other.limit().reminder();
            limitOverdue = other.limit().overdue();
            innerOther = this;
        } else {
            port = port();
            jdbcUrl = jdbcUrl();
            jdbcUser = jdbcUser();
            jdbcPassword = jdbcPassword();
            currencyName = currency().name();
            currencyScale = currency().scale();
            fee.putAll(other.fee());
            fee.putAll(fee());
            limitReminder = limit().reminder();
            limitOverdue = limit().overdue();
            innerOther = other;
        }
        if (port == 0) port = innerOther.port();
        if (jdbcUrl == null) jdbcUrl = innerOther.jdbcUrl();
        if (jdbcUser == null) jdbcUser = innerOther.jdbcUser();
        if (jdbcPassword == null) jdbcPassword = innerOther.jdbcPassword();
        if (currencyName == null) currencyName = innerOther.currency().name();
        if (currencyScale == null) currencyScale = innerOther.currency().scale();
        // fee already merged
        if (limitReminder == null) limitReminder = innerOther.limit().reminder();
        if (limitOverdue == null) limitOverdue = innerOther.limit().overdue();

        return new AbstractConfig(
            port,
            jdbcUrl, jdbcUser, jdbcPassword,
            new AbstractConfig.AbstractCurrency(currencyName, currencyScale),
            fee,
            new AbstractConfig.AbstractLimit(limitReminder, limitOverdue));
    }

    interface Limit {
        Duration reminder();
        Duration overdue();
    }

    interface Currency {
        String name();
        Integer scale();
    }
}
