package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** @author rstumm2s */
public class AbstractConfig implements Config {
    protected int port;
    protected String jdbcUrl, jdbcUser, jdbcPassword;
    // TODO serialize properly in JsonConfig so we we can directly hold _fee
    protected HashMap<String, String> fee;
    protected transient Map<Spot.Type, BigDecimal> _fee;
    protected AbstractLimit limit;

    /** Creates an uninitialized instance.
     * It is up to the subclass or caller
     * to prepare it for usage. */
    protected AbstractConfig() {}

    public AbstractConfig(
        final int port,
        final String jdbcUrl, final String jdbcUser, final String jdbcPassword,
        final Map<Spot.Type, BigDecimal> fee,
        final Limit limit
    ) {
        this.port = port;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
        this.fee = new HashMap<>(Spot.Type.values().length, 1);
        if ((_fee = fee) != null) {
            fee.forEach((key, value) -> this.fee.put(key.name(), value.toPlainString()));
        }
        this.limit = limit instanceof AbstractLimit
            ? (AbstractLimit) limit
            : limit == null
                ? new AbstractLimit()
                : new AbstractLimit(limit);
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String jdbcUser() {
        return jdbcUser;
    }

    @Override
    public String jdbcPassword() {
        return jdbcPassword;
    }

    @Override
    public Map<Spot.Type, BigDecimal> fee() {
        if (_fee == null) {
            _fee = new HashMap<>(Spot.Type.values().length, 1);
            fee.forEach((key, value) -> _fee.put(Spot.Type.valueOf(key), new BigDecimal(value)));
            _fee = Collections.unmodifiableMap(_fee);
        }
        return _fee;
    }

    @Override
    public Limit limit() {
        return limit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractConfig)) return false;
        final AbstractConfig that = (AbstractConfig) o;
        return port() == that.port() &&
            Objects.equals(jdbcUrl(), that.jdbcUrl()) &&
            Objects.equals(jdbcUser(), that.jdbcUser()) &&
            Objects.equals(jdbcPassword(), that.jdbcPassword()) &&
            Objects.equals(fee(), that.fee());
    }

    public static class AbstractLimit implements Limit {
        // TODO serialize properly in JsonConfig so we can directly hold _reminder and _overdue
        private String reminder, overdue;
        private transient Duration _reminder, _overdue;

        /** copy constructor */
        public AbstractLimit(final Limit limit) {
            reminder = limit.reminder().toString();
            overdue = limit.overdue().toString();
        }

        /** no limits */
        public AbstractLimit() {}

        public AbstractLimit(final Duration reminder, final Duration overdue) {
            _reminder = reminder;
            _overdue = overdue;
        }

        @Override
        public Duration reminder() {
            if (_reminder == null && reminder != null) {
                _reminder = Duration.parse(reminder);
            }
            return _reminder;
        }

        @Override
        public Duration overdue() {
            if (_overdue == null && overdue != null) {
                _overdue = Duration.parse(overdue);
            }
            return _overdue;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractLimit)) return false;
            final AbstractLimit that = (AbstractLimit) o;
            return Objects.equals(reminder(), that.reminder()) &&
                Objects.equals(overdue(), that.overdue());
        }
    }
}
