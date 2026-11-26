package dev.santiagosantofimio.worldomens.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;

public interface Platform {
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No World Omens platform found"));

	Path configDir();

	Map<String, String> readModFiles(String folder) throws IOException;
}
