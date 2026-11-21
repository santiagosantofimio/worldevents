package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class FireflyNightEvent implements WorldEvent {
	public static final String ID = "firefly_night";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("lightsPerPlayer", 5);
		json.addProperty("experiencePerLight", 5);
		return json;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	@Override
	public boolean canStart(ServerLevel level) {
		return DayCycle.isBetween(level, DayCycle.NIGHT_START, 21000);
	}

	@Override
	public int initialDuration(ServerLevel level) {
		return DayCycle.untilDawn(level, settings().durationTicks());
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		return !DayCycle.isBetween(level, DayCycle.NIGHT_START, DayCycle.DAWN);
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.GREEN;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GREEN;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		int limit = Math.clamp(settings().getInt("lightsPerPlayer"), 1, 20);
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || !player.isAlive()) {
				continue;
			}
			String key = player.getUUID().toString();
			CompoundTag progress = active.data().getCompoundOrEmpty(key);
			int found = progress.getIntOr("found", 0);
			if (found >= limit) {
				continue;
			}
			BlockPos light = target(active, player);
			if (light != null && (player.distanceToSqr(Vec3.atBottomCenterOf(light)) > 48 * 48
					|| level.isPositionEntityTicking(light) && !safe(level, light))) {
				progress.remove("target");
				light = null;
			}
			if (light == null && level.canSeeSky(player.blockPosition())) {
				light = MobSpawning.spotNear(level, EntityTypes.PLAYER, player.blockPosition(), 8, 16, 3, 64,
						pos -> safe(level, pos));
				if (light != null) {
					progress.putLong("target", light.asLong());
				}
			}
			active.data().put(key, progress);
			WorldEventsData.get(level).setDirty();
			if (light == null || !level.isPositionEntityTicking(light)) {
				continue;
			}
			Vec3 center = Vec3.atBottomCenterOf(light);
			if (player.distanceToSqr(center) <= 2.25 && level.canSeeSky(player.blockPosition())) {
				found++;
				progress.putInt("found", found);
				progress.remove("target");
				active.data().put(key, progress);
				EventManager.score(player, ID, 2);
				player.giveExperiencePoints(Math.clamp(settings().getInt("experiencePerLight"), 0, 100));
				level.playSound(null, center.x, center.y, center.z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.3f);
				player.sendSystemMessage(Lang.component(player, "event.firefly_night.found", found, limit).withStyle(ChatFormatting.GREEN), true);
				if (found >= limit) {
					Advancements.award(player, "follow_the_lights");
				}
			} else {
				String direction = Math.abs(center.x - player.getX()) > Math.abs(center.z - player.getZ())
						? (center.x > player.getX() ? "east" : "west") : (center.z > player.getZ() ? "south" : "north");
				player.sendSystemMessage(Lang.component(player, "event.firefly_night.hint",
						Lang.text(Lang.language(player), "event.firefly_night." + direction),
						(int) Math.ceil(Math.sqrt(player.distanceToSqr(center))), found, limit).withStyle(ChatFormatting.YELLOW), true);
			}
		}
	}

	@Override
	public void fastTick(ServerLevel level, ActiveEvent active) {
		if (level.getGameTime() % 5 != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			BlockPos light = target(active, player);
			if (!player.isSpectator() && light != null && level.isPositionEntityTicking(light)) {
				level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false,
						light.getX() + 0.5, light.getY() + 0.8, light.getZ() + 0.5, 5, 0.25, 0.3, 0.25, 0.0);
			}
		}
	}

	private static boolean safe(ServerLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getWorldBorder().isWithinBounds(pos) && level.canSeeSky(pos)
				&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
				&& !level.getBlockState(below).is(Blocks.MAGMA_BLOCK)
				&& !level.getBlockState(below).is(Blocks.CACTUS)
				&& !level.getBlockState(pos).is(BlockTags.FIRE)
				&& !level.getBlockState(pos).is(Blocks.POWDER_SNOW)
				&& !level.getBlockState(pos).is(Blocks.SWEET_BERRY_BUSH)
				&& !level.getBlockState(pos).is(Blocks.WITHER_ROSE)
				&& level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
				&& level.noCollision(EntityTypes.PLAYER.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}

	public static BlockPos target(ActiveEvent active, ServerPlayer player) {
		CompoundTag progress = active.data().getCompoundOrEmpty(player.getUUID().toString());
		return progress.contains("target") ? BlockPos.of(progress.getLongOr("target", 0L)) : null;
	}
}
