package dev.santiagosantofimio.worldevents.util;

import net.minecraft.server.level.ServerLevel;

/** Time of day for events tied to the day and night cycle. Times are in ticks, 0 being sunrise. */
public final class DayCycle {
	public static final long DAY_LENGTH = 24000;
	public static final long NIGHT_START = 13000;
	public static final long DAWN = 23500;

	private DayCycle() {
	}

	public static long timeOfDay(ServerLevel level) {
		return Math.floorMod(level.getDayTime(), DAY_LENGTH);
	}

	/** Whether the dimension has days and nights: the overworld, not the Nether or the End. */
	public static boolean hasDayCycle(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
	}

	/** Whether the time of day is at least {@code from} and before {@code to}. */
	public static boolean isBetween(ServerLevel level, long from, long to) {
		long time = timeOfDay(level);
		return time >= from && time < to;
	}

	/** At night, {@code duration} cut short so it ends at dawn; otherwise {@code duration} unchanged. */
	public static int untilDawn(ServerLevel level, int duration) {
		long time = timeOfDay(level);
		if (hasDayCycle(level) && level.isDarkOutside() && time < DAWN) {
			return (int) Math.min(duration, DAWN - time);
		}
		return duration;
	}
}
