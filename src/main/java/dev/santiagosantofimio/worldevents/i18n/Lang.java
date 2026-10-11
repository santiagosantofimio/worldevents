package dev.santiagosantofimio.worldevents.i18n;

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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side translation. Vanilla clients do not have our language files, so we pick the text by the
 * player's client language and send it as a literal component.
 */
public final class Lang {
	private static Translations translations = new Translations(Map.of());

	private Lang() {
	}

	public static void load() {
		Map<String, Map<String, String>> tables = new HashMap<>();
		Path dir = FabricLoader.getInstance().getModContainer(WorldEventsMod.MOD_ID)
				.flatMap(container -> container.findPath(WorldEventsMod.MOD_ID + "/lang"))
				.orElse(null);
		if (dir == null) {
			WorldEventsMod.LOGGER.error("Language folder not found; players will see raw keys");
			return;
		}
		try (Stream<Path> files = Files.list(dir)) {
			for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
				String name = file.getFileName().toString();
				tables.put(name.substring(0, name.length() - ".json".length()), readTable(file));
			}
		} catch (IOException e) {
			WorldEventsMod.LOGGER.error("Could not read language files", e);
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
		return translations.resolveLanguage(player.clientInformation().language());
	}

	public static String language(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null ? language(player) : Translations.DEFAULT_LANGUAGE;
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
