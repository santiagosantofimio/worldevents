package dev.santiagosantofimio.worldevents.event;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;

public interface WorldEvent {
	/** Stable id used in config, commands, saves and language keys ({@code event.<id>.name}). */
	String id();

	/** Config defaults; start from {@link EventSettings#defaults} and add the event's own keys. */
	JsonObject defaultSettings();

	/** Registers Fabric API callbacks the event needs. Called once at startup. */
	default void registerHooks() {
	}

	default EventSettings settings() {
		return ModConfig.get().event(id());
	}

	/**
	 * Whether the event can happen in this dimension at all right now (dimension type, difficulty, game rules),
	 * ignoring the time of day. Checked when drawing the next event, before it starts, and by {@code /worldevents start}.
	 */
	default boolean isAvailable(ServerLevel level) {
		return true;
	}

	/** Whether this is the right moment to start (time of day…). A planned event waits until this is true. */
	default boolean canStart(ServerLevel level) {
		return true;
	}

	/** Length of a new run in ticks. Events tied to the day cycle can shorten it. */
	default int initialDuration(ServerLevel level) {
		return settings().durationTicks();
	}

	/** Called once when the event begins. Announcements are handled by {@link EventManager}. */
	default void start(ServerLevel level, ActiveEvent active) {
	}

	/** Called after a server restart for an event that was running; must not repeat one-time effects. */
	default void resume(ServerLevel level, ActiveEvent active) {
	}

	/** Called every {@link EventManager#ACTIVE_TICK_INTERVAL} ticks while active. */
	default void tick(ServerLevel level, ActiveEvent active) {
	}

	/** Called every tick while active, for short-lived visual effects. Keep it cheap. */
	default void fastTick(ServerLevel level, ActiveEvent active) {
	}

	/** Lets an event end before its time runs out, e.g. at dawn. */
	default boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		return false;
	}

	/** Called once when the event ends, whether by time, early end or command. */
	default void end(ServerLevel level, ActiveEvent active) {
	}

	/** Extra text for the boss bar, between the event name and the time left (a wave counter…), or {@code null}. */
	default String barDetail(ActiveEvent active, String language) {
		return null;
	}

	BossEvent.BossBarColor barColor();

	ChatFormatting textColor();

	/** Sound played to each player when the event starts, or {@code null} for none. */
	default Holder<SoundEvent> startSound() {
		return null;
	}

	/** Darkens the sky for players in the boss bar (vanilla boss bar flag, like the Wither). */
	default boolean darkenSky() {
		return false;
	}
}
