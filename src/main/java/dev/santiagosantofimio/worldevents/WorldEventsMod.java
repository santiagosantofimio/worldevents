package dev.santiagosantofimio.worldevents;

import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldevents.event.impl.GoldRushEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldEventsMod implements ModInitializer {
	public static final String MOD_ID = "worldevents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EventRegistry.register(new BloodMoonEvent());
		EventRegistry.register(new GoldRushEvent());

		ModConfig.load();
		Lang.load();

		for (WorldEvent event : EventRegistry.all()) {
			event.registerHooks();
		}
		ServerTickEvents.END_LEVEL_TICK.register(EventManager::onLevelTick);
		ServerPlayerEvents.JOIN.register(EventManager::onPlayerEnteredLevel);
		ServerPlayerEvents.LEAVE.register(EventManager::onPlayerLeft);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
				EventManager.onPlayerEnteredLevel(player));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				WorldEventsCommand.register(dispatcher));

		LOGGER.info("World Events loaded with {} events", EventRegistry.all().size());
	}
}
