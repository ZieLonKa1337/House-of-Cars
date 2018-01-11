package de.codazz.houseofcars.template;

import de.codazz.houseofcars.Config;
import de.codazz.houseofcars.Garage;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** @author rstumm2s */
public class Price {
    public final BigDecimal value;

    public Price(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return toString(value);
    }

    /** @return the actual price to charge */
    public static String toString(final BigDecimal price) {
        final Config.Currency currency = Garage.instance().config.currency();
        return price
            .setScale(currency.scale(), RoundingMode.DOWN)
            .toPlainString()
                + currency.name();
    }
}
