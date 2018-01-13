package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Vehicle;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;

/** @author rstumm2s */
public class CustomerLogin implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;

    private Customer customer;

    @Override
    public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        final NameCallback license;
        final PasswordCallback password;
        try {
            license = new NameCallback(" ");
            password = new PasswordCallback(" ", false);
            callbackHandler.handle(new Callback[] {license, password});
        } catch (final IOException | UnsupportedCallbackException e) {
            throw new AssertionError(e);
        }

        final Vehicle vehicle = Garage.instance().persistence.execute(em -> em.find(Vehicle.class, license.getName()));
        final Customer c = vehicle.owner().orElseThrow(() -> new LoginException("wrong license: " + license.getName() + " has no owner"));
        try {
            if (!BCrypt.checkpw(new String(password.getPassword()), c.pass()))
                throw new LoginException("wrong password");
            customer = c;
            return true;
        } finally {
            password.clearPassword();
        }
    }

    @Override
    public boolean commit() {
        if (customer != null) {
            subject.getPrincipals().add(customer);
            return true;
        }
        return false;
    }

    @Override
    public boolean abort() {
        customer = null;
        return true;
    }

    @Override
    public boolean logout() {
        assert customer != null : "not logged in";

        subject.getPrincipals().remove(customer);
        customer = null;

        subject.getPublicCredentials().clear();
        subject.getPrivateCredentials().clear();
        return true;
    }
}
