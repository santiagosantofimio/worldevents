package dev.santiagosantofimio.worldomens.event;

import dev.santiagosantofimio.worldomens.WorldOmensMod;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.data.PlayerStatsData;
import dev.santiagosantofimio.worldomens.data.WorldOmensData;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.util.Drops;
import dev.santiagosantofimio.worldomens.util.WeightedPicker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class EventManager {
	public static final int ACTIVE_TICK_INTERVAL = 20;
	private static final String CONVERGENCE = "convergence";
	private static final long PLAN_TIMEOUT_TICKS = 48000;
	private static final int RANKING_LINES = 3;

	private EventManager() {
	}

	public static void onLevelTick(ServerLevel level) {
		if (!level.tickRateManager().runsNormally()) {
			return;
		}
		int tick = level.getServer().getTickCount();
		WorldOmensData data = WorldOmensData.get(level);
		ActiveEvent active = data.active();
		if (active != null) {
			active.event().fastTick(level, active);
			if (tick % ACTIVE_TICK_INTERVAL == 0) {
				tickActive(level, data, active);
			}
		} else if (tick % ModConfig.get().checkIntervalTicks() == 0) {
			checkSchedule(level, data);
		}
	}

	private static void tickActive(ServerLevel level, WorldOmensData data, ActiveEvent active) {
		WorldEvent event = active.event();
		if (active.bossBar() == null) {
			active.setBossBar(new EventBossBar(event));
			event.resume(level, active);
			WorldOmensMod.LOGGER.info("Resumed {} in {}", event.id(), level.dimension().identifier());
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

	private static void checkSchedule(ServerLevel level, WorldOmensData data) {
		ModConfig config = ModConfig.get();
		long now = level.getGameTime();
		data.initCooldownIfNew(now);
		boolean enoughPlayers = countPlayers(level) >= config.minPlayers();

		WorldOmensData.Plan plan = data.plan();
		if (plan != null) {
			WorldEvent event = EventRegistry.get(plan.eventId());
			if (event == null || !event.settings().enabled() || !event.isAvailable(level) || now - plan.plannedAt() > PLAN_TIMEOUT_TICKS) {
				data.setPlan(null);
				WorldOmensMod.LOGGER.debug("Dropped plan {} in {}", plan.eventId(), level.dimension().identifier());
			} else if (enoughPlayers && event.canStart(level)) {
				if (plan.warned() && now - plan.warnedAt() >= config.warningTicks() || config.warningTicks() == 0) {
					start(level, event, level.getRandom().nextDouble() < config.convergenceChance());
				} else if (!plan.warned()) {
					data.setPlan(plan.warnedAt(now));
					warn(level, event, config.warningTicks() / 20);
				}
			}
			return;
		}

		if (!enoughPlayers || globalCooldownLeft(data, now) > 0 || level.getRandom().nextDouble() >= config.startChance()) {
			return;
		}
		List<WorldEvent> candidates = new ArrayList<>();
		for (WorldEvent event : EventRegistry.all()) {
			EventSettings settings = event.settings();
			if (settings.enabled() && settings.weight() > 0 && cooldownLeft(data, event, now) <= 0 && event.isAvailable(level)) {
				candidates.add(event);
			}
		}
		WorldEvent picked = WeightedPicker.pick(candidates, event -> ModConfig.get().preset().scaleWeight(event.settings().weight(), event.peaceful()),
				level.getRandom().nextDouble());
		if (picked != null) {
			data.setPlan(new WorldOmensData.Plan(picked.id(), now));
			WorldOmensMod.LOGGER.info("Planned {} in {}", picked.id(), level.dimension().identifier());
		}
	}

	private static void warn(ServerLevel level, WorldEvent event, int seconds) {
		for (ServerPlayer player : level.players()) {
			String name = Lang.text(Lang.language(player), "event." + event.id() + ".name");
			player.sendSystemMessage(Lang.component(player, "event.warning", name, seconds).withStyle(event.textColor()));
			player.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_BELL, SoundSource.MASTER,
					player.getX(), player.getY(), player.getZ(), 1.0f, 0.7f, player.getRandom().nextLong()));
		}
		WorldOmensMod.LOGGER.info("Warned of {} in {}", event.id(), level.dimension().identifier());
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

	public static long cooldownLeft(WorldOmensData data, WorldEvent event, long now) {
		Long lastEnd = data.lastEnd(event.id());
		return lastEnd == null ? 0 : Math.max(0, lastEnd + event.settings().cooldownTicks() - now);
	}

	public static long globalCooldownLeft(WorldOmensData data, long now) {
		Long lastEnd = data.lastEventEnd();
		return lastEnd == null ? 0 : Math.max(0, lastEnd + ModConfig.get().globalCooldownTicks() - now);
	}

	public static ActiveEvent active(ServerLevel level) {
		return WorldOmensData.get(level).active();
	}

	public static boolean isActive(ServerLevel level, WorldEvent event) {
		ActiveEvent active = active(level);
		return active != null && active.event() == event;
	}

	public static WorldEvent planned(ServerLevel level) {
		WorldOmensData.Plan plan = WorldOmensData.get(level).plan();
		return plan != null ? EventRegistry.get(plan.eventId()) : null;
	}

	public static boolean start(ServerLevel level, WorldEvent event) {
		return start(level, event, false);
	}

	public static boolean start(ServerLevel level, WorldEvent event, boolean convergence) {
		WorldOmensData data = WorldOmensData.get(level);
		if (data.active() != null) {
			return false;
		}
		int duration = Math.max(ACTIVE_TICK_INTERVAL, event.initialDuration(level));
		if (convergence) {
			duration = duration * 3 / 2;
		}
		CompoundTag eventData = new CompoundTag();
		if (convergence) {
			eventData.putBoolean(CONVERGENCE, true);
		}
		ActiveEvent active = new ActiveEvent(event, duration, duration, eventData);
		active.setBossBar(new EventBossBar(event));
		data.setPlan(null);
		data.setActive(active);
		event.start(level, active);
		for (ServerPlayer player : level.players()) {
			announceStart(player, event, true);
			if (convergence) {
				player.sendSystemMessage(Lang.component(player, "event.convergence.chat").withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		active.bossBar().update(level, active);
		WorldOmensMod.LOGGER.info("Started {} in {} for {} ticks", event.id(), level.dimension().identifier(), duration);
		return true;
	}

	public static WorldEvent stop(ServerLevel level) {
		WorldOmensData data = WorldOmensData.get(level);
		ActiveEvent active = data.active();
		if (active == null) {
			return null;
		}
		end(level, data);
		return active.event();
	}

	private static void end(ServerLevel level, WorldOmensData data) {
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
		WorldOmensMod.LOGGER.info("Ended {} in {}", event.id(), level.dimension().identifier());
		if (ModConfig.get().ranking()) {
			announceRanking(level, active);
		}
	}

	public static boolean isConvergence(ActiveEvent active) {
		return active.data().getBooleanOr(CONVERGENCE, false);
	}

	public static void score(ServerPlayer player, String eventId, int points) {
		ActiveEvent active = active(player.level());
		if (active != null && active.event().id().equals(eventId)) {
			int amount = isConvergence(active) ? points * 2 : points;
			EventScores.add(active, player, amount);
			PlayerStatsData.get(player.level().getServer()).add(player.getUUID(), 0, amount, 0, 0);
		}
	}

	private static void announceRanking(ServerLevel level, ActiveEvent active) {
		List<EventScores.Entry> ranking = EventScores.ranking(active);
		ModConfig config = ModConfig.get();
		if (ranking.isEmpty() || ranking.get(0).points() < config.minScoreForSeal()) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			player.sendSystemMessage(Lang.component(player, "event.ranking.header").withStyle(ChatFormatting.GOLD));
			for (int i = 0; i < Math.min(RANKING_LINES, ranking.size()); i++) {
				EventScores.Entry entry = ranking.get(i);
				player.sendSystemMessage(Lang.component(player, "event.ranking.entry", i + 1, entry.name(), entry.points()).withStyle(ChatFormatting.YELLOW));
			}
		}
		for (EventScores.Entry entry : ranking) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.player());
			if (player == null || player.level() != level || entry.points() < config.minScoreForSeal()) {
				continue;
			}
			int seals = config.sealsPerEvent() + (entry == ranking.get(0) ? config.sealsForWinner() : 0);
			if (isConvergence(active)) {
				seals *= 2;
			}
			PlayerStatsData.get(level.getServer()).add(player.getUUID(), 1, 0, Math.max(0, seals), 0);
			if (seals > 0) {
				ItemStack stack = EventItems.EVENT_SEAL.create(Lang.language(player), seals);
				if (!player.getInventory().add(stack)) {
					Drops.spawn(level, player.position(), stack);
				}
				player.sendSystemMessage(Lang.component(player, "event.ranking.seals", seals).withStyle(ChatFormatting.GOLD));
			}
		}
	}

	public static void onPlayerEnteredLevel(ServerPlayer player) {
		ActiveEvent active = active(player.level());
		if (active != null) {
			announceStart(player, active.event(), false);
		}
	}

	public static void onPlayerLeft(ServerPlayer player) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			ActiveEvent active = WorldOmensData.get(level).active();
			if (active != null && active.bossBar() != null) {
				active.bossBar().removePlayer(player);
			}
		}
	}

	public static Component bossBarTitle(ServerPlayer player) {
		ActiveEvent active = active(player.level());
		return active != null && active.bossBar() != null ? active.bossBar().titleFor(player) : null;
	}

	private static void announceStart(ServerPlayer player, WorldEvent event, boolean withTitle) {
		String prefix = "event." + event.id() + ".start.";
		if (withTitle) {
			showTitle(player, Lang.component(player, prefix + "title").withStyle(event.textColor()), Lang.component(player, prefix + "subtitle"),
					event.startSound());
		}
		player.sendSystemMessage(Lang.component(player, prefix + "chat").withStyle(event.textColor()));
	}

	public static void showTitle(ServerPlayer player, Component title, Component subtitle, Holder<SoundEvent> sound) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		if (sound != null) {
			player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER,
					player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, player.getRandom().nextLong()));
		}
	}
}
