package dev.santiagosantofimio.worldevents.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.platform.Platform;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/worldevents.json. Missing keys are filled in from the defaults in code; the user's values are kept.
 * Durations are in seconds.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String EVENTS = "events";

	private static ModConfig current;

	private final JsonObject values;
	private final JsonObject defaults;

	private ModConfig(JsonObject values, JsonObject defaults) {
		this.values = values;
		this.defaults = defaults;
	}

	public static ModConfig get() {
		return current;
	}

	public static Path path() {
		return Platform.INSTANCE.configDir().resolve(WorldEventsMod.MOD_ID + ".json");
	}

	/**
	 * Loads the config, writing back any missing keys. If the file cannot be parsed it is left untouched and
	 * the previous config (or the defaults, on first load) stays in use.
	 *
	 * @return whether the file was read successfully
	 */
	public static boolean load() {
		JsonObject defaults = buildDefaults();
		Path path = path();
		JsonObject values = new JsonObject();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonElement parsed = new JsonParser().parse(reader);
				if (!parsed.isJsonObject()) {
					throw new JsonParseException("Root must be a JSON object");
				}
				values = parsed.getAsJsonObject();
			} catch (IOException | JsonParseException | IllegalStateException e) {
				WorldEventsMod.LOGGER.error("Could not read {}; it will not be overwritten", path, e);
				if (current == null) {
					current = new ModConfig(JsonMerge.copy(defaults).getAsJsonObject(), defaults);
				}
				return false;
			}
		}
		if (JsonMerge.addMissing(values, defaults) || !Files.exists(path)) {
			write(path, values);
		}
		current = new ModConfig(values, defaults);
		return true;
	}

	private static JsonObject buildDefaults() {
		JsonObject root = new JsonObject();
		root.addProperty("checkIntervalTicks", 200);
		root.addProperty("startChance", 0.05);
		root.addProperty("globalCooldown", 3600);
		root.addProperty("minPlayers", 1);
		root.addProperty("blockDamage", true);
		root.addProperty("warningSeconds", 60);
		root.addProperty("ranking", true);
		root.addProperty("sealsPerEvent", 1);
		root.addProperty("sealsForWinner", 2);
		root.addProperty("minScoreForSeal", 3);
		JsonObject events = new JsonObject();
		for (WorldEvent event : EventRegistry.all()) {
			events.add(event.id(), event.defaultSettings());
		}
		root.add(EVENTS, events);
		return root;
	}

	private static void write(Path path, JsonObject values) {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(values, writer);
			}
		} catch (IOException e) {
			WorldEventsMod.LOGGER.error("Could not write {}", path, e);
		}
	}

	private EventSettings root() {
		return new EventSettings(values, defaults);
	}

	/** How often (in ticks) each dimension checks whether a new event should start. */
	public int checkIntervalTicks() {
		return Math.max(20, root().getInt("checkIntervalTicks"));
	}

	/** Chance per check, once the global cooldown is over, that the next event gets drawn. */
	public double startChance() {
		return Math.max(0.0, Math.min(1.0, root().getDouble("startChance")));
	}

	/** Minimum time between the end of one event and drawing the next, per dimension. */
	public long globalCooldownTicks() {
		return Math.max(0L, (long) root().getInt("globalCooldown") * 20L);
	}

	/** Players needed in a dimension before an event can start there. */
	public int minPlayers() {
		return Math.max(1, root().getInt("minPlayers"));
	}

	/** Master switch for any event that changes blocks. */
	public boolean blockDamage() {
		return root().getBoolean("blockDamage");
	}

	/** How long before a drawn event starts players are warned. 0 starts it right away. */
	public int warningTicks() {
		return Math.max(0, root().getInt("warningSeconds") * 20);
	}

	/** Whether the best players are announced when an event ends, and seals handed out. */
	public boolean ranking() {
		return root().getBoolean("ranking");
	}

	/** Event seals for everyone who took part, when an event ends. */
	public int sealsPerEvent() {
		return Math.max(0, root().getInt("sealsPerEvent"));
	}

	/** Extra seals for the player who did the most. */
	public int sealsForWinner() {
		return Math.max(0, root().getInt("sealsForWinner"));
	}

	/** Points that count as taking part in an event. */
	public int minScoreForSeal() {
		return Math.max(1, root().getInt("minScoreForSeal"));
	}

	public EventSettings event(String id) {
		return new EventSettings(section(values, id), section(defaults, id));
	}

	private static JsonObject section(JsonObject root, String id) {
		JsonElement events = root.get(EVENTS);
		if (events == null || !events.isJsonObject()) {
			return null;
		}
		JsonElement section = events.getAsJsonObject().get(id);
		return section != null && section.isJsonObject() ? section.getAsJsonObject() : null;
	}
}
