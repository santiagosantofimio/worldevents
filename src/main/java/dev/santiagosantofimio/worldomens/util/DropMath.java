package dev.santiagosantofimio.worldomens.util;

public final class DropMath {
	private DropMath() {
	}

	public static int multiply(int count, double multiplier, double roll) {
		if (count <= 0 || multiplier <= 0) {
			return 0;
		}
		double scaled = count * multiplier;
		int whole = (int) Math.floor(scaled);
		return whole + (roll < scaled - whole ? 1 : 0);
	}

	public static String formatTicks(long ticks) {
		long totalSeconds = Math.max(0, (ticks + 19) / 20);
		return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
	}
}
