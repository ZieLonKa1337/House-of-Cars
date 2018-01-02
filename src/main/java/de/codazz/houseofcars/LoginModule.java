package de.codazz.houseofcars;

import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Vehicle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/** @author rstumm2s */
public class LoginModule implements javax.security.auth.spi.LoginModule {
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
        final Customer c = vehicle.owner().orElseThrow(LoginException::new);
        try {
            if (!Arrays.equals(c.pass().toCharArray(), password.getPassword()))
                // TODO check against salted hash
                throw new LoginException("wrong password");
            customer = c;
            return true;
        } finally {
            password.clearPassword();
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (customer != null) {
            subject.getPrincipals().add(customer);
            return true;
        }
        return false;
    }

    @Override
    public boolean abort() throws LoginException {
        customer = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(customer);
        customer = null;
        return true;
    }
}
