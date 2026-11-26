package dev.santiagosantofimio.worldomens.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Protection {
	private Protection() {
	}

	public static boolean isSpawnProtected(ServerLevel level, BlockPos pos) {
		if (!(level.getServer() instanceof DedicatedServer server)) {
			return false;
		}
		int radius = server.getSpawnProtectionRadius();
		if (radius <= 0) {
			return false;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		BlockPos center = level.getSharedSpawnPos();
		return Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) <= radius;
	}
}
