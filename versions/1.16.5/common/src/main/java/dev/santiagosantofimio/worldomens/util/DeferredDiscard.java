package dev.santiagosantofimio.worldomens.util;

import dev.santiagosantofimio.worldomens.platform.Hooks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;

public final class DeferredDiscard {
	private static final List<Entity> PENDING = new ArrayList<>();

	private DeferredDiscard() {
	}

	public static void register() {
		Hooks.END_SERVER_TICK.register(server -> {
			if (!PENDING.isEmpty()) {
				List<Entity> batch = new ArrayList<>(PENDING);
				PENDING.clear();
				batch.forEach(Entity::remove);
			}
		});
	}

	public static void discard(Entity entity) {
		PENDING.add(entity);
	}
}
