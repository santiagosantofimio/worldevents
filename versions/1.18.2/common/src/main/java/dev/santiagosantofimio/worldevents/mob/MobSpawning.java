package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.Protection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Shared helpers for the event mobs. None of them loads chunks. */
public final class MobSpawning {
	/** Where {@link #roomFor} looks around a spot, nearest first, by search radius. */
	private static final Map<Integer, List<BlockPos>> ROOM_OFFSETS = new ConcurrentHashMap<>();

	private MobSpawning() {
	}

	public static void setBase(Mob mob, Attribute attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Name shown above the mob, in {@code language}: 1.18 has no translatable names with a fallback. */
	public static void name(Mob mob, String key, String language, ChatFormatting color) {
		mob.setCustomName(new TextComponent(Lang.text(language, key)).withStyle(color));
		mob.setCustomNameVisible(true);
	}

	/** A surface spot {@code minDistance}-{@code maxDistance} blocks from {@code center} where {@code type} fits. */
	public static BlockPos surfaceSpot(ServerLevel level, EntityType<?> type, BlockPos center, int minDistance, int maxDistance, int attempts) {
		Random random = level.getRandom();
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
		Random random = level.getRandom();
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

	/**
	 * The spot nearest to {@code start} where something of {@code size} fits standing on solid ground, out of
	 * liquids; {@code null} if there is none. It looks {@code radius} blocks across and up (hillsides), and half as
	 * far down. For creatures bigger than their vanilla mob, and to free one stuck in blocks.
	 */
	public static BlockPos roomFor(ServerLevel level, EntityDimensions size, BlockPos start, int radius) {
		for (BlockPos offset : ROOM_OFFSETS.computeIfAbsent(radius, MobSpawning::roomOffsets)) {
			BlockPos pos = start.offset(offset);
			BlockPos below = pos.below();
			if (level.isPositionEntityTicking(pos)
					&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				AABB box = size.makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				if (level.noCollision(box) && !level.containsAnyLiquid(box)) {
					return pos;
				}
			}
		}
		return null;
	}

	/** Where {@link #roomFor} looks, nearest first. */
	private static List<BlockPos> roomOffsets(int radius) {
		List<BlockPos> offsets = new ArrayList<>();
		for (int dy = -radius / 2; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					offsets.add(new BlockPos(dx, dy, dz));
				}
			}
		}
		offsets.sort(Comparator.comparingInt(offset -> offset.getX() * offset.getX() + offset.getY() * offset.getY() + offset.getZ() * offset.getZ()));
		return offsets;
	}

	/** Players (spectators too) within {@code distance} of {@code mob}: who sees a boss's health bar. */
	public static List<ServerPlayer> playersNear(Mob mob, double distance) {
		List<ServerPlayer> players = new ArrayList<>();
		if (mob.level instanceof ServerLevel level) {
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
				&& level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}
}
