package de.codazz.houseofcars.service;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.MagicCookie;
import de.codazz.houseofcars.domain.Customer;
import de.codazz.houseofcars.domain.Vehicle;
import org.eclipse.jetty.websocket.api.Session;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** @author rstumm2s */
public class Sessions {
    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<Subject, LoginContext> sessions = Collections.synchronizedMap(new IdentityHashMap<>());

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
        log.trace("logged in {}", ctx.getSubject());
        return ctx.getSubject();
    }

    public void logout(final Subject subject) throws LoginException {
        log.trace("logging out {}", subject);
        final LoginContext ctx = sessions.get(subject);
        if (ctx != null) {
            ctx.logout();
        } else
            throw new LoginException("not logged in");
    }

    public Stream<Subject> present() {
        return sessions.keySet().stream();
    }
}
