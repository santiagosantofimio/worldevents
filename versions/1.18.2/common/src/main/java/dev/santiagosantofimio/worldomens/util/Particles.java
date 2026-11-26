package dev.santiagosantofimio.worldomens.util;

import com.mojang.math.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Particles {
	private Particles() {
	}

	public static DustParticleOptions dust(int rgb, float scale) {
		return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f), scale);
	}

	public static void far(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
			double dx, double dy, double dz, double speed) {
		for (ServerPlayer player : level.players()) {
			level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
		}
	}
}
