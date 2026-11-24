package dev.santiagosantofimio.worldevents.neoforge;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.platform.Platform;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatform implements Platform {
	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public Map<String, String> readModFiles(String folder) throws IOException {
		Map<String, String> files = new HashMap<>();
		String prefix = folder.endsWith("/") ? folder : folder + "/";
		try {
			ModList.get().getModFileById(WorldEventsMod.MOD_ID).getFile().getContents().visitContent(prefix, (path, resource) -> {
				String name = path.substring(path.lastIndexOf('/') + 1);
				// Only the files directly inside the folder.
				if (!name.isEmpty() && path.equals(prefix + name)) {
					try {
						files.put(name, new String(resource.readAllBytes(), StandardCharsets.UTF_8));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		return files;
	}
}
