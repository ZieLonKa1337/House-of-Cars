package de.codazz.houseofcars.websocket;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.OutputType;

/** @author rstumm2s */
public class Message {
    protected static final ThreadLocal<Json> json = ThreadLocal.withInitial(() -> new Json(OutputType.json) {{
        setTypeName(null);
    }});

    public final String type;

    public Message(final String type) {
        this.type = type;
    }

    public String toJson() {
        return json.get().toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
