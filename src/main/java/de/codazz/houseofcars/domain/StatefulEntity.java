package de.codazz.houseofcars.domain;

import de.codazz.houseofcars.Garage;
import de.codazz.houseofcars.statemachine.StateMachine;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** @author rstumm2s */
@MappedSuperclass
public abstract class StatefulEntity<Class extends StatefulEntity<Class, Lifecycle, State, Data, Event, Transition>, Lifecycle extends StateMachine<State, Data, Event>, State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data, Event, Transition extends StatefulEntityTransition<Transition, Class, Event, State, Data>> extends Entity {
    @Transient
    private transient final java.lang.Class<Transition> transitionClass;

    @Transient
    private transient final String lastTransitionQuery;

    public StatefulEntity(final java.lang.Class<Transition> transitionClass, final String lastTransitionQuery) {
        this.transitionClass = Objects.requireNonNull(transitionClass);
        this.lastTransitionQuery = Objects.requireNonNull(lastTransitionQuery);
    }

    @Transient
    private transient volatile Lifecycle lifecycle;

    /** do not cache! may be a new instance */
    public Lifecycle state() {
        return state(lastTransition().orElse(null));
    }

    /** restore to specified state
     * @param init {@code null} for root state */
    private Lifecycle state(final Transition init) {
        if (init == null) {
            lifecycle = initLifecycle();
        } else if (lifecycle == null || lifecycle.state() != init.state()) {
            lifecycle = restoreLifecycle(init);
        }
        return lifecycle;
    }

    protected abstract Lifecycle initLifecycle();
    protected abstract Lifecycle restoreLifecycle(final Transition init);

    public Optional<Transition> lastTransition() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(lastTransitionQuery, transitionClass)
            .setMaxResults(1)
            .setParameter("entity", this)
            .getResultStream().findFirst());
    }
}

/** @author rstumm2s */
@MappedSuperclass
abstract class StatefulEntityTransition<Class extends StatefulEntityTransition<Class, Entity, Event, State, Data>, Entity extends StatefulEntity, Event, State extends de.codazz.houseofcars.statemachine.State<Data, Event>, Data> extends Transition<Event, State, Data> {
    @ManyToOne(optional = false)
    private Entity entity;

    @Transient
    private transient final String nextQuery, previousQuery;
    @Transient
    private transient final java.lang.Class<Class> clazz;

    /** @deprecated only for JPA */
    @Deprecated
    protected StatefulEntityTransition(final java.lang.Class<Class> clazz, final String nextQuery, final String previousQuery) {
        this.clazz = clazz;
        this.nextQuery = nextQuery;
        this.previousQuery = previousQuery;
    }

    /** @param clazz the concrete class of {@code this} */
    protected StatefulEntityTransition(
        final java.lang.Class<Class> clazz, final String nextQuery, final String previousQuery,
        final Entity entity, final Data data
    ) {
        super(data);
        this.entity = Objects.requireNonNull(entity);

        this.clazz = Objects.requireNonNull(clazz);
        this.nextQuery = Objects.requireNonNull(nextQuery);
        this.previousQuery = Objects.requireNonNull(previousQuery);
    }

    public Entity entity() {
        return entity;
    }

    /** @return the associated vehicle's
     *     next transition, if any */
    public Optional<Class> next() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(nextQuery, clazz)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("entity", entity)
            .getResultStream().findFirst());
    }

    /** @return the associated vehicle's
     *     previous transition, if any */
    public Optional<Class> previous() {
        return Garage.instance().persistence.execute(em -> em
            .createNamedQuery(previousQuery, clazz)
            .setMaxResults(1)
            .setParameter("time", time())
            .setParameter("entity", entity)
            .getResultStream().findFirst());
    }

    public Duration duration() {
        return Duration.between(
            time(),
            next().map(Transition::time)
                .orElseGet(ZonedDateTime::now)
        );
    }

    private transient volatile de.codazz.houseofcars.template.Duration durationTemplate;

    public de.codazz.houseofcars.template.Duration durationTemplate() {
        if (durationTemplate == null && next().isPresent()) {
            durationTemplate = new de.codazz.houseofcars.template.Duration(duration());
        }
        return durationTemplate != null
            ? durationTemplate
            : new de.codazz.houseofcars.template.Duration(duration());
    }
}
