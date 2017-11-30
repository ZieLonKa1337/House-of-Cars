package de.codazz.houseofcars;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** @author rstumm2s */
public class PerstistenceUnitInfoImpl implements PersistenceUnitInfo {
    private final String persistenceUnitName, persistenceProviderClassName;

    public PerstistenceUnitInfoImpl(final String persistenceUnitName, final String persistenceProviderClassName) {
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceProviderClassName = persistenceProviderClassName;
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persistenceProviderClassName;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public DataSource getJtaDataSource() {
        return null;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return null;
    }

    @Override
    public List<String> getMappingFileNames() {
        return null;
    }

    @Override
    public List<URL> getJarFileUrls() {
        // only works if running from an exploded JAR
        // so we still have to list entities in getManagedClassNames()
        try {
            return Collections.list(getClass().getClassLoader().getResources(""));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    @Override
    public List<String> getManagedClassNames() {
        final List<String> classes = new ArrayList<>();
        classes.add("de.codazz.houseofcars.domain.Spot");
        classes.add("de.codazz.houseofcars.domain.LicensePlate");
        classes.add("de.codazz.houseofcars.domain.Parking");
        return Collections.unmodifiableList(classes);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return null;
    }

    @Override
    public ValidationMode getValidationMode() {
        return null;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void addTransformer(final ClassTransformer transformer) {}

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
