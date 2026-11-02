package dev.santiagosantofimio.worldevents.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

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
		int radius = server.spawnProtectionRadius();
		if (radius <= 0) {
			return false;
		}
		LevelData.RespawnData spawn = level.getRespawnData();
		if (level.dimension() != spawn.dimension()) {
			return false;
		}
		BlockPos center = spawn.pos();
		return Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) <= radius;
	}
}
