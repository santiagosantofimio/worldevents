package dev.santiagosantofimio.worldevents.i18n;

import dev.santiagosantofimio.worldevents.platform.Platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
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
	private static Translations translations = new Translations(Map.of());

	private Lang() {
	}

	/**
	 * Loads the server-side texts ({@code worldevents/lang}) and the client-side ones ({@code assets/worldevents/lang},
	 * item and mob names also used by the resource pack), so the server can produce fallback names in any language.
	 */
	public static void load() {
		Map<String, Map<String, String>> tables = new HashMap<>();
		for (String folder : new String[] {WorldEventsMod.MOD_ID + "/lang", "assets/" + WorldEventsMod.MOD_ID + "/lang"}) {
			Path dir = Platform.INSTANCE.findModPath(folder).orElse(null);
			if (dir == null) {
				WorldEventsMod.LOGGER.error("Language folder {} not found; players will see raw keys", folder);
				continue;
			}
			try (Stream<Path> files = Files.list(dir)) {
				for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
					String name = file.getFileName().toString();
					tables.computeIfAbsent(name.substring(0, name.length() - ".json".length()), language -> new HashMap<>())
							.putAll(readTable(file));
				}
			} catch (IOException e) {
				WorldEventsMod.LOGGER.error("Could not read language files in {}", folder, e);
			}
		}
		translations = new Translations(tables);
	}

	private static Map<String, String> readTable(Path file) throws IOException {
		Map<String, String> table = new HashMap<>();
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
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
