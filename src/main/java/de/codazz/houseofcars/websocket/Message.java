package de.codazz.houseofcars.websocket;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.OutputType;

/** @author rstumm2s */
public class Message {
    public static final ThreadLocal<Json> json = ThreadLocal.withInitial(() -> new Json(OutputType.json) {{
        setTypeName(null);
    }});

    public String toJson() {
        return json.get().toJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
