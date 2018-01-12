package de.codazz.houseofcars;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** @author rstumm2s */
public class MagicCookie {
    public final String value;

    public MagicCookie() {
        final byte[] magicBytes = new byte[(int) (8 + Math.random() * 8)];
        new SecureRandom().nextBytes(magicBytes);
        value = Base64.getEncoder().encodeToString(magicBytes);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MagicCookie)) return false;
        final MagicCookie that = (MagicCookie) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
