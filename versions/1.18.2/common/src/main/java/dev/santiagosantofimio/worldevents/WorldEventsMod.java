package dev.santiagosantofimio.worldevents;

import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.event.impl.BlessedHarvestEvent;
import dev.santiagosantofimio.worldevents.event.impl.BlizzardEvent;
import dev.santiagosantofimio.worldevents.event.impl.FireflyNightEvent;
import dev.santiagosantofimio.worldevents.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldevents.event.impl.EclipseEvent;
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
import dev.santiagosantofimio.worldevents.mob.EclipseWraith;
import dev.santiagosantofimio.worldevents.mob.FrostYeti;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class WorldEventsMod {
	public static final String MOD_ID = "worldevents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
		EventRegistry.register(new EclipseEvent());
		EventRegistry.register(new BlizzardEvent());
		EventRegistry.register(new FireflyNightEvent());

		Creature.registerKinds(BloodMoonLord.KIND, BloodEye.KIND, MimicSack.KIND, LivingMeteorite.KIND, StormSpirit.KIND,
				HarvestSpirit.KIND, NomadMerchant.KIND, HordeWarlord.KIND, SpectralCaptain.KIND, MagmaColossus.KIND, VoidStalker.KIND,
				EclipseWraith.KIND, FrostYeti.KIND);
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
		EventItems.registerHooks();
		Hooks.END_LEVEL_TICK.register(EventManager::onLevelTick);
		Hooks.PLAYER_JOIN.register(EventManager::onPlayerEnteredLevel);
		Hooks.PLAYER_LEAVE.register(EventManager::onPlayerLeft);
		Hooks.PLAYER_CHANGED_LEVEL.register(EventManager::onPlayerEnteredLevel);

		LOGGER.info("World Omens loaded with {} events", EventRegistry.all().size());
	}
}
