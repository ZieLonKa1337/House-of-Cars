package de.codazz.houseofcars.service;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Vehicle;
import org.mindrot.jbcrypt.BCrypt;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** @author rstumm2s */
public class Sessions {
    public final Map<Subject, LoginContext> sessions = new ConcurrentHashMap<>(8, .9f, 1);

    public Customer register(final String license, final String pass) {
        final Vehicle vehicle = Garage.instance().persistence.execute(em -> em.find(Vehicle.class, license));
        return Garage.instance().persistence.transact((em, __) -> {
            final Customer c = new Customer(
                BCrypt.hashpw(pass, BCrypt.gensalt()),
                vehicle
            );
            em.persist(c);
            return c;
        });
    }

    public Subject login(final String license, final String pass) throws LoginException {
        final LoginContext ctx = new LoginContext("default", callbacks -> {
            for (final Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(license);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(pass.toCharArray());
                }
            }
        });
        ctx.login();
        sessions.put(ctx.getSubject(), ctx);
        return ctx.getSubject();
    }

    public void logout(final Subject subject) throws LoginException {
        if (sessions.containsKey(subject)) {
            sessions.get(subject).logout();
        } else {
            throw new LoginException("not logged in");
        }
    }
}
