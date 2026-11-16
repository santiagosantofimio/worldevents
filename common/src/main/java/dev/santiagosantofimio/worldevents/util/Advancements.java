package dev.santiagosantofimio.worldevents.util;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class Advancements {
	/** Criterion name used by the advancements the mod grants from code. */
	public static final String CRITERION = "done";

	private Advancements() {
	}

	/** Grants one of the mod's code-driven advancements ({@code data/worldevents/advancement/<id>.json}). */
	public static void award(ServerPlayer player, String id) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, id));
		if (holder != null) {
			player.getAdvancements().award(holder, CRITERION);
		}
	}

	public static boolean has(ServerPlayer player, String id) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, id));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
