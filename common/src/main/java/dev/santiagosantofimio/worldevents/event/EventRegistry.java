package dev.santiagosantofimio.worldevents.event;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The events this mod offers. Internal list, not a Minecraft registry. */
public final class EventRegistry {
	private static final Map<String, WorldEvent> EVENTS = new LinkedHashMap<>();

	private EventRegistry() {
	}

	public static void register(WorldEvent event) {
		if (EVENTS.putIfAbsent(event.id(), event) != null) {
			throw new IllegalArgumentException("Duplicate world event id: " + event.id());
		}
	}

	public static WorldEvent get(String id) {
		return EVENTS.get(id);
	}

	public static Collection<WorldEvent> all() {
		return Collections.unmodifiableCollection(EVENTS.values());
	}
}
