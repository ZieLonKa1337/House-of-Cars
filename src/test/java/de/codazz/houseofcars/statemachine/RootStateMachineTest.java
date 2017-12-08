package de.codazz.houseofcars.statemachine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class RootStateMachineTest<Event, Response, Remote> extends AbstractStateMachineTest<Event,Response, Remote> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
                {TestRootStateMachine1.class, false}, {TestRootStateMachine1.class, true},
                {TestRootStateMachine2.class, false}, {TestRootStateMachine2.class, true},
                {TestRootStateMachine3.class, false}, {TestRootStateMachine3.class, true}
        });
    }

    public RootStateMachineTest(final Class root, final boolean lazy) {
        super(root, lazy, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected StateMachine instantiate(final Class root) {
        try {
            return (StateMachine) root.getConstructor(boolean.class).newInstance(lazy);
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException e) {
            throw new RuntimeException("likely bug in test code", e);
        }
    }

    @Test
    public void meta() {
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

    public static abstract class TestRootStateMachine<Event, Response, Remote> extends RootStateMachine<Event, Response, Remote> {
        public TestRootStateMachine() throws StateMachineException {}

        public TestRootStateMachine(final Class root, final boolean lazy) throws StateMachineException {
            super(root, lazy);
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
}
