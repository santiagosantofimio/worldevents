package dev.santiagosantofimio.worldevents.i18n;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side translation. Vanilla clients do not have our language files, so we pick the text by the
 * player's client language and send it as a literal component.
 */
public final class Lang {
	/** The mod's languages. Mod loaders disagree on how to list a mod's files, so a new language goes here too. */
	public static final List<String> LANGUAGES = List.of("en_us", "es_es");

	private static Translations translations = new Translations(Map.of());

	private Lang() {
	}

	/**
	 * Loads the server-side texts ({@code worldevents/lang}) and the client-side ones ({@code assets/worldevents/lang},
	 * item and mob names also used by the resource pack), so the server can produce fallback names in any language.
	 */
	public static void load() {
		Map<String, Map<String, String>> tables = new HashMap<>();
		for (String language : LANGUAGES) {
			for (String folder : new String[] {WorldEventsMod.MOD_ID + "/lang", "assets/" + WorldEventsMod.MOD_ID + "/lang"}) {
				String file = folder + "/" + language + ".json";
				try (InputStream stream = Lang.class.getClassLoader().getResourceAsStream(file)) {
					if (stream == null) {
						WorldEventsMod.LOGGER.error("Language file {} not found; players will see raw keys", file);
						continue;
					}
					tables.computeIfAbsent(language, key -> new HashMap<>()).putAll(readTable(stream));
				} catch (IOException e) {
					WorldEventsMod.LOGGER.error("Could not read language file {}", file, e);
				}
			}
		}
		translations = new Translations(tables);
	}

	private static Map<String, String> readTable(InputStream stream) throws IOException {
		Map<String, String> table = new HashMap<>();
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				table.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		return table;
	}

	public static String language(ServerPlayer player) {
		return resolveLanguage(player.clientInformation().language());
	}

	/** The language we have texts for that is closest to a game language code (es_mx gets es_es…). */
	public static String resolveLanguage(String code) {
		return translations.resolveLanguage(code);
	}

	public static String language(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null ? language(player) : Translations.DEFAULT_LANGUAGE;
	}

	/** Language of the player nearest to {@code pos} within {@code maxDistance}, or the default. */
	public static String languageNear(ServerLevel level, Vec3 pos, double maxDistance) {
		ServerPlayer nearest = null;
		double best = maxDistance * maxDistance;
		for (ServerPlayer player : level.players()) {
			double distance = player.distanceToSqr(pos);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest != null ? language(nearest) : Translations.DEFAULT_LANGUAGE;
	}

	/** Language of {@code player} if it is a server player, otherwise the default. */
	public static String languageOf(Player player) {
		return player instanceof ServerPlayer serverPlayer ? language(serverPlayer) : Translations.DEFAULT_LANGUAGE;
	}

	public static String text(String language, String key, Object... args) {
		return translations.format(language, key, args);
	}

	public static MutableComponent component(String language, String key, Object... args) {
		return Component.literal(text(language, key, args));
	}

	public static MutableComponent component(ServerPlayer player, String key, Object... args) {
		return component(language(player), key, args);
	}

	public static MutableComponent component(CommandSourceStack source, String key, Object... args) {
		return component(language(source), key, args);
	}
}
