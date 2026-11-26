package dev.santiagosantofimio.worldomens.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

public final class JsonMerge {
	private JsonMerge() {
	}

	public static boolean addMissing(JsonObject target, JsonObject defaults) {
		boolean changed = false;
		for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
			String key = entry.getKey();
			JsonElement defaultValue = entry.getValue();
			JsonElement current = target.get(key);
			if (current == null) {
				target.add(key, defaultValue.deepCopy());
				changed = true;
			} else if (current.isJsonObject() && defaultValue.isJsonObject()) {
				changed |= addMissing(current.getAsJsonObject(), defaultValue.getAsJsonObject());
			}
		}
		return changed;
	}
}
