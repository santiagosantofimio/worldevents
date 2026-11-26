package dev.santiagosantofimio.worldomens.event;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;

public interface WorldEvent {
	String id();

	JsonObject defaultSettings();

	default void registerHooks() {
	}

	default EventSettings settings() {
		return ModConfig.get().event(id());
	}

	default boolean peaceful() {
		return false;
	}

	default boolean isAvailable(ServerLevel level) {
		return true;
	}

	default boolean canStart(ServerLevel level) {
		return true;
	}

	default int initialDuration(ServerLevel level) {
		return settings().durationTicks();
	}

	default void start(ServerLevel level, ActiveEvent active) {
	}

	default void resume(ServerLevel level, ActiveEvent active) {
	}

	default void tick(ServerLevel level, ActiveEvent active) {
	}

	default void fastTick(ServerLevel level, ActiveEvent active) {
	}

	default boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		return false;
	}

	default void end(ServerLevel level, ActiveEvent active) {
	}

	default String barDetail(ActiveEvent active, String language) {
		return null;
	}

	BossEvent.BossBarColor barColor();

	ChatFormatting textColor();

	default Holder<SoundEvent> startSound() {
		return null;
	}

	default boolean darkenSky() {
		return false;
	}
}
