package de.codazz.houseofcars;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.Duration;
import java.util.Objects;

/** @author rstumm2s */
@Converter(autoApply = true)
public class DurationPersistenceConverter implements AttributeConverter<Duration, String> {
    @Override
    public String convertToDatabaseColumn(final Duration attribute) {
        return Objects.toString(attribute, null);
    }

    @Override
    public Duration convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : Duration.parse(dbData);
    }
}
