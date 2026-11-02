package dev.santiagosantofimio.worldevents.event;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.WeightedPicker;
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

/**
 * Starts, ticks and ends events, one at a time per dimension.
 *
 * <p>Once the global cooldown is over, the next event is drawn by weight among those available in the dimension
 * and kept as the plan. The planned event starts as soon as its own conditions hold (night, players…). Drawing
 * before checking the time of day keeps weights meaningful: an event that can only start at night is not
 * crowded out by one that can start at any time.
 */
public final class EventManager {
	/** Active events tick once per second rather than every tick. */
	public static final int ACTIVE_TICK_INTERVAL = 20;
	/** A planned event that has not started after this long (two in-game days) is dropped and drawn again. */
	private static final long PLAN_TIMEOUT_TICKS = 48000;
	private static final int RANKING_LINES = 3;

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
			active.event().fastTick(level, active);
			if (tick % ACTIVE_TICK_INTERVAL == 0) {
				tickActive(level, data, active);
			}
		} else if (tick % ModConfig.get().checkIntervalTicks() == 0) {
			checkSchedule(level, data);
		}
	}

	private static void tickActive(ServerLevel level, WorldEventsData data, ActiveEvent active) {
		WorldEvent event = active.event();
		if (active.bossBar() == null) {
			// First tick since the server started: the event was loaded from the save.
			active.setBossBar(new EventBossBar(event));
			event.resume(level, active);
			WorldEventsMod.LOGGER.info("Resumed {} in {}", event.id(), level.dimension().identifier());
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

	private static void checkSchedule(ServerLevel level, WorldEventsData data) {
		ModConfig config = ModConfig.get();
		long now = level.getGameTime();
		data.initCooldownIfNew(now);
		boolean enoughPlayers = countPlayers(level) >= config.minPlayers();

		WorldEventsData.Plan plan = data.plan();
		if (plan != null) {
			WorldEvent event = EventRegistry.get(plan.eventId());
			if (event == null || !event.settings().enabled() || !event.isAvailable(level) || now - plan.plannedAt() > PLAN_TIMEOUT_TICKS) {
				data.setPlan(null);
				WorldEventsMod.LOGGER.debug("Dropped plan {} in {}", plan.eventId(), level.dimension().identifier());
			} else if (enoughPlayers && event.canStart(level)) {
				if (plan.warned() && now - plan.warnedAt() >= config.warningTicks() || config.warningTicks() == 0) {
					start(level, event);
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
		WorldEvent picked = WeightedPicker.pick(candidates, event -> event.settings().weight(), level.getRandom().nextDouble());
		if (picked != null) {
			data.setPlan(new WorldEventsData.Plan(picked.id(), now));
			WorldEventsMod.LOGGER.info("Planned {} in {}", picked.id(), level.dimension().identifier());
		}
	}

	/** Players get a moment to get ready before a drawn event starts. Events started by command skip this. */
	private static void warn(ServerLevel level, WorldEvent event, int seconds) {
		for (ServerPlayer player : level.players()) {
			String name = Lang.text(Lang.language(player), "event." + event.id() + ".name");
			player.sendSystemMessage(Lang.component(player, "event.warning", name, seconds).withStyle(event.textColor()));
			player.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_BELL, SoundSource.MASTER,
					player.getX(), player.getY(), player.getZ(), 1.0f, 0.7f, player.getRandom().nextLong()));
		}
		WorldEventsMod.LOGGER.info("Warned of {} in {}", event.id(), level.dimension().identifier());
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

	/** Ticks until the next event may be drawn in this dimension (0 if ready). */
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

	/** The event planned to run next in {@code level}, or {@code null}. */
	public static WorldEvent planned(ServerLevel level) {
		WorldEventsData.Plan plan = WorldEventsData.get(level).plan();
		return plan != null ? EventRegistry.get(plan.eventId()) : null;
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
		data.setPlan(null);
		data.setActive(active);
		event.start(level, active);
		for (ServerPlayer player : level.players()) {
			announceStart(player, event, true);
		}
		active.bossBar().update(level, active);
		WorldEventsMod.LOGGER.info("Started {} in {} for {} ticks", event.id(), level.dimension().identifier(), duration);
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
		WorldEventsMod.LOGGER.info("Ended {} in {}", event.id(), level.dimension().identifier());
		if (ModConfig.get().ranking()) {
			announceRanking(level, active);
		}
	}

	/** Credits {@code player} for something done during {@code eventId}, if it is running in their dimension. */
	public static void score(ServerPlayer player, String eventId, int points) {
		ActiveEvent active = active(player.level());
		if (active != null && active.event().id().equals(eventId)) {
			EventScores.add(active, player, points);
		}
	}

	/** Shows the best players and hands out event seals to whoever took part and is still here. */
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
			if (seals > 0) {
				ItemStack stack = EventItems.EVENT_SEAL.create(Lang.language(player), seals);
				if (!player.getInventory().add(stack)) {
					Drops.spawn(level, player.position(), stack);
				}
				player.sendSystemMessage(Lang.component(player, "event.ranking.seals", seals).withStyle(ChatFormatting.GOLD));
			}
		}
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

	/** The boss bar title {@code player} currently sees, or {@code null} if they see none. */
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

	/** Shows a title and subtitle in the middle of the screen, with a sound if {@code sound} is not {@code null}. */
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
