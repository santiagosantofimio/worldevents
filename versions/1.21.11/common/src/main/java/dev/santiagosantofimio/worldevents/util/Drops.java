package dev.santiagosantofimio.worldevents.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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

	/** Drops {@code stack} at {@code at} with a small random hop, like a mob drop. */
	public static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		RandomSource random = level.getRandom();
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack,
				Mth.nextDouble(random, -0.15, 0.15), 0.3, Mth.nextDouble(random, -0.15, 0.15));
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
	}
}
