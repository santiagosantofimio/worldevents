package dev.santiagosantofimio.worldevents.util;

public final class DropMath {
	private DropMath() {
	}

	/**
	 * Scales an item count by a multiplier. The fractional part becomes a chance of one extra item,
	 * so 1.5x on a single item gives 1 or 2 items with equal odds.
	 *
	 * @param roll a uniform random value in [0, 1)
	 */
	public static int multiply(int count, double multiplier, double roll) {
		if (count <= 0 || multiplier <= 0) {
			return 0;
		}
		double scaled = count * multiplier;
		int whole = (int) Math.floor(scaled);
		return whole + (roll < scaled - whole ? 1 : 0);
	}

	/** Formats a tick count as {@code m:ss}. */
	public static String formatTicks(long ticks) {
		long totalSeconds = Math.max(0, (ticks + 19) / 20);
		return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
	}
}
