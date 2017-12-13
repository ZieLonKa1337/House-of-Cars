package de.codazz.houseofcars.statemachine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** This state machine implementation supports
 * both Moore and Mealy machines through the use
 * of {@link OnEnter} and {@link OnExit} for Moore
 * and {@link EventHandler event} responses for Mealy.
 * @author rstumm2s */
public class StateMachine<Event, Response, Remote> implements EventHandler<Event, Optional<Response>> {
    private final Map<Class<?>, StateHandler<?>> stateHandlers;
    /** the current state */
    private Class<?> state;

    private Class<?> root;

    private final Remote remote;

    private boolean lazy;

    public StateMachine(final Class<?> root) throws StateMachineException {
        this(root, false);
    }

    public StateMachine(final Class<?> root, final boolean lazy) throws StateMachineException {
        this(root, lazy, null, 16);
    }

    public StateMachine(final Class<?> root, final Remote remote) throws StateMachineException {
        this(root, false, remote, 16);
    }

    public StateMachine(final Class<?> root, final boolean lazy, final Remote remote) throws StateMachineException {
        this(root, lazy, remote, 16);
    }

    /** <strong>How to create a {@link StateMachine} class that is its own root {@link State}:</strong>
     * <p>
     * Add a no-arg and/or one-arg (remote) constructor for use as state that combines {@code lazy = true} and {@code capacity = -1}.
     * Since the state machine is lazy, nothing is done on instantiation. The special value {@code -1} means
     * that the collection holding states will not be created. This makes the instance unusable as state machine
     * but instantiation trivial and fast. Be sure <strong>not</strong> to call these "as-state" constructors yourself!
     * </p>
     * @param lazy If {@code true}, state initialization and validation are delayed until a state is first entered.
     *      Can be used to write a class that is both a {@link StateMachine} and a {@link State}.
     * @param remote Will be passed into the constructor of every state that takes a {@link State#remote() remote}. Make sure they match!
     * @param capacity The initial capacity of the collection in which states are held. Set to the known count of states to avoid rehashes.<br/>
     *      {@code -1} to use this instance as {@link State}, see above.
     * @see RootStateMachine */
    public StateMachine(final Class<?> root, final boolean lazy, final Remote remote, final int capacity) throws StateMachineException {
        this.root = Objects.requireNonNull(root);
        this.lazy = lazy;
        this.remote = remote;

        stateHandlers = capacity == -1 ? null : new HashMap<>(capacity, 1);

        if (!lazy) {
            instantiate();
        }
    }

    /** enter the root state */
    public void start() throws StateMachineException {
        state = root;
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

    /** Can be useful with a lazy state machine that has
     * not yet instantiated its {@link #root() root state}.
     * @return the class of the root state */
    public Class<?> rootClass() {
        return root;
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
                forceInstantiate(eventHandler.next);
                handler = stateHandlers.get(eventHandler.next);
            }
        }
        lazy = false;
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
            throw new IllegalStateException("state already instantiated");
        return forceInstantiate(state);
    }

    /** Replaces the state with a new instance.
     * @see #instantiate(Class) */
    public Object forceInstantiate(final Class<?> state) throws StateMachineException {
        if (!state.equals(root) && meta(state).root())
            throw new StateMachineException("state " + name(state) + " would be second root");

        final StateHandler<?> handler = new StateHandler<>(state);
        stateHandlers.put(state, handler);
        return handler.state;
    }

    /** @throws IllegalStateException if this state machine has not been {@link #start() started} yet */
    @Override
    public Optional<Response> onEvent(final Event event) throws StateMachineException {
        final StateHandler<?> handler = handler();
        if (handler == null) throw new IllegalStateException("state machine not started but received event: " + event);
        return Optional.ofNullable(handler.onEvent(event));
    }

    private StateHandler handler() {
        return state == null ? null : stateHandlers.get(state);
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

            final boolean isInstanceMember = state.isMemberClass() && !Modifier.isStatic(state.getModifiers());
            final Object enclosingInstance;
            if (isInstanceMember) {
                final StateHandler enclosingHandler = stateHandlers.get(state.getEnclosingClass());
                if (enclosingHandler != null) {
                    enclosingInstance = enclosingHandler.state;
                } else {
                    if (state.getEnclosingClass().equals(StateMachine.this.getClass())) {
                        enclosingInstance = StateMachine.this;
                    } else
                        throw new StateMachineException("state " + name(state) + " is a non-static member class but its enclosing state has not yet been instantiated in this state machine");
                }
            } else {
                enclosingInstance = null;
            }

            final Constructor<T> constructor;
            if (meta.remote() != void.class) {
                if (!remote.getClass().isAssignableFrom(meta.remote()))
                    throw new StateMachineException("remote for state " + name(state) + " must be " + meta.remote());
                try {
                    if (isInstanceMember) {
                        constructor = state.getConstructor(state.getEnclosingClass(), meta.remote());
                    } else{
                        constructor = state.getConstructor(meta.remote());
                    }
                } catch (final NoSuchMethodException | NullPointerException e) {
                    throw new StateMachineException("remote required for state " + name(state), e);
                }
            } else {
                try {
                    if (isInstanceMember) {
                        constructor = state.getConstructor(state.getEnclosingClass());
                    } else {
                        constructor = state.getConstructor();
                    }
                } catch (final NoSuchMethodException e) {
                    throw new StateMachineException("no-arg constructor required for state without remote: " + name(state), e);
                }
            }
            // (state instantiation after all checks)

            onEvent = new HashMap<>();
            class EventMapper {
                void map(final OnEvent meta, final Method action) throws StateMachineException {
                    if (onEvent.containsKey(meta.value()))
                        throw new StateMachineException("Event " + meta.value() + " already mapped on state " + name(state) + ". Only one handler per event is permitted.");

                    if (action != null) {
                        final Parameter[] parameters = action.getParameters();
                        if (parameters.length != 1 || !meta.value().isAssignableFrom(parameters[0].getType()))
                            throw new StateMachineException("action " + action.getName() + " on state " + name(state) + " does not take event " + meta.value());
                    }

                    onEvent.put(meta.value(), new EventHandler(action, meta.next()));
                }
            }
            final EventMapper eventMapper = new EventMapper();

            {
                Method onEnter = null, onExit = null;

                for (final Method method : Stream.concat(
                    Arrays.stream(state.getMethods()),
                    Arrays.stream(state.getDeclaredMethods()).peek(method -> {
                        if (!Modifier.isPublic(method.getModifiers()) &&
                            !method.isAccessible()
                        ) {
                            method.setAccessible(true);
                        }
                    })
                ).distinct().toArray(Method[]::new)) {
                    if (method.isAnnotationPresent(OnEnter.class)) {
                        if (onEnter != null) throw new StateMachineException("only one onEnter per State is permitted");
                        onEnter = method;
                        if (meta.root() && method.getParameterCount() > 0)
                            throw new StateMachineException("the initial state must not take parameters");
                    } else if (method.isAnnotationPresent(OnExit.class)) {
                        if (onExit != null) throw new StateMachineException("only one onExit per State is permitted");
                        onExit = method;
                    }

                    final OnEvent metaOnEvent = method.getAnnotation(OnEvent.class);
                    if (metaOnEvent != null) {
                        eventMapper.map(metaOnEvent, method);
                    }

                    final OnEvent.OnEvents metaOnEvents = method.getAnnotation(OnEvent.OnEvents.class);
                    if (metaOnEvents != null) {
                        for (final OnEvent it : metaOnEvents.value()) {
                            eventMapper.map(it, method);
                        }
                    }
                }

                {
                    final OnEvent metaOnEvent = state.getAnnotation(OnEvent.class);
                    if (metaOnEvent != null) {
                        eventMapper.map(metaOnEvent, null);
                    }

                    final OnEvent.OnEvents metaOnEvents = state.getAnnotation(OnEvent.OnEvents.class);
                    if (metaOnEvents != null) {
                        for (final OnEvent it : metaOnEvents.value()) {
                            eventMapper.map(it, null);
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
                    if (isInstanceMember) {
                        if (meta.remote() != void.class) {
                            instance = constructor.newInstance(enclosingInstance, remote);
                        } else {
                            instance = constructor.newInstance(enclosingInstance);
                        }
                    } else {
                        if (meta.remote() != void.class) {
                            instance = constructor.newInstance(remote);
                        } else {
                            instance = constructor.newInstance();
                        }
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
            if (handler == null) throw new IllegalArgumentException("event " + event + " is not handled");
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
                if (action == null) return null;
                try {
                    return (Response) action.invoke(state, event);
                } catch (final IllegalAccessException e) {
                    throw new StateMachineException("failed to call event action " + action.getName() + " of state " + name(StateMachine.this.state), e);
                } catch (final InvocationTargetException e) {
                    throw new RuntimeException("event action " + action.getName() + " of state " + name(StateMachine.this.state) + " threw exception", e);
                } catch (final ClassCastException e) {
                    throw new StateMachineException("event action " + action.getName() + " of state " + name(StateMachine.this.state) + " returned wrong response type", e);
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
