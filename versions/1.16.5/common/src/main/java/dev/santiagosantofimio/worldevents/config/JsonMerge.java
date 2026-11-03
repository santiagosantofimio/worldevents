package dev.santiagosantofimio.worldevents.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public final class JsonMerge {
	private JsonMerge() {
	}

	/**
	 * Copies every key from {@code defaults} that is missing in {@code target}, recursing into nested objects.
	 * Values the user already set are never overwritten, even if their type differs from the default.
	 *
	 * @return whether {@code target} changed
	 */
	/** A deep copy: 1.16's Gson keeps deepCopy to itself. */
	public static JsonElement copy(JsonElement element) {
		return new JsonParser().parse(element.toString());
	}

	public static boolean addMissing(JsonObject target, JsonObject defaults) {
		boolean changed = false;
		for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
			String key = entry.getKey();
			JsonElement defaultValue = entry.getValue();
			JsonElement current = target.get(key);
			if (current == null) {
				target.add(key, copy(defaultValue));
				changed = true;
			} else if (current.isJsonObject() && defaultValue.isJsonObject()) {
				changed |= addMissing(current.getAsJsonObject(), defaultValue.getAsJsonObject());
			}
		}
		return changed;
	}
}
