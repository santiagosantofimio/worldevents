package dev.santiagosantofimio.worldevents;

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
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The mod's setup, shared by every loader: the loader module calls {@link #init()} once and forwards its events to {@link Hooks}. */
public final class WorldEventsMod {
	public static final String MOD_ID = "worldevents";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private WorldEventsMod() {
	}

	public static void init() {
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
		// What each kind wears, so a creature loaded from disk gets its parts back.
		Creature.registerDress(BloodMoonLord.KIND, BloodMoonLord.ORB, BloodMoonLord.CROWN);
		Creature.registerDress(BloodEye.KIND, BloodEye.BODY);
		Creature.registerDress(MimicSack.KIND, MimicSack.BODY);
		Creature.registerDress(LivingMeteorite.KIND, LivingMeteorite.BODY);
		Creature.registerDress(StormSpirit.KIND, StormSpirit.BODY);
		Creature.registerDress(HarvestSpirit.KIND, HarvestSpirit.BODY);
		Creature.registerDress(NomadMerchant.KIND, NomadMerchant.BODY);
		Creature.registerDress(HordeWarlord.KIND, HordeWarlord.BODY);
		Creature.registerDress(SpectralCaptain.KIND, SpectralCaptain.BODY);
		Creature.registerDress(MagmaColossus.KIND, MagmaColossus.BODY);
		Creature.registerDress(VoidStalker.KIND, VoidStalker.BODY);

		ModConfig.load();
		Lang.load();

		DeferredDiscard.register();
		Creature.registerHooks();
		for (WorldEvent event : EventRegistry.all()) {
			event.registerHooks();
		}
		// After the events, so the starfall pickaxe smelts drops the gold rush has already doubled.
		EventItems.registerHooks();
		Hooks.END_LEVEL_TICK.register(EventManager::onLevelTick);
		Hooks.PLAYER_JOIN.register(EventManager::onPlayerEnteredLevel);
		Hooks.PLAYER_LEAVE.register(EventManager::onPlayerLeft);
		Hooks.PLAYER_CHANGED_LEVEL.register(EventManager::onPlayerEnteredLevel);
		// Commands are registered by the loader module, with WorldEventsCommand.register.

		LOGGER.info("World Omens loaded with {} events", EventRegistry.all().size());
	}
}
