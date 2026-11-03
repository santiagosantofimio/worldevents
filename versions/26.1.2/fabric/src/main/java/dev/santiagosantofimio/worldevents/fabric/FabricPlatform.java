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
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {
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
