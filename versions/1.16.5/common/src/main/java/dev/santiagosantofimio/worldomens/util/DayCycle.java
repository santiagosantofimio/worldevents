package dev.santiagosantofimio.worldomens.util;

import net.minecraft.server.level.ServerLevel;

public final class DayCycle {
	public static final long DAY_LENGTH = 24000;
	public static final long NIGHT_START = 13000;
	public static final long DAWN = 23500;

	private DayCycle() {
	}

	public static long timeOfDay(ServerLevel level) {
		return Math.floorMod(level.getDayTime(), DAY_LENGTH);
	}

	public static boolean hasDayCycle(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
	}

	public static boolean isBetween(ServerLevel level, long from, long to) {
		long time = timeOfDay(level);
		return time >= from && time < to;
	}

	public static int untilDawn(ServerLevel level, int duration) {
		long time = timeOfDay(level);
		if (hasDayCycle(level) && level.isNight() && time < DAWN) {
			return (int) Math.min(duration, DAWN - time);
		}
		return duration;
	}
}
