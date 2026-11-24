package dev.santiagosantofimio.worldevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import org.junit.jupiter.api.Test;

class EventSettingsTest {
	@Test
	void fallsBackToDefaultsOnBadValues() {
		JsonObject defaults = EventSettings.defaults(true, 10, 60, 30);
		defaults.addProperty("multiplier", 2.0);
		JsonObject user = JsonParser.parseString("{\"enabled\": \"yes\", \"weight\": 4, \"multiplier\": \"big\"}").getAsJsonObject();

		EventSettings settings = new EventSettings(user, defaults);

		assertEquals(true, settings.enabled());
		assertEquals(4, settings.weight());
		assertEquals(2.0, settings.getDouble("multiplier"));
		assertEquals(1200L, settings.cooldownTicks());
		assertEquals(600, settings.durationTicks());
	}

	@Test
	void clampsNegativeValues() {
		JsonObject defaults = EventSettings.defaults(false, 10, 60, 30);
		JsonObject user = JsonParser.parseString("{\"weight\": -3, \"cooldown\": -1, \"duration\": 0}").getAsJsonObject();

		EventSettings settings = new EventSettings(user, defaults);

		assertFalse(settings.enabled());
		assertEquals(0, settings.weight());
		assertEquals(0L, settings.cooldownTicks());
		assertEquals(20, settings.durationTicks());
	}
}
