package de.codazz.houseofcars.statemachine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** This state machine implementation supports
 * both Moore and Mealy machines through the use
 * of {@link OnEnter} and {@link OnExit} for Moore
 * and {@link EventHandler event} responses for Mealy.
 * @author rstumm2s */
public class StateMachine<Event, Response, Remote> implements EventHandler<Event, Optional<Response>> {
    private final Map<Class<?>, StateHandler<?>> stateHandlers = new HashMap<>();
    /** the current state */
    private Class<?> state;

    private Class<?> root;

    private final Remote remote;

    private boolean lazy;

    public StateMachine(final Class<?> root) throws StateMachineException {
        this(root, false);
    }

    public StateMachine(final Class<?> root, final boolean lazy) throws StateMachineException {
        this(root, lazy, null);
    }

    /** @param lazy If {@code true}, state initialization and validation are delayed until a state is first entered.
     * @param remote Will be passed into the constructor of every state that takes a {@link State#remote() remote}. Make sure they match! */
    public StateMachine(final Class<?> root, final boolean lazy, final Remote remote) throws StateMachineException {
        this.state = this.root = root;
        this.lazy = lazy;
        this.remote = remote;

        if (!lazy) {
            instantiate();
        }
    }

    /** enter the root state */
    public void start() throws StateMachineException {
        if (handler() == null) {
            // lazy instantiation
            instantiate(root);
        }
        handler().onEnter(null);
    }

    /** @return the current state */
    public Object state() {
        final StateHandler handler = handler();
        return handler == null ? null : handler.state;
    }

    /** @return the current state's {@link State} annotation */
    public State metaState() {
        final StateHandler handler = handler();
        return handler == null ? null : handler.meta;
    }

    /** @return the root state */
    public Object root() {
        final StateHandler rootHandler = stateHandlers.get(root);
        return rootHandler == null ? null : rootHandler.state;
    }

    /** @return the root state's {@link State} annotation */
    public State meta() {
        final StateHandler rootHandler = stateHandlers.get(root);
        return rootHandler == null ? null : rootHandler.meta;
    }

    /** @return this state machine's {@link State#remote() remote} */
    public Remote remote() {
        return remote;
    }

    /** shortcut for {@link #metaState()}.{@link State#end() end()}
     * @return whether the current state is an accepted final state */
    public boolean valid() {
        final StateHandler handler = handler();
        return handler != null && handler.meta.end();
    }

    /** A lazy state machine can be fully
     * {@link #instantiate() instantiated}
     * so it is no longer lazy.
     * @return whether this state machine is lazy */
    public boolean lazy() {
        return lazy;
    }

    /** Instantiates and validates all states of this state machine.
     * After this a state machine is no longer lazy.
     * @throws StateMachineException if instantiation or validation failed for some state */
    public void instantiate() throws StateMachineException {
        StateHandler<?> handler = stateHandlers.get(root);
        if (handler == null) {
            handler = new StateHandler<>(root);
            stateHandlers.put(root, handler);
        }
        while (handler != null) {
            final Collection<? extends StateHandler<?>.EventHandler> eventHandlers = handler.onEvent.values();
            handler = null;
            for (final StateHandler<?>.EventHandler eventHandler : eventHandlers) {
                if (stateHandlers.containsKey(eventHandler.next)) continue;

                final StateHandler<?> next = new StateHandler<>(eventHandler.next);
                stateHandlers.put(next.state.getClass(), next);

                handler = next;
            }
        }
        lazy = false;
    }

    @Override
    public Optional<Response> onEvent(final Event event) throws StateMachineException {
        assert stateHandlers.containsKey(state);
        return Optional.ofNullable(stateHandlers.get(state).onEvent(event));
    }

    private StateHandler handler() {
        return stateHandlers.get(state);
    }

    /** <p>
     * Instantiate only the specified state.
     * This can be useful to preload slowly instantiating states.
     * </p><p>
     * A lazy state machine will still be considered lazy after the
     * call exits, even if this was the last state to be instantiated.
     * Use {@link #instantiate()} instead if you know
     * that this is the last uninstantiated state.
     * </p>
     * @param state the class of the state to instantiate
     * @return the newly instantiated state
     * @throws IllegalStateException if the state has already been instantiated */
    public Object instantiate(final Class<?> state) throws StateMachineException, IllegalStateException {
        if (stateHandlers.containsKey(state))
            throw new IllegalArgumentException("state already instantiated");

        final StateHandler<?> handler = new StateHandler<>(state);
        stateHandlers.put(state, handler);
        return handler.state;
    }

    private class StateHandler<T> implements EventHandler<Event, Response> {
        public final State meta;

        /** the {@link State} instance */
        public final T state;

        final Map<Class<?>, EventHandler> onEvent;
        private final Method onEnter, onExit;

        /** @param state the initial state, annotated with {@link State} */
        private StateHandler(final Class<T> state) throws StateMachineException {
            meta = meta(state);

            final Constructor<T> constructor;
            if (meta.remote() != void.class) {
                if (!remote.getClass().isAssignableFrom(meta.remote()))
                    throw new StateMachineException("remote for state " + name(state) + " must be " + meta.remote());
                try {
                    constructor = state.getConstructor(meta.remote());
                } catch (final NoSuchMethodException | NullPointerException e) {
                    throw new StateMachineException("remote required for state " + name(state), e);
                }
            } else {
                try {
                    constructor = state.getConstructor();
                } catch (final NoSuchMethodException e) {
                    throw new StateMachineException("no-arg constructor required for state without remote: " + name(state), e);
                }
            }
            // (state instantiation after all checks)

            {
                Method onEnter = null, onExit = null;
                onEvent = new HashMap<>();
                for (final Method method : state.getMethods()) {
                    if (method.isAnnotationPresent(OnEnter.class)) {
                        if (onEnter != null) throw new StateMachineException("only one onEnter per State is permitted");
                        onEnter = method;
                        if (meta.root() && method.getParameterCount() > 0)
                            throw new StateMachineException("the initial state must not take parameters");
                    } else if (method.isAnnotationPresent(OnExit.class)) {
                        if (onExit != null) throw new StateMachineException("only one onExit per State is permitted");
                        onExit = method;
                    } else if (method.isAnnotationPresent(OnEvent.class)) {
                        final OnEvent annotation = method.getAnnotation(OnEvent.class);
                        if (onEvent.containsKey(annotation.value()))
                            throw new StateMachineException("only one handler per event is permitted");
                        onEvent.put(annotation.value(), new EventHandler(method, annotation.next()));
                    } else if (method.isAnnotationPresent(OnEvents.class)) {
                        final OnEvents annotation = method.getAnnotation(OnEvents.class);
                        for (final OnEvent it : annotation.value()) {
                            if (onEvent.containsKey(it.value()))
                                throw new StateMachineException("only one handler per event is permitted");
                            onEvent.put(it.value(), new EventHandler(method, it.next()));
                        }
                    }
                }

                if (onEvent.isEmpty() && !meta.end())
                    throw new StateMachineException("state " + name(state) + " cannot exit but is not an accepted final state");

                this.onEnter = onEnter;
                this.onExit = onExit;
            }

            {
                T instance;

                try {
                    if (meta.remote() != void.class) {
                        instance = constructor.newInstance(remote);
                    } else {
                        instance = constructor.newInstance();
                    }
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new StateMachineException("failed to instantiate state " + name(state), e);
                }

                this.state = instance;
            }
        }

        /** @throws StateMachineException if {@link #lazy() lazy}, when lazy state instantiation failed */
        @Override
        public Response onEvent(final Event event) throws StateMachineException {
            final EventHandler handler = onEvent.get(event.getClass());
            if (handler == null) throw new StateMachineException(new IllegalArgumentException("event " + event + " is not handled"));
            final Response response = handler.onEvent(event);

            final StateHandler<?> next; {
                StateHandler<?> it = stateHandlers.get(handler.next);
                if (it == null) {
                    // lazy instantiation
                    try {
                        it = new StateHandler<>(handler.next);
                        stateHandlers.put(handler.next, it);
                    } catch (final StateMachineException e) {
                        throw new StateMachineException("lazy instantiation failed for state " + name(handler.next), e);
                    }
                }
                next = it;
            }

            next.onEnter(onExit());
            StateMachine.this.state = handler.next;
            return response;
        }

        private void onEnter(final Object result) throws StateMachineException {
            if (onEnter == null) return;
            try {
                if (result != null) {
                    onEnter.invoke(state, result);
                } else {
                    onEnter.invoke(state);
                }
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new StateMachineException(e);
            }
        }

        private Object onExit() throws StateMachineException {
            if (onExit == null) return null;
            final Object result;
            try {
                result = onExit.invoke(state);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new StateMachineException(e);
            }
            return result;
        }

        private class EventHandler implements de.codazz.houseofcars.statemachine.EventHandler<Event, Response> {
            /** the class of the next state */
            public final Class<?> next;

            private final Method action;

            EventHandler(final Method action, final Class<?> next) {
                this.action = action;
                this.next = next;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Response onEvent(final Event event) throws StateMachineException {
                try {
                    return (Response) action.invoke(state, event);
                } catch (final IllegalAccessException e) {
                    throw new StateMachineException("failed to call event action in state " + name(StateMachine.this.state), e);
                } catch (final InvocationTargetException e) {
                    throw new RuntimeException("event action threw exception", e);
                } catch (final ClassCastException e) {
                    throw new StateMachineException("event action returned wrong response type", e);
                }
            }
        }
    }

    private static State meta(final Class<?> state) throws StateMachineException {
        final State meta = state.getAnnotation(State.class);
        if (meta == null)
            throw new StateMachineException(new NullPointerException(state.getName() + " is not annotated with @" + State.class.getSimpleName()));
        return meta;
    }

    private static String name(final Class<?> state) throws StateMachineException {
        final String name = meta(state).name();
        if (name.isEmpty()) return state.getSimpleName();
        return name;
    }
}
