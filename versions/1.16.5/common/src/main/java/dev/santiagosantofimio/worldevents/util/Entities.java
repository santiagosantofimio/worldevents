package dev.santiagosantofimio.worldevents.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Entity lookups that 1.16 does not have built in. */
public final class Entities {
	private Entities() {
	}

	/** Whether entities at {@code pos} are ticking (its chunk is loaded and near enough to a player). */
	public static boolean isTicking(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().isEntityTickingChunk(new ChunkPos(pos));
	}

	/** Experience orbs worth {@code amount} in all, at {@code at}, split like vanilla drops. */
	public static void awardExperience(ServerLevel level, Vec3 at, int amount) {
		while (amount > 0) {
			int value = ExperienceOrb.getExperienceValue(amount);
			amount -= value;
			level.addFreshEntity(new ExperienceOrb(level, at.x, at.y, at.z, value));
		}
	}

	/** Every loaded entity of the level that is a {@code type} and passes {@code filter}. */
	public static <T extends Entity> List<T> of(ServerLevel level, Class<T> type, Predicate<? super T> filter) {
		List<T> found = new ArrayList<>();
		for (Entity entity : level.getEntities(null, entity -> type.isInstance(entity))) {
			T typed = type.cast(entity);
			if (filter.test(typed)) {
				found.add(typed);
			}
		}
		return found;
	}
}
