package dev.santiagosantofimio.worldevents.forge;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

public class ForgePlatform implements Platform {
	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isOre(BlockState state) {
		return Tags.Blocks.ORES.contains(state.getBlock());
	}

	@Override
	public String language(ServerPlayer player) {
		// Forge keeps the player's game language; vanilla 1.16 does not.
		return player.getLanguage();
	}

	@Override
	public Map<String, String> readModFiles(String folder) throws IOException {
		Path dir = ModList.get().getModFileById(WorldEventsMod.MOD_ID).getFile().findResource(folder);
		Map<String, String> files = new HashMap<>();
		if (!Files.isDirectory(dir)) {
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
