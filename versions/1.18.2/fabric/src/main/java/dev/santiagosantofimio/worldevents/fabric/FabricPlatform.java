package dev.santiagosantofimio.worldevents.fabric;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.platform.Platform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatform implements Platform {
	/** Each player's game language, which 1.18 servers do not keep: {@code ServerPlayerMixin} records it. */
	private static final Map<UUID, String> LANGUAGES = new ConcurrentHashMap<>();

	public static void rememberLanguage(ServerPlayer player, String language) {
		LANGUAGES.put(player.getUUID(), language);
	}

	public static void forget(ServerPlayer player) {
		LANGUAGES.remove(player.getUUID());
	}

	@Override
	public String language(ServerPlayer player) {
		return LANGUAGES.getOrDefault(player.getUUID(), "en_us");
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public Map<String, String> readModFiles(String folder) throws IOException {
		Path dir = FabricLoader.getInstance().getModContainer(WorldEventsMod.MOD_ID)
				.flatMap(container -> container.findPath(folder))
				.orElse(null);
		Map<String, String> files = new HashMap<>();
		if (dir == null) {
			return files;
		}
		List<Path> paths;
		try (Stream<Path> list = Files.list(dir)) {
			paths = list.filter(Files::isRegularFile).toList();
		}
		for (Path path : paths) {
			files.put(path.getFileName().toString(), Files.readString(path, StandardCharsets.UTF_8));
		}
		return files;
	}
}
