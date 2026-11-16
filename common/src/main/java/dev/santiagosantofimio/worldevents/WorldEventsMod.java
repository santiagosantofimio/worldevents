package dev.santiagosantofimio.worldevents;

import dev.santiagosantofimio.worldevents.command.WorldEventsCommand;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.event.impl.BlessedHarvestEvent;
import dev.santiagosantofimio.worldevents.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldevents.event.impl.EruptionEvent;
import dev.santiagosantofimio.worldevents.event.impl.GoldRushEvent;
import dev.santiagosantofimio.worldevents.event.impl.HordeEvent;
import dev.santiagosantofimio.worldevents.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldevents.event.impl.MeteorShowerEvent;
import dev.santiagosantofimio.worldevents.event.impl.ThunderstormEvent;
import dev.santiagosantofimio.worldevents.event.impl.TreasureHuntEvent;
import dev.santiagosantofimio.worldevents.event.impl.VoidStormEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.BloodEye;
import dev.santiagosantofimio.worldevents.mob.BloodMoonLord;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.HarvestSpirit;
import dev.santiagosantofimio.worldevents.mob.HordeWarlord;
import dev.santiagosantofimio.worldevents.mob.LivingMeteorite;
import dev.santiagosantofimio.worldevents.mob.MagmaColossus;
import dev.santiagosantofimio.worldevents.mob.MimicSack;
import dev.santiagosantofimio.worldevents.mob.NomadMerchant;
import dev.santiagosantofimio.worldevents.mob.SpectralCaptain;
import dev.santiagosantofimio.worldevents.mob.StormSpirit;
import dev.santiagosantofimio.worldevents.mob.VoidStalker;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The mod itself, the same on every mod loader. Each loader calls {@link #init} and feeds {@link GameEvents}. */
public final class WorldEventsMod {
	public static final String MOD_ID = "worldevents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private WorldEventsMod() {
	}

	public static void init(Path configDir) {
		EventRegistry.register(new BloodMoonEvent());
		EventRegistry.register(new GoldRushEvent());
		EventRegistry.register(new MeteorShowerEvent());
		EventRegistry.register(new ThunderstormEvent());
		EventRegistry.register(new BlessedHarvestEvent());
		EventRegistry.register(new MerchantCaravanEvent());
		EventRegistry.register(new HordeEvent());
		EventRegistry.register(new TreasureHuntEvent());
		EventRegistry.register(new EruptionEvent());
		EventRegistry.register(new VoidStormEvent());

		Creature.registerKinds(BloodMoonLord.KIND, BloodEye.KIND, MimicSack.KIND, LivingMeteorite.KIND, StormSpirit.KIND,
				HarvestSpirit.KIND, NomadMerchant.KIND, HordeWarlord.KIND, SpectralCaptain.KIND, MagmaColossus.KIND, VoidStalker.KIND);

		ModConfig.setDirectory(configDir);
		ModConfig.load();
		Lang.load();

		DeferredDiscard.register();
		Creature.registerHooks();
		for (WorldEvent event : EventRegistry.all()) {
			event.registerHooks();
		}
		// After the events, so the starfall pickaxe smelts drops the gold rush has already doubled.
		EventItems.registerHooks();
		GameEvents.LEVEL_TICK.register(EventManager::onLevelTick);
		GameEvents.PLAYER_JOIN.register(EventManager::onPlayerEnteredLevel);
		GameEvents.PLAYER_LEAVE.register(EventManager::onPlayerLeft);
		GameEvents.PLAYER_CHANGE_LEVEL.register(EventManager::onPlayerEnteredLevel);

		LOGGER.info("World Omens loaded with {} events", EventRegistry.all().size());
	}
}
