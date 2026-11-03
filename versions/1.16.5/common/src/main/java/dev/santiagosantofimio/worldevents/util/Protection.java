package dev.santiagosantofimio.worldevents.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Protection {
	private Protection() {
	}

	/**
	 * Whether {@code pos} is inside the server's spawn protection. Unlike vanilla, this does not depend on the
	 * operator list: events stay out of the protected area either way. Singleplayer has no spawn protection.
	 */
	public static boolean isSpawnProtected(ServerLevel level, BlockPos pos) {
		if (!(level.getServer() instanceof DedicatedServer server)) {
			return false;
		}
		int radius = server.getSpawnProtectionRadius();
		if (radius <= 0) {
			return false;
		}
		// Spawn protection only covers the overworld's spawn.
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		BlockPos center = level.getSharedSpawnPos();
		return Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) <= radius;
	}
}
