package dev.santiagosantofimio.worldevents.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** What the common code needs from the mod loader. Each loader module provides one through {@link ServiceLoader}. */
public interface Platform {
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No World Omens platform found"));

	/** The loader's config folder. */
	Path configDir();

	/**
	 * The files directly inside a folder of the mod's jar (or its resources in development), by file name, read as
	 * UTF-8. Empty if the folder does not exist.
	 */
	Map<String, String> readModFiles(String folder) throws IOException;

	/** Whether the loader's ore tag lists the block (1.16 has no shared ore tag). */
	boolean isOre(BlockState state);

	/** The language code the player's game is set to ({@code en_us}…). 1.18 servers do not keep it themselves. */
	String language(ServerPlayer player);
}
