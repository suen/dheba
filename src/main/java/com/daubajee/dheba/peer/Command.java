package com.daubajee.dheba.peer;

import java.util.Arrays;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class Command {

	private final String type;
	
	private final JsonObject content;

	public static final String ASK_PEER_LIST = "ASK_PEER_LIST";
	
	public static final List<String> TYPES = Arrays.asList(ASK_PEER_LIST);
	
	public Command(String type, JsonObject content) {
		this.type = type;
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public JsonObject getContent() {
		return content;
	}

    public boolean isValid() {
        return type != null && !type.isEmpty() && TYPES.contains(type);
    }

    public JsonObject toJson() {
        return new JsonObject()
                    .put("type", type)
                    .put("content", content);
    }

    public static Command from(JsonObject json) {
        String jtype = json.getString("type");
        JsonObject jcontent = json.getJsonObject("content", new JsonObject());
        return new Command(jtype, jcontent);
    }
	
}
