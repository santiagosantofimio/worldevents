package dev.santiagosantofimio.worldevents.fabric;

import java.nio.file.Path;
import java.util.Optional;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {
	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public Optional<Path> findModPath(String path) {
		return FabricLoader.getInstance().getModContainer(WorldEventsMod.MOD_ID).flatMap(container -> container.findPath(path));
	}
}
