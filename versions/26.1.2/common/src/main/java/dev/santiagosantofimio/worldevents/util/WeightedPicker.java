package dev.santiagosantofimio.worldevents.util;

import java.util.List;
import java.util.function.ToIntFunction;

public final class WeightedPicker {
	private WeightedPicker() {
	}

	/**
	 * Picks one entry with probability proportional to its weight. Entries with weight 0 or less are never picked.
	 *
	 * @param roll a uniform random value in [0, 1)
	 * @return the picked entry, or {@code null} if no entry has a positive weight
	 */
	public static <T> T pick(List<T> entries, ToIntFunction<T> weight, double roll) {
		long total = 0;
		for (T entry : entries) {
			total += Math.max(0, weight.applyAsInt(entry));
		}
		if (total <= 0) {
			return null;
		}
		long target = (long) (roll * total);
		for (T entry : entries) {
			int w = Math.max(0, weight.applyAsInt(entry));
			if (target < w) {
				return entry;
			}
			target -= w;
		}
		return null;
	}
}
