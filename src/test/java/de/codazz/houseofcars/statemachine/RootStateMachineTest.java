package de.codazz.houseofcars.statemachine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.Assert.*;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class RootStateMachineTest<Event, Response, Remote> extends AbstractStateMachineTest<Event,Response, Remote> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
                {TestRootStateMachine1.class, false, null}, {TestRootStateMachine1.class, true, null},
                {TestRootStateMachine2.class, false, null}, {TestRootStateMachine2.class, true, null},
                {TestRootStateMachine3.class, false, null}, {TestRootStateMachine3.class, true, null},
                {TestRootStateMachine4.class, false, new Object()}, {TestRootStateMachine4.class, true, new Object()}
        });
    }

    private final Object remote;

    public RootStateMachineTest(final Class root, final boolean lazy, final Object remote) {
        super(root, lazy);
        this.remote = remote;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected StateMachine instantiate(final Class root) {
        try {
            if (remote != null) {
                return (StateMachine) root.getConstructor(boolean.class, remote.getClass()).newInstance(lazy, remote);
            }
            return (StateMachine) root.getConstructor(boolean.class).newInstance(lazy);
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException("likely bug in test code", e);
        }
    }

    @Test
    public void meta() {
        if (lazy) return;

        final Object root = machine().root();
        final State meta = machine().meta();
        if (root instanceof TestRootStateMachine1) {
            assertTrue(meta.root()); // inherited
            assertFalse(meta.end());
        } else if (root instanceof TestRootStateMachine2) {
            assertFalse(meta.root());
            assertTrue(meta.end());
        } else if (root instanceof TestRootStateMachine3) {
            assertTrue(meta.root());
            assertTrue(meta.end());
        }
    }

    @Test
    public void remote() {
        if (lazy) return;

        final Object root = machine().root();
        if (root instanceof TestRootStateMachine4) {
            assertSame(remote, machine().remote());
        } else {
            assertNull(machine().remote());
        }
    }

    public static abstract class TestRootStateMachine<Event, Response, Remote> extends RootStateMachine<Event, Response, Remote> {
        public TestRootStateMachine() throws StateMachineException {}

        public TestRootStateMachine(final Remote remote) throws StateMachineException {
            super(remote);
        }

        public TestRootStateMachine(final Class<?> root, final boolean lazy) throws StateMachineException {
            super(root, lazy);
        }

        public TestRootStateMachine(final Class root, final boolean lazy, final Remote remote) throws StateMachineException {
            super(root, lazy, remote);
        }
    }

    /** {@link State} inherited */
    @OnEvent(value = Object.class, next = TestRootStateMachine1.End.class)
    public static class TestRootStateMachine1 extends TestRootStateMachine<Object, Object, Object> {
        public TestRootStateMachine1() throws StateMachineException {}

        public TestRootStateMachine1(final boolean lazy) throws StateMachineException {
            super(TestRootStateMachine1.class, lazy);
        }

        @State(end = true)
        public static class End {}
    }

    @State(end = true)
    public static class TestRootStateMachine2 extends TestRootStateMachine<Object, Object, Object> {
        public TestRootStateMachine2() throws StateMachineException {}

        public TestRootStateMachine2(final boolean lazy) throws StateMachineException {
            super(TestRootStateMachine2.class, lazy);
        }
    }

    @State(root = true, end = true)
    public static class TestRootStateMachine3 extends TestRootStateMachine<Object, Object, Object> {
        public TestRootStateMachine3() throws StateMachineException {}

        public TestRootStateMachine3(final boolean lazy) throws StateMachineException {
            super(TestRootStateMachine3.class, lazy);
        }
    }

    /** with remote */
    @State(end = true)
    public static class TestRootStateMachine4 extends TestRootStateMachine<Object, Object, Object> {
        public TestRootStateMachine4() throws StateMachineException {}

        public TestRootStateMachine4(final Object remote) throws StateMachineException {
            super(remote);
        }

        public TestRootStateMachine4(final boolean lazy, final Object remote) throws StateMachineException {
            super(TestRootStateMachine4.class, lazy, remote);
        }
    }
}
