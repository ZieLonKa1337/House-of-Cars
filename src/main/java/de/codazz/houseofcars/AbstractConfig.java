package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Spot;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** @author rstumm2s */
class AbstractConfig implements Config {
    protected int port;
    protected String jdbcUrl, jdbcUser, jdbcPassword;
    protected HashMap<String, String> fee;
    protected transient Map<Spot.Type, BigDecimal> _fee;

    AbstractConfig(
        final int port,
        final String jdbcUrl, final String jdbcUser, final String jdbcPassword,
        final Map<Spot.Type, BigDecimal> fee
    ) {
        this.port = port;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
        this.fee = new HashMap<>(Spot.Type.values().length, 1);
        if (Objects.requireNonNull(fee) != null) {
            fee.forEach((key, value) -> this.fee.put(key.name(), value.toPlainString()));
        }
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
}
