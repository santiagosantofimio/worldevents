package dev.santiagosantofimio.worldomens;

import dev.santiagosantofimio.worldomens.command.WorldOmensCommand;
import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.EventRegistry;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.event.impl.BlessedHarvestEvent;
import dev.santiagosantofimio.worldomens.event.impl.BlizzardEvent;
import dev.santiagosantofimio.worldomens.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldomens.event.impl.EclipseEvent;
import dev.santiagosantofimio.worldomens.event.impl.EruptionEvent;
import dev.santiagosantofimio.worldomens.event.impl.GoldRushEvent;
import dev.santiagosantofimio.worldomens.event.impl.FireflyNightEvent;
import dev.santiagosantofimio.worldomens.mob.Tidecaller;
import dev.santiagosantofimio.worldomens.mob.PiglinWarchief;
import dev.santiagosantofimio.worldomens.event.impl.PiglinWarbandEvent;
import dev.santiagosantofimio.worldomens.event.impl.FishingFestivalEvent;
import dev.santiagosantofimio.worldomens.event.impl.DrownedTideEvent;
import dev.santiagosantofimio.worldomens.event.impl.HordeEvent;
import dev.santiagosantofimio.worldomens.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldomens.event.impl.MeteorShowerEvent;
import dev.santiagosantofimio.worldomens.event.impl.ThunderstormEvent;
import dev.santiagosantofimio.worldomens.event.impl.TreasureHuntEvent;
import dev.santiagosantofimio.worldomens.event.impl.VoidStormEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.BloodEye;
import dev.santiagosantofimio.worldomens.mob.BloodMoonLord;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.EclipseWraith;
import dev.santiagosantofimio.worldomens.mob.FrostYeti;
import dev.santiagosantofimio.worldomens.mob.HarvestSpirit;
import dev.santiagosantofimio.worldomens.mob.HordeWarlord;
import dev.santiagosantofimio.worldomens.mob.LivingMeteorite;
import dev.santiagosantofimio.worldomens.mob.MagmaColossus;
import dev.santiagosantofimio.worldomens.mob.MimicSack;
import dev.santiagosantofimio.worldomens.mob.NomadMerchant;
import dev.santiagosantofimio.worldomens.mob.SpectralCaptain;
import dev.santiagosantofimio.worldomens.mob.StormSpirit;
import dev.santiagosantofimio.worldomens.mob.VoidStalker;
import dev.santiagosantofimio.worldomens.util.DeferredDiscard;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldOmensMod implements ModInitializer {
	public static final String MOD_ID = "worldomens";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
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
		EventRegistry.register(new FishingFestivalEvent());
		EventRegistry.register(new PiglinWarbandEvent());
		EventRegistry.register(new DrownedTideEvent());

		Creature.registerKinds(BloodMoonLord.KIND, BloodEye.KIND, MimicSack.KIND, LivingMeteorite.KIND, StormSpirit.KIND,
				HarvestSpirit.KIND, NomadMerchant.KIND, HordeWarlord.KIND, SpectralCaptain.KIND, MagmaColossus.KIND, VoidStalker.KIND,
				EclipseWraith.KIND, FrostYeti.KIND, PiglinWarchief.KIND, Tidecaller.KIND);

		ModConfig.load();
		Lang.load();

		DeferredDiscard.register();
		Creature.registerHooks();
		for (WorldEvent event : EventRegistry.all()) {
			event.registerHooks();
		}
		EventItems.registerHooks();
		ServerTickEvents.END_LEVEL_TICK.register(EventManager::onLevelTick);
		ServerPlayerEvents.JOIN.register(EventManager::onPlayerEnteredLevel);
		ServerPlayerEvents.LEAVE.register(EventManager::onPlayerLeft);
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
				EventManager.onPlayerEnteredLevel(player));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				WorldOmensCommand.register(dispatcher));

		LOGGER.info("World Omens loaded with {} events", EventRegistry.all().size());
	}
}
