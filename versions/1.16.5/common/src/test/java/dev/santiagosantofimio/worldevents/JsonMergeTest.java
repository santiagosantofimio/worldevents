package dev.santiagosantofimio.worldevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldevents.config.JsonMerge;
import org.junit.jupiter.api.Test;

class JsonMergeTest {
	private static JsonObject json(String text) {
		return new JsonParser().parse(text).getAsJsonObject();
	}

	@Test
	void addsMissingKeysWithoutTouchingUserValues() {
		JsonObject user = json("{\"a\": 5, \"events\": {\"x\": {\"weight\": 1}}}");
		JsonObject defaults = json("{\"a\": 1, \"b\": 2, \"events\": {\"x\": {\"weight\": 10, \"enabled\": true}, \"y\": {\"weight\": 3}}}");

		assertTrue(JsonMerge.addMissing(user, defaults));

		assertEquals(5, user.get("a").getAsInt());
		assertEquals(2, user.get("b").getAsInt());
		JsonObject x = user.getAsJsonObject("events").getAsJsonObject("x");
		assertEquals(1, x.get("weight").getAsInt());
		assertTrue(x.get("enabled").getAsBoolean());
		assertEquals(3, user.getAsJsonObject("events").getAsJsonObject("y").get("weight").getAsInt());
	}

	@Test
	void keepsUserValueWhenTypeDiffers() {
		JsonObject user = json("{\"events\": \"oops\"}");
		JsonObject defaults = json("{\"events\": {\"x\": {}}}");

		assertFalse(JsonMerge.addMissing(user, defaults));
		assertEquals("oops", user.get("events").getAsString());
	}

	@Test
	void reportsNoChangeWhenComplete() {
		JsonObject user = json("{\"a\": 1, \"extra\": true}");
		assertFalse(JsonMerge.addMissing(user, json("{\"a\": 9}")));
		assertTrue(user.get("extra").getAsBoolean());
	}

	@Test
	void doesNotShareDefaultInstances() {
		JsonObject defaults = json("{\"nested\": {\"v\": 1}}");
		JsonObject user = new JsonObject();
		JsonMerge.addMissing(user, defaults);
		user.getAsJsonObject("nested").addProperty("v", 2);
		assertEquals(1, defaults.getAsJsonObject("nested").get("v").getAsInt());
	}
}
