package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.Protection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.levelgen.Heightmap;

/** Shared helpers for the event mobs. None of them loads chunks. */
public final class MobSpawning {
	private MobSpawning() {
	}

	public static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Name shown above the mob: translated by clients with the resource pack, {@code language} for the rest. */
	public static void name(Mob mob, String key, String language, ChatFormatting color) {
		mob.setCustomName(Component.translatableWithFallback(key, Lang.text(language, key)).withStyle(color));
		mob.setCustomNameVisible(true);
	}

	/** A surface spot {@code minDistance}-{@code maxDistance} blocks from {@code center} where {@code type} fits. */
	public static BlockPos surfaceSpot(ServerLevel level, EntityType<?> type, BlockPos center, int minDistance, int maxDistance, int attempts) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < attempts; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Mth.nextDouble(random, minDistance, maxDistance);
			int x = Mth.floor(center.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(center.getZ() + Math.sin(angle) * distance);
			// Checked before the height lookup, which would load the chunk.
			if (!level.isPositionEntityTicking(new BlockPos(x, center.getY(), z))) {
				continue;
			}
			BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (fits(level, type, pos)) {
				return pos;
			}
		}
		return null;
	}

	/** A spot around {@code center}, within {@code vertical} blocks up or down, where {@code type} can stand. */
	public static BlockPos spotNear(ServerLevel level, EntityType<?> type, BlockPos center, int minDistance, int maxDistance, int vertical, int attempts) {
		return spotNear(level, type, center, minDistance, maxDistance, vertical, attempts, pos -> true);
	}

	/** Like {@link #spotNear(ServerLevel, EntityType, BlockPos, int, int, int, int)}, for spots that also pass {@code extra}. */
	public static BlockPos spotNear(ServerLevel level, EntityType<?> type, BlockPos center, int minDistance, int maxDistance, int vertical, int attempts,
			Predicate<BlockPos> extra) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < attempts; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Mth.nextDouble(random, minDistance, maxDistance);
			int x = Mth.floor(center.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(center.getZ() + Math.sin(angle) * distance);
			BlockPos spot = spotInColumn(level, type, x, z, center.getY() + vertical, center.getY() - vertical, extra);
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}

	/** The highest spot in the column between {@code fromY} and {@code toY} where {@code type} can stand. */
	public static BlockPos spotInColumn(ServerLevel level, EntityType<?> type, int x, int z, int fromY, int toY) {
		return spotInColumn(level, type, x, z, fromY, toY, pos -> true);
	}

	private static BlockPos spotInColumn(ServerLevel level, EntityType<?> type, int x, int z, int fromY, int toY, Predicate<BlockPos> extra) {
		if (!level.isPositionEntityTicking(new BlockPos(x, fromY, z))) {
			return null;
		}
		for (int y = fromY; y >= toY; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (fits(level, type, pos) && extra.test(pos)) {
				return pos;
			}
		}
		return null;
	}

	/** Players (spectators too) within {@code distance} of {@code mob}: who sees a boss's health bar. */
	public static List<ServerPlayer> playersNear(Mob mob, double distance) {
		List<ServerPlayer> players = new ArrayList<>();
		if (mob.level() instanceof ServerLevel level) {
			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(mob) < distance * distance) {
					players.add(player);
				}
			}
		}
		return players;
	}

	private static boolean fits(ServerLevel level, EntityType<?> type, BlockPos pos) {
		BlockPos below = pos.below();
		return level.isPositionEntityTicking(pos)
				&& !Protection.isSpawnProtected(level, pos)
				&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
				&& level.getFluidState(pos).isEmpty()
				&& level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}
}
