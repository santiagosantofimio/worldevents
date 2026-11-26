package dev.santiagosantofimio.worldomens.util;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class Advancements {
	public static final String CRITERION = "done";

	private Advancements() {
	}

	public static void award(ServerPlayer player, String id) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(WorldOmensMod.MOD_ID, id));
		if (holder != null) {
			player.getAdvancements().award(holder, CRITERION);
		}
	}

	public static boolean has(ServerPlayer player, String id) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(WorldOmensMod.MOD_ID, id));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
