package dev.santiagosantofimio.worldevents.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class Drops {
	private Drops() {
	}

	/** Multiplies every stack in {@code drops} in place, splitting into extra stacks past the max stack size. */
	public static void multiply(List<ItemStack> drops, double multiplier, RandomSource random) {
		if (multiplier == 1.0) {
			return;
		}
		List<ItemStack> extra = new ArrayList<>();
		for (ItemStack stack : drops) {
			if (stack.isEmpty()) {
				continue;
			}
			int target = DropMath.multiply(stack.getCount(), multiplier, random.nextDouble());
			int max = stack.getMaxStackSize();
			int first = Math.min(target, max);
			int rest = target - first;
			while (rest > 0) {
				int size = Math.min(rest, max);
				extra.add(stack.copyWithCount(size));
				rest -= size;
			}
			stack.setCount(first);
		}
		drops.removeIf(ItemStack::isEmpty);
		drops.addAll(extra);
	}
}
