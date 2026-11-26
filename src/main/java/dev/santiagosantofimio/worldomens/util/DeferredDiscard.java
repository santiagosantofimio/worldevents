package dev.santiagosantofimio.worldomens.util;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;

public final class DeferredDiscard {
	private static final List<Entity> PENDING = new ArrayList<>();

	private DeferredDiscard() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
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
