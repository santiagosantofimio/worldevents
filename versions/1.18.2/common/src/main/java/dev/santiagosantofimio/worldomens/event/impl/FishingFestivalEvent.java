package dev.santiagosantofimio.worldomens.event.impl;

import net.minecraft.util.Mth;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.data.WorldOmensData;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.util.Advancements;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;

public final class FishingFestivalEvent implements WorldEvent {
	public static final String ID = "fishing_festival";
	private static final String CATCHES = "catches";
	private static final long EARLIEST_START = 0;
	private static final long LATEST_START = 9000;
	private static final int WATER_SEARCH_RADIUS = 10;
	private static final int LUCK_TICKS = 200;
	private static final double HOOK_DISTANCE_SQ = 9.0;

	@Override
	public boolean peaceful() {
		return true;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		JsonObject json = EventSettings.defaults(true, 8, 4800, 480);
		json.addProperty("luckLevel", 2);
		json.addProperty("pointsPerCatch", 1);
		json.addProperty("experiencePerCatch", 3);
		json.addProperty("catchLimit", 40);
		json.addProperty("goal", 10);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.BLUE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.AQUA;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.NOTE_BLOCK_CHIME;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && DayCycle.hasDayCycle(level);
	}

	@Override
	public boolean canStart(ServerLevel level) {
		if (level.isNight() || !DayCycle.isBetween(level, EARLIEST_START, LATEST_START)) {
			return false;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && level.containsAnyLiquid(player.getBoundingBox().inflate(WATER_SEARCH_RADIUS))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		int amplifier = Mth.clamp(settings().getInt("luckLevel") - 1, 0, 9);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.isAlive()) {
				player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, amplifier, true, false));
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		for (ServerPlayer player : level.players()) {
			player.removeEffect(MobEffects.LUCK);
		}
	}

	public static int catches(ActiveEvent active, ServerPlayer player) {
		return active.data().getCompound(CATCHES).getInt(player.getUUID().toString());
	}

	private void recordCatch(ServerLevel level, ActiveEvent active, ServerPlayer player) {
		EventSettings settings = settings();
		int limit = Math.max(1, settings.getInt("catchLimit"));
		CompoundTag all = active.data().getCompound(CATCHES);
		String key = player.getUUID().toString();
		int count = all.getInt(key);
		if (count >= limit) {
			return;
		}
		count++;
		all.putInt(key, count);
		active.data().put(CATCHES, all);
		WorldOmensData.get(level).setDirty();
		EventManager.score(player, ID, Math.max(0, settings.getInt("pointsPerCatch")));
		player.giveExperiencePoints(Mth.clamp(settings.getInt("experiencePerCatch"), 0, 100));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
		int goal = Math.max(1, settings.getInt("goal"));
		player.displayClientMessage(Lang.component(player, "event.fishing_festival.catch", count, goal).withStyle(ChatFormatting.AQUA), true);
		if (count == goal) {
			Advancements.award(player, "festival_angler");
		}
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof ItemEntity item) || item.getThrower() != null) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active == null || active.event() != this) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				FishingHook hook = player.fishing;
				if (hook != null && !player.isSpectator() && hook.distanceToSqr(item) <= HOOK_DISTANCE_SQ) {
					recordCatch(level, active, player);
					return;
				}
			}
		});
	}
}
