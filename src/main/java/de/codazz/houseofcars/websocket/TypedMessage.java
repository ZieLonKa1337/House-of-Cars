package de.codazz.houseofcars.websocket;

/** @author rstumm2s */
public class TypedMessage extends Message {
    public final String type;

    public TypedMessage(final String type) {
        this.type = type;
    }
}
