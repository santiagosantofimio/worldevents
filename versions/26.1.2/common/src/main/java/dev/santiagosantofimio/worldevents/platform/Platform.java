package dev.santiagosantofimio.worldevents.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/** What the common code needs from the mod loader. Each loader module provides one through {@link ServiceLoader}. */
public interface Platform {
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No World Events platform found"));

	/** The loader's config folder. */
	Path configDir();

	/** A folder or file inside the mod's jar (or its resources in development). */
	Optional<Path> findModPath(String path);
}
