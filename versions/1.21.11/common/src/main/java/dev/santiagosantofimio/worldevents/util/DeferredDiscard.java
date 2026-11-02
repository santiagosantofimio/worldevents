package dev.santiagosantofimio.worldevents.util;

import dev.santiagosantofimio.worldevents.platform.Hooks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;

/**
 * Removes entities at the end of the server tick. Used for event entities that load from a save after their event
 * is over: they cannot be removed inside the load callback, while they are still being added to the level.
 */
public final class DeferredDiscard {
	private static final List<Entity> PENDING = new ArrayList<>();

	private DeferredDiscard() {
	}

	public static void register() {
		Hooks.END_SERVER_TICK.register(server -> {
			if (!PENDING.isEmpty()) {
				List<Entity> batch = new ArrayList<>(PENDING);
				PENDING.clear();
				batch.forEach(Entity::discard);
			}
		});
	}

	public static void discard(Entity entity) {
		PENDING.add(entity);
	}
}
