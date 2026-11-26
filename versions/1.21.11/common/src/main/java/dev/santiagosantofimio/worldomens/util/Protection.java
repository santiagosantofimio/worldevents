package dev.santiagosantofimio.worldomens.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

public final class Protection {
	private Protection() {
	}

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
