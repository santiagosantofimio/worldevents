package dev.santiagosantofimio.worldevents.util;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class Advancements {
	/** Criterion name used by the advancements the mod grants from code. */
	public static final String CRITERION = "done";

	private Advancements() {
	}

	/** Grants one of the mod's code-driven advancements ({@code data/worldevents/advancement/<id>.json}). */
	public static void award(ServerPlayer player, String id) {
		Advancement advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(WorldEventsMod.MOD_ID, id));
		if (advancement != null) {
			player.getAdvancements().award(advancement, CRITERION);
		}
	}

	public static boolean has(ServerPlayer player, String id) {
		Advancement advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(WorldEventsMod.MOD_ID, id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}
}
