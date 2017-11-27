package de.codazz.houseofcars;

import java.util.Objects;

/** @author rstumm2s */
abstract class AbstractConfig implements Config {
    private int port;
    private String jdbcUrl, jdbcUser, jdbcPassword;

    AbstractConfig(final int port,
            final String jdbcUrl, final String jdbcUser, final String jdbcPassword) {
        this.port = port;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractConfig)) return false;
        final AbstractConfig that = (AbstractConfig) o;
        return port() == that.port() &&
                Objects.equals(jdbcUrl(), that.jdbcUrl()) &&
                Objects.equals(jdbcUser(), that.jdbcUser()) &&
                Objects.equals(jdbcPassword(), that.jdbcPassword());
    }
}
