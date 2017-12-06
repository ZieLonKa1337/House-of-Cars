package de.codazz.houseofcars.websocket;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.OutputType;

/** @author rstumm2s */
public class Message {
    protected static final Json json = new Json(OutputType.json) {{
        setTypeName(null);
    }};

    public final String type;

    public Message(final String type) {
        this.type = type;
    }

    public String toJson() {
        return json.toJson(this);
    }
}
