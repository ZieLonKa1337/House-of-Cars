package de.codazz.houseofcars.statemachine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/** @author rstumm2s */
@RunWith(Parameterized.class)
public class StateMachineTest extends AbstractStateMachineTest<Object, Object, Object> {
    @Parameterized.Parameters
    public static Iterable data() {
        return Arrays.asList(new Object[][]{
            {Root1.class, false}, {Root1.class, true},
            {Root2.class, false}, {Root2.class, true},
            {Root3.class, false}, {Root3.class, true},
            {Root4.class, false}, {Root4.class, true},
            {Root5.class, false}, {Root5.class, true},
            {Root6.class, false}, {Root6.class, true}
        });
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public StateMachineTest(final Class root, final boolean lazy) {
        super(root, lazy, null);
    }

    @Override
    protected StateMachine<Object, Object, Object> instantiate(final Class root) throws StateMachineException {
        if (!lazy && (
            root.equals(Root1.class) ||
            root.equals(Root4.class) ||
            root.equals(Root6.class)
        )) {
            thrown.expect(StateMachineException.class);
        }
        return super.instantiate(root);
    }

    @Test
    public void start() throws StateMachineException {
        if (lazy && (
            machine().rootClass().equals(Root1.class) ||
            machine().rootClass().equals(Root6.class)
        )) {
            thrown.expect(StateMachineException.class);
        }
        machine().start();
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
    @OnEvents({
        @OnEvent(value = Object.class, next = Sub1.class),
        @OnEvent(value = String.class, next = Sub2.class),
        @OnEvent(value = CharSequence.class, next = Sub2.class)
    })
    public static class Root5 {}

    /** invalid: ambiguous event */
    @State
    @OnEvents({
        @OnEvent(value = Object.class, next = Sub1.class),
        @OnEvent(value = Object.class, next = Sub2.class)
    })
    public static class Root6 {}

    @State
    public static class Sub1 {
        @OnEvents({
            @OnEvent(value = Integer.class, next = Sub2.class),
            @OnEvent(value = String.class, next = Sub2.class)
        })
        public void event(final String event) {}
    }

    @State(end = true)
    public static class Sub2 {
        @OnEvent(value = String.class, next = Sub2.class)
        public String echo(final String event) {
            return event;
        }
    }
}
