package de.codazz.houseofcars.statemachine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class StateMachineTest extends AbstractStateMachineTest<Object, Object, Object> {
    @Parameterized.Parameters
    public static Iterable data() {
        @State(end = true)
        class LocalSub1 {
            public LocalSub1() {}
        }

        @State(root = true)
        @OnEvent(value = Object.class, next = LocalSub1.class)
        class LocalRoot1 {
            public LocalRoot1() {}
        }

        @State
        @OnEvent(value = Object.class, next = LocalRoot2.Sub1.class)
        class LocalRoot2 {
            public LocalRoot2() {}

            @State(end = true)
            class Sub1 {
                public Sub1() {}
            }
        }

        return Arrays.asList(new Object[][]{
            {Root1.class, false}, {Root1.class, true},
            {Root2.class, false}, {Root2.class, true},
            {Root3.class, false}, {Root3.class, true},
            {Root4.class, false}, {Root4.class, true},
            {Root5.class, false}, {Root5.class, true},
            {Root6.class, false}, {Root6.class, true},
            {Root7.class, false}, {Root7.class, true},
            {Root8.class, false}, {Root8.class, true},
            {LocalRoot1.class, false}, {LocalRoot1.class, true},
            {LocalRoot2.class, false}, {LocalRoot2.class, true}
        });
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public StateMachineTest(final Class root, final boolean lazy) {
        super(root, lazy);
    }

    @Override
    protected StateMachine<Object, Object, Object> instantiate(final Class root) throws StateMachineException {
        if (!lazy && (
            root.equals(Root1.class) ||
            root.equals(Root4.class) ||
            root.equals(Root6.class) ||
            root.equals(Root8.class)
        )) {
            thrown.expect(StateMachineException.class);
        }
        return super.instantiate(root);
    }

    @Test
    public void start() throws StateMachineException {
        assertSame(lazy, machine().lazy());

        final Class<?> root = machine().rootClass();

        if (lazy) {
            assertFalse(machine().valid());

            if (root.equals(Root1.class) ||
                root.equals(Root6.class) ||
                root.equals(Root8.class)
            ) {
                thrown.expect(StateMachineException.class);
            }
        }
        machine().start();

        assertEquals(machine().state(), machine().root());
        assertSame(machine().meta().end(), machine().valid());
    }

    @Test
    public void root2_eventNotHandled() throws StateMachineException {
        if (notRunningFor(Root2.class)) return;

        thrown.expect(IllegalStateException.class);
        assertFalse(machine().onEvent(new Object()).isPresent());
    }

    @Test
    public void root3() throws StateMachineException {
        if (notRunningFor(Root3.class)) return;
        machine().start();

        assertFalse(machine().onEvent(new Object()).isPresent());
        assertEquals(machine().state(), machine().root());
    }

    @Test
    public void root5_1() throws StateMachineException {
        if (notRunningFor(Root5.class)) return;
        machine().start();

        assertFalse(machine().onEvent(new Object()).isPresent());
        assertEquals(Sub1.class, machine().state().getClass());
        assertFalse(machine().valid());

        assertFalse(machine().onEvent(new LinkedList<>()).isPresent());
        assertEquals(Sub2.class, machine().state().getClass());
        assertTrue(machine().valid());
    }

    @Test
    public void root5_2() throws StateMachineException {
        if (notRunningFor(Root5.class)) return;
        machine().start();

        assertFalse(machine().onEvent("event").isPresent());
        assertEquals(Sub2.class, machine().state().getClass());
        assertTrue(machine().valid());

        assertEquals("event", machine().onEvent("event").get());
        assertEquals(Sub2.class, machine().state().getClass());
    }

    @Test
    public void root5_3() throws StateMachineException {
        if (notRunningFor(Root5.class)) return;
        machine().start();

        thrown.expect(IllegalArgumentException.class);
        machine().onEvent(new CharSequence() {
            @Override
            public int length() {
                return 0;
            }

            @Override
            public char charAt(final int i) {
                return 0;
            }

            @Override
            public CharSequence subSequence(final int i, final int i1) {
                return null;
            }
        });
    }

    @Test
    public void root7_1() throws StateMachineException {
        if (notRunningFor(Root7.class)) return;
        machine().start();

        assertFalse(machine().onEvent(new Object()).isPresent());
        assertEquals(Root7.StaticMemberSub.class, machine().state().getClass());
        assertTrue(machine().valid());
    }

    @Test
    public void root7_2() throws StateMachineException {
        if (notRunningFor(Root7.class)) return;
        machine().start();

        assertFalse(machine().onEvent("event").isPresent());
        assertEquals(Root7.MemberSub.class, machine().state().getClass());
        assertTrue(machine().valid());
    }

    @Test
    public void localRoot1() throws StateMachineException {
        if (notRunningFor("LocalRoot1")) return;
        machine().start();

        assertFalse(machine().onEvent(new Object()).isPresent());
        assertEquals("LocalSub1", machine().state().getClass().getSimpleName());
        assertTrue(machine().valid());
    }

    @Test
    public void localRoot2() throws StateMachineException {
        if (notRunningFor("LocalRoot2")) return;
        machine().start();

        assertFalse(machine().onEvent(new Object()).isPresent());
        assertEquals("Sub1", machine().state().getClass().getSimpleName());
        assertTrue(machine().valid());
    }

    private boolean notRunningFor(final Class stateMachine) {
        return !machine().rootClass().equals(stateMachine);
    }

    private boolean notRunningFor(final String simpleName) {
        return !machine().rootClass().getSimpleName().equals(simpleName);
    }

    /** invalid: only one state which is not an accepted final state */
    @State(root = true)
    public static class Root1 {}

    @State(root = true, end = true)
    public static class Root2 {}

    /** states can refer to themselves */
    @State
    @OnEvent(value = Object.class, next = Root3.class)
    public static class Root3 {}

    /** invalid: refers to second root state */
    @State(root = true)
    @OnEvent(value = Object.class, next = Root2.class)
    public static class Root4 {}

    @State
    @OnEvent(value = Object.class, next = Sub1.class)
    @OnEvent(value = String.class, next = Sub2.class)
    @OnEvent(value = CharSequence.class, next = Sub2.class)
    public static class Root5 {}

    /** invalid: ambiguous event */
    @State
    @OnEvent(value = Object.class, next = Sub1.class)
    @OnEvent(value = Object.class, next = Sub2.class)
    public static class Root6 {}

    @State
    @OnEvent(value = Object.class,  next = Root7.StaticMemberSub.class)
    @OnEvent(value = String.class,  next = Root7.MemberSub.class)
    public static class Root7 {
        @State(end = true)
        public static class StaticMemberSub {}

        @State(end = true)
        public class MemberSub {}
    }

    /** invalid: enclosing instance is not a state */
    @State(end = true)
    public class Root8 {}

    @State
    public static class Sub1 {
        @OnEvent(value = ArrayList.class, next = Sub2.class)
        @OnEvent(value = LinkedList.class, next = Sub2.class)
        void event(final List event) {}
    }

    @State(end = true)
    public static class Sub2 {
        @OnEvent(value = String.class, next = Sub2.class)
        String echo(final String event) {
            return event;
        }
    }
}
