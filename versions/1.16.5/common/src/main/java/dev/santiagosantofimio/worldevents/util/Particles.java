package dev.santiagosantofimio.worldevents.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Particle helpers for what 1.18 does not have built in. */
public final class Particles {
	private Particles() {
	}

	/** Coloured dust, from an RGB colour like {@code 0xFFD700}. */
	public static DustParticleOptions dust(int rgb, float scale) {
		return new DustParticleOptions(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, scale);
	}

	/** Particles that every player in the level sees, however far away (shooting stars, the treasure's beam). */
	public static void far(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
			double dx, double dy, double dz, double speed) {
		for (ServerPlayer player : level.players()) {
			level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
		}
	}
}
