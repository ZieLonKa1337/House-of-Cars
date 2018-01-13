package de.codazz.houseofcars.domain;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Objects;

/** @author rstumm2s */
@Deprecated
public class EnumStateTransition<Event, State extends Enum<State> & de.codazz.houseofcars.statemachine.State<Data, Event>, Data> extends Transition<Event, State, Data> {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    public EnumStateTransition(final Data data, final State state) {
        super(data);
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public State state() {
        return state;
    }
}
