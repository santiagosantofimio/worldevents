package dev.santiagosantofimio.worldevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.santiagosantofimio.worldevents.util.DropMath;
import dev.santiagosantofimio.worldevents.util.WeightedPicker;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UtilTest {
	@Test
	void pickRespectsWeights() {
		Map<String, Integer> weights = Map.of("a", 1, "b", 0, "c", 3);
		List<String> entries = List.of("a", "b", "c");

		assertEquals("a", WeightedPicker.pick(entries, weights::get, 0.0));
		assertEquals("a", WeightedPicker.pick(entries, weights::get, 0.24));
		assertEquals("c", WeightedPicker.pick(entries, weights::get, 0.25));
		assertEquals("c", WeightedPicker.pick(entries, weights::get, 0.999));
	}

	@Test
	void pickReturnsNullWithoutPositiveWeights() {
		assertNull(WeightedPicker.pick(List.of("a"), e -> 0, 0.5));
		assertNull(WeightedPicker.pick(List.<String>of(), e -> 1, 0.5));
	}

	@Test
	void multiplyHandlesFractions() {
		assertEquals(4, DropMath.multiply(2, 2.0, 0.99));
		assertEquals(2, DropMath.multiply(1, 1.5, 0.4));
		assertEquals(1, DropMath.multiply(1, 1.5, 0.6));
		assertEquals(0, DropMath.multiply(0, 2.0, 0.0));
		assertEquals(3, DropMath.multiply(3, 1.0, 0.0));
	}

	@Test
	void formatsTicksRoundingUp() {
		assertEquals("0:00", DropMath.formatTicks(0));
		assertEquals("0:01", DropMath.formatTicks(1));
		assertEquals("1:00", DropMath.formatTicks(1200));
		assertEquals("8:20", DropMath.formatTicks(10000));
	}
}
