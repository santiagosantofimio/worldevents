package dev.santiagosantofimio.worldevents.test;

import dev.santiagosantofimio.worldevents.event.ActiveEvent;
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
import dev.santiagosantofimio.worldevents.fabric.WorldEventsFabricClient;
import dev.santiagosantofimio.worldevents.mob.LivingMeteorite;
import dev.santiagosantofimio.worldevents.mob.SpectralCaptain;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.ChatVisiblity;

/**
 * Visual check on a real client, run with {@code ./gradlew runClientGameTest} (not part of the build: it opens a
 * window). Gives the special items and shows each event mob during its event, and saves screenshots to
 * {@code build/run/clientGameTest/screenshots} to look at by eye.
 */
public class WorldEventsClientGameTests implements FabricClientGameTest {
	private static final String[] ITEMS = {"meteorite_fragment", "lucky_nugget", "bloody_tear", "blood_scythe", "starfall_pickaxe", "bottled_lightning",
			"golden_seed", "warlord_axe", "captains_cutlass", "colossus_hammer", "magma_heart", "unstable_pearl", "event_seal"};

	@FunctionalInterface
	private interface MobSpawner {
		Mob spawn(WorldEvent event, ServerLevel level, ActiveEvent active, BlockPos pos);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> client.options.chatVisibility().set(ChatVisiblity.HIDDEN));
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");
			singleplayer.getServer().runCommand("difficulty easy");
			for (String item : ITEMS) {
				singleplayer.getServer().runCommand("worldevents give @a " + item);
			}
			// Let the advancement toasts for the new items go away.
			context.waitTicks(220);

			context.getInput().pressKey(options -> options.keyHotbarSlots[3]);
			context.waitTicks(10);
			context.takeScreenshot("worldevents_hotbar_scythe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[4]);
			context.waitTicks(10);
			context.takeScreenshot("worldevents_hand_pickaxe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[7]);
			// A slow weapon comes up as its attack recharges: a full second for this axe.
			context.waitTicks(30);
			context.takeScreenshot("worldevents_hand_warlord_axe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[8]);
			context.waitTicks(30);
			context.takeScreenshot("worldevents_hand_captains_cutlass");
			context.getInput().pressKey(options -> options.keyHotbarSlots[3]);
			context.getInput().pressKey(options -> options.keyTogglePerspective);
			context.getInput().pressKey(options -> options.keyTogglePerspective);
			context.waitTicks(10);
			context.takeScreenshot("worldevents_third_person_scythe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[7]);
			context.waitTicks(30);
			context.takeScreenshot("worldevents_third_person_warlord_axe");
			context.getInput().pressKey(options -> options.keyTogglePerspective);

			// Event creatures only exist while their event runs, so each one gets its own event.
			showMob(context, singleplayer, BloodMoonEvent.ID, "worldevents_creature_lord", 4,
					(event, level, active, pos) -> ((BloodMoonEvent) event).spawnLord(level, active, pos, "es_es"));
			showMob(context, singleplayer, BloodMoonEvent.ID, "worldevents_creature_blood_eye", 3,
					(event, level, active, pos) -> ((BloodMoonEvent) event).spawnEye(level, active, pos.above(), "es_es"));
			showMob(context, singleplayer, GoldRushEvent.ID, "worldevents_creature_mimic_sack", 3,
					(event, level, active, pos) -> ((GoldRushEvent) event).spawnSack(level, active, pos, "es_es"));
			showMob(context, singleplayer, MeteorShowerEvent.ID, "worldevents_creature_living_meteorite", 3,
					(event, level, active, pos) -> LivingMeteorite.spawn(level, pos, "es_es"));
			showMob(context, singleplayer, ThunderstormEvent.ID, "worldevents_creature_storm_spirit", 3,
					(event, level, active, pos) -> ((ThunderstormEvent) event).spawnSpirit(level, active, pos.above(), "es_es"));
			showMob(context, singleplayer, BlessedHarvestEvent.ID, "worldevents_creature_harvest_spirit", 3,
					(event, level, active, pos) -> ((BlessedHarvestEvent) event).spawnSpirit(level, active, pos.above(), level.players().get(0)));
			showMob(context, singleplayer, MerchantCaravanEvent.ID, "worldevents_creature_nomad_merchant", 4,
					(event, level, active, pos) -> ((MerchantCaravanEvent) event).spawnCaravan(level, active, pos, "es_es"));
			showMob(context, singleplayer, HordeEvent.ID, "worldevents_creature_horde_warlord", 5,
					(event, level, active, pos) -> ((HordeEvent) event).spawnWarlord(level, active, pos, "es_es"));
			showMob(context, singleplayer, TreasureHuntEvent.ID, "worldevents_creature_spectral_captain", 4,
					(event, level, active, pos) -> SpectralCaptain.spawn(level, pos, "es_es", 120.0));
			showMob(context, singleplayer, EruptionEvent.ID, "worldevents_creature_magma_colossus", 6,
					(event, level, active, pos) -> ((EruptionEvent) event).spawnColossus(level, active, pos, "es_es"));
			showMob(context, singleplayer, VoidStormEvent.ID, "worldevents_creature_void_stalker", 4,
					(event, level, active, pos) -> ((VoidStormEvent) event).spawnStalker(level, active, pos, "es_es"));

			// The creative tab, with the special items and the creature eggs.
			singleplayer.getServer().runCommand("gamemode creative @a");
			context.waitTicks(5);
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitForScreen(CreativeModeInventoryScreen.class);
			context.runOnClient(client -> ((FabricCreativeModeInventoryScreen) client.screen)
					.setSelectedTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorldEventsFabricClient.TAB)));
			context.waitTicks(10);
			context.takeScreenshot("worldevents_creative_tab");
			context.setScreen(() -> null);
		}
	}

	private static void showMob(ClientGameTestContext context, TestSingleplayerContext singleplayer, String eventId, String screenshot,
			int distance, MobSpawner spawner) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = player.level();
			WorldEvent event = EventRegistry.get(eventId);
			EventManager.start(level, event);
			Mob mob = spawner.spawn(event, level, EventManager.active(level), player.blockPosition().offset(0, 0, distance));
			if (mob != null) {
				// Posed facing the player, and not after them: some creatures act on their own while they have a target.
				mob.setNoAi(true);
				mob.setTarget(null);
				mob.setYRot(180.0f);
				mob.setYHeadRot(180.0f);
				mob.setYBodyRot(180.0f);
			}
		});
		context.getInput().lookAt(0.0f, 12.0f);
		// Wait for the event title to fade out.
		context.waitTicks(110);
		context.takeScreenshot(screenshot);
		singleplayer.getServer().runOnServer(server -> EventManager.stop(server.getPlayerList().getPlayers().get(0).level()));
		context.waitTicks(20);
	}
}
