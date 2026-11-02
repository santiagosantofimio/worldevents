package dev.santiagosantofimio.worldevents.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Typed view over one event's config section. Invalid or missing values fall back to the defaults from code.
 * Durations in the JSON are in seconds; the {@code *Ticks} accessors convert them.
 */
public final class EventSettings {
	public static final String ENABLED = "enabled";
	public static final String WEIGHT = "weight";
	public static final String COOLDOWN = "cooldown";
	public static final String DURATION = "duration";

	private final JsonObject values;
	private final JsonObject defaults;

	public EventSettings(JsonObject values, JsonObject defaults) {
		this.values = values;
		this.defaults = defaults;
	}

	/** Starts a defaults object with the keys every event has. Durations are in seconds. */
	public static JsonObject defaults(boolean enabled, int weight, int cooldownSeconds, int durationSeconds) {
		JsonObject json = new JsonObject();
		json.addProperty(ENABLED, enabled);
		json.addProperty(WEIGHT, weight);
		json.addProperty(COOLDOWN, cooldownSeconds);
		json.addProperty(DURATION, durationSeconds);
		return json;
	}

	public boolean enabled() {
		return getBoolean(ENABLED);
	}

	public int weight() {
		return Math.max(0, getInt(WEIGHT));
	}

	public long cooldownTicks() {
		return Math.max(0L, (long) getInt(COOLDOWN) * 20L);
	}

	public int durationTicks() {
		return Math.max(20, getInt(DURATION) * 20);
	}

	public boolean getBoolean(String key) {
		JsonPrimitive value = primitive(values, key);
		if (value != null && value.isBoolean()) {
			return value.getAsBoolean();
		}
		JsonPrimitive fallback = primitive(defaults, key);
		return fallback != null && fallback.getAsBoolean();
	}

	public int getInt(String key) {
		JsonPrimitive value = primitive(values, key);
		if (value != null && value.isNumber()) {
			return value.getAsInt();
		}
		JsonPrimitive fallback = primitive(defaults, key);
		return fallback != null ? fallback.getAsInt() : 0;
	}

	public double getDouble(String key) {
		JsonPrimitive value = primitive(values, key);
		if (value != null && value.isNumber()) {
			return value.getAsDouble();
		}
		JsonPrimitive fallback = primitive(defaults, key);
		return fallback != null ? fallback.getAsDouble() : 0.0;
	}

	private static JsonPrimitive primitive(JsonObject object, String key) {
		if (object == null) {
			return null;
		}
		JsonElement element = object.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
	}
}
