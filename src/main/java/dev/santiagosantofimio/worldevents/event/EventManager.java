package dev.santiagosantofimio.worldevents.event;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.WeightedPicker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Starts, ticks and ends events, one at a time per dimension. */
public final class EventManager {
	/** Active events tick once per second rather than every tick. */
	public static final int ACTIVE_TICK_INTERVAL = 20;

	private EventManager() {
	}

	public static void onLevelTick(ServerLevel level) {
		if (!level.tickRateManager().runsNormally()) {
			return;
		}
		int tick = level.getServer().getTickCount();
		WorldEventsData data = WorldEventsData.get(level);
		ActiveEvent active = data.active();
		if (active != null) {
			if (tick % ACTIVE_TICK_INTERVAL == 0) {
				tickActive(level, data, active);
			}
		} else if (tick % ModConfig.get().checkIntervalTicks() == 0) {
			tryRandomStart(level, data);
		}
	}

	private static void tickActive(ServerLevel level, WorldEventsData data, ActiveEvent active) {
		WorldEvent event = active.event();
		if (active.bossBar() == null) {
			// First tick since the server started: the event was loaded from the save.
			active.setBossBar(new EventBossBar(event));
			event.resume(level, active);
			WorldEventsMod.LOGGER.info("Resumed {} in {}", event.id(), level.dimension());
		}
		event.tick(level, active);
		active.countDown(ACTIVE_TICK_INTERVAL);
		data.setDirty();
		if (active.remainingTicks() <= 0 || event.shouldEndEarly(level, active)) {
			end(level, data);
			return;
		}
		active.bossBar().update(level, active);
	}

	private static void tryRandomStart(ServerLevel level, WorldEventsData data) {
		ModConfig config = ModConfig.get();
		long now = level.getGameTime();
		data.initCooldownIfNew(now);
		if (now - data.lastEventEnd() < config.globalCooldownTicks()) {
			return;
		}
		if (countPlayers(level) < config.minPlayers()) {
			return;
		}
		if (level.getRandom().nextDouble() >= config.startChance()) {
			return;
		}
		List<WorldEvent> candidates = new ArrayList<>();
		for (WorldEvent event : EventRegistry.all()) {
			EventSettings settings = event.settings();
			if (settings.enabled() && settings.weight() > 0 && cooldownLeft(data, event, now) <= 0 && event.canStart(level)) {
				candidates.add(event);
			}
		}
		WorldEvent picked = WeightedPicker.pick(candidates, event -> event.settings().weight(), level.getRandom().nextDouble());
		if (picked != null) {
			start(level, picked);
		}
	}

	private static int countPlayers(ServerLevel level) {
		int count = 0;
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				count++;
			}
		}
		return count;
	}

	/** Ticks until {@code event} may start again in this dimension (0 if ready). */
	public static long cooldownLeft(WorldEventsData data, WorldEvent event, long now) {
		Long lastEnd = data.lastEnd(event.id());
		return lastEnd == null ? 0 : Math.max(0, lastEnd + event.settings().cooldownTicks() - now);
	}

	/** Ticks until any random event may start in this dimension (0 if ready). */
	public static long globalCooldownLeft(WorldEventsData data, long now) {
		Long lastEnd = data.lastEventEnd();
		return lastEnd == null ? 0 : Math.max(0, lastEnd + ModConfig.get().globalCooldownTicks() - now);
	}

	public static ActiveEvent active(ServerLevel level) {
		return WorldEventsData.get(level).active();
	}

	public static boolean isActive(ServerLevel level, WorldEvent event) {
		ActiveEvent active = active(level);
		return active != null && active.event() == event;
	}

	/** Starts {@code event} now, ignoring cooldowns and conditions. Returns false if another event is running. */
	public static boolean start(ServerLevel level, WorldEvent event) {
		WorldEventsData data = WorldEventsData.get(level);
		if (data.active() != null) {
			return false;
		}
		int duration = Math.max(ACTIVE_TICK_INTERVAL, event.initialDuration(level));
		ActiveEvent active = new ActiveEvent(event, duration, duration, new CompoundTag());
		active.setBossBar(new EventBossBar(event));
		data.setActive(active);
		event.start(level, active);
		for (ServerPlayer player : level.players()) {
			announceStart(player, event, true);
		}
		active.bossBar().update(level, active);
		WorldEventsMod.LOGGER.info("Started {} in {} for {} ticks", event.id(), level.dimension(), duration);
		return true;
	}

	/** Ends the running event in {@code level}, if any. */
	public static WorldEvent stop(ServerLevel level) {
		WorldEventsData data = WorldEventsData.get(level);
		ActiveEvent active = data.active();
		if (active == null) {
			return null;
		}
		end(level, data);
		return active.event();
	}

	private static void end(ServerLevel level, WorldEventsData data) {
		ActiveEvent active = data.active();
		WorldEvent event = active.event();
		data.setActive(null);
		data.recordEnd(event.id(), level.getGameTime());
		try {
			event.end(level, active);
		} finally {
			if (active.bossBar() != null) {
				active.bossBar().clear();
			}
		}
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Lang.component(player, "event." + event.id() + ".end").withStyle(event.textColor()));
		}
		WorldEventsMod.LOGGER.info("Ended {} in {}", event.id(), level.dimension());
	}

	/** Tells a player who joins or changes dimension about the event already running there. */
	public static void onPlayerEnteredLevel(ServerPlayer player) {
		ActiveEvent active = active(player.level());
		if (active != null) {
			announceStart(player, active.event(), false);
		}
	}

	public static void onPlayerLeft(ServerPlayer player) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			ActiveEvent active = WorldEventsData.get(level).active();
			if (active != null && active.bossBar() != null) {
				active.bossBar().removePlayer(player);
			}
		}
	}

	private static void announceStart(ServerPlayer player, WorldEvent event, boolean withTitle) {
		String prefix = "event." + event.id() + ".start.";
		if (withTitle) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Lang.component(player, prefix + "subtitle")));
			player.connection.send(new ClientboundSetTitleTextPacket(Lang.component(player, prefix + "title").withStyle(event.textColor())));
			Holder<SoundEvent> sound = event.startSound();
			if (sound != null) {
				player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER,
						player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, player.getRandom().nextLong()));
			}
		}
		player.sendSystemMessage(Lang.component(player, prefix + "chat").withStyle(event.textColor()));
	}
}
