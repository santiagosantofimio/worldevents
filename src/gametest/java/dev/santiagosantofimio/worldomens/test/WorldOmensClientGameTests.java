package dev.santiagosantofimio.worldomens.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldomens.WorldOmensClient;
import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.EventRegistry;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.event.impl.BlessedHarvestEvent;
import dev.santiagosantofimio.worldomens.event.impl.BlizzardEvent;
import dev.santiagosantofimio.worldomens.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldomens.event.impl.EclipseEvent;
import dev.santiagosantofimio.worldomens.event.impl.EruptionEvent;
import dev.santiagosantofimio.worldomens.event.impl.GoldRushEvent;
import dev.santiagosantofimio.worldomens.event.impl.HordeEvent;
import dev.santiagosantofimio.worldomens.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldomens.event.impl.MeteorShowerEvent;
import dev.santiagosantofimio.worldomens.event.impl.ThunderstormEvent;
import dev.santiagosantofimio.worldomens.event.impl.TreasureHuntEvent;
import dev.santiagosantofimio.worldomens.event.impl.VoidStormEvent;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.LivingMeteorite;
import dev.santiagosantofimio.worldomens.mob.MagmaColossus;
import dev.santiagosantofimio.worldomens.mob.NomadMerchant;
import dev.santiagosantofimio.worldomens.mob.SpectralCaptain;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.world.phys.Vec3;

public class WorldOmensClientGameTests implements FabricClientGameTest {
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
				singleplayer.getServer().runCommand("worldomens give @a " + item);
			}
			context.waitTicks(220);

			context.getInput().pressKey(options -> options.keyHotbarSlots[3]);
			context.waitTicks(30);
			context.takeScreenshot("worldomens_hotbar_scythe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[4]);
			context.waitTicks(30);
			context.takeScreenshot("worldomens_hand_pickaxe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[7]);
			context.waitTicks(30);
			context.takeScreenshot("worldomens_hand_warlord_axe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[8]);
			context.waitTicks(30);
			context.takeScreenshot("worldomens_hand_captains_cutlass");
			context.getInput().pressKey(options -> options.keyHotbarSlots[3]);
			context.getInput().pressKey(options -> options.keyTogglePerspective);
			context.getInput().pressKey(options -> options.keyTogglePerspective);
			context.waitTicks(10);
			context.takeScreenshot("worldomens_third_person_scythe");
			context.getInput().pressKey(options -> options.keyHotbarSlots[7]);
			context.waitTicks(30);
			context.takeScreenshot("worldomens_third_person_warlord_axe");
			context.getInput().pressKey(options -> options.keyTogglePerspective);

			context.getInput().pressKey(options -> options.keyHotbarSlots[8]);
			context.waitTicks(30);
			Vec3 before = context.computeOnClient(client -> client.player.position());
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			Vec3 after = context.computeOnClient(client -> client.player.position());
			if (after.subtract(before).horizontalDistance() < 2.0) {
				throw new AssertionError("the cutlass's lunge did not move the player: " + before + " -> " + after);
			}

			showMob(context, singleplayer, BloodMoonEvent.ID, "worldomens_creature_lord", 4,
					(event, level, active, pos) -> ((BloodMoonEvent) event).spawnLord(level, active, pos, "es_es"));
			showMob(context, singleplayer, BloodMoonEvent.ID, "worldomens_creature_blood_eye", 3,
					(event, level, active, pos) -> ((BloodMoonEvent) event).spawnEye(level, active, pos.above(), "es_es"));
			showMob(context, singleplayer, GoldRushEvent.ID, "worldomens_creature_mimic_sack", 3,
					(event, level, active, pos) -> ((GoldRushEvent) event).spawnSack(level, active, pos, "es_es"));
			showMob(context, singleplayer, MeteorShowerEvent.ID, "worldomens_creature_living_meteorite", 3,
					(event, level, active, pos) -> LivingMeteorite.spawn(level, pos, "es_es"));
			showMob(context, singleplayer, ThunderstormEvent.ID, "worldomens_creature_storm_spirit", 3,
					(event, level, active, pos) -> ((ThunderstormEvent) event).spawnSpirit(level, active, pos.above(), "es_es"));
			showMob(context, singleplayer, BlessedHarvestEvent.ID, "worldomens_creature_harvest_spirit", 3,
					(event, level, active, pos) -> ((BlessedHarvestEvent) event).spawnSpirit(level, active, pos.above(), level.players().get(0)));
			showMob(context, singleplayer, MerchantCaravanEvent.ID, "worldomens_creature_nomad_merchant", 4,
					(event, level, active, pos) -> NomadMerchant.spawn(level, pos, "es_es", active.remainingTicks() + 2400, 1.0));
			showMob(context, singleplayer, HordeEvent.ID, "worldomens_creature_horde_warlord", 5,
					(event, level, active, pos) -> ((HordeEvent) event).spawnWarlord(level, active, pos, "es_es"));
			showMob(context, singleplayer, TreasureHuntEvent.ID, "worldomens_creature_spectral_captain", 4,
					(event, level, active, pos) -> SpectralCaptain.spawn(level, pos, "es_es", 120.0));
			showMob(context, singleplayer, EruptionEvent.ID, "worldomens_creature_magma_colossus", 6,
					(event, level, active, pos) -> ((EruptionEvent) event).spawnColossus(level, active, pos, "es_es"));
			showMob(context, singleplayer, VoidStormEvent.ID, "worldomens_creature_void_stalker", 4,
					(event, level, active, pos) -> ((VoidStormEvent) event).spawnStalker(level, active, pos, "es_es"));
			showMob(context, singleplayer, EclipseEvent.ID, "worldomens_creature_eclipse_wraith", 12,
					(event, level, active, pos) -> ((EclipseEvent) event).spawnWraith(level, active, pos.above(2), "es_es"));
			showMob(context, singleplayer, BlizzardEvent.ID, "worldomens_creature_frost_yeti", 6,
					(event, level, active, pos) -> ((BlizzardEvent) event).spawnYeti(level, active, pos, "es_es"));

			singleplayer.getServer().runCommand("gamemode creative @a");
			showSummoned(context, singleplayer, MagmaColossus.KIND, "worldomens_showcase_colossus_front", 9, 180.0f);
			showSummoned(context, singleplayer, MagmaColossus.KIND, "worldomens_showcase_colossus_side", 9, 135.0f);

			Runnable restoreConfig = fastMeteors();
			singleplayer.getServer().runCommand("time set midnight");
			singleplayer.getServer().runOnServer(server -> EventManager.start(server.getPlayerList().getPlayers().get(0).level(),
					EventRegistry.get(MeteorShowerEvent.ID)));
			context.getInput().lookAt(0.0f, -30.0f);
			for (int frame = 0; frame < 4; frame++) {
				context.waitTicks(60);
				context.takeScreenshot("worldomens_meteor_shower_" + frame);
			}
			singleplayer.getServer().runOnServer(server -> EventManager.stop(server.getPlayerList().getPlayers().get(0).level()));
			restoreConfig.run();
			singleplayer.getServer().runCommand("time set noon");
			context.getInput().lookAt(0.0f, 12.0f);

			singleplayer.getServer().runCommand("gamemode creative @a");
			context.waitTicks(5);
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitForScreen(CreativeModeInventoryScreen.class);
			context.runOnClient(client -> ((FabricCreativeModeInventoryScreen) client.gui.screen())
					.setSelectedTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorldOmensClient.TAB)));
			context.waitTicks(10);
			context.takeScreenshot("worldomens_creative_tab");
			context.setScreen(() -> null);
		}
	}

	private static Runnable fastMeteors() {
		Path path = ModConfig.path();
		try {
			String original = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject json = JsonParser.parseString(original).getAsJsonObject();
			JsonObject section = json.getAsJsonObject("events").getAsJsonObject(MeteorShowerEvent.ID);
			section.addProperty("shootingStarInterval", 1);
			section.addProperty("impactChance", 0.25);
			section.addProperty("impactsPerPlayer", 4);
			Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
			ModConfig.load();
			return () -> {
				try {
					Files.writeString(path, original, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				ModConfig.load();
			};
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void showSummoned(ClientGameTestContext context, TestSingleplayerContext singleplayer, Creature.Kind kind, String screenshot,
			int distance, float yaw) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.clearFire();
			Mob mob = Creature.summon(kind, player.level(), player.blockPosition().offset(0, 0, distance), "es_es", null);
			if (mob != null) {
				mob.setNoAi(true);
				mob.setYRot(yaw);
				mob.setYHeadRot(yaw);
				mob.setYBodyRot(yaw);
			}
		});
		context.getInput().lookAt(0.0f, 12.0f);
		context.waitTicks(40);
		context.takeScreenshot(screenshot);
		singleplayer.getServer().runOnServer(server -> {
			for (Mob mob : server.getPlayerList().getPlayers().get(0).level().getEntitiesOfClass(Mob.class,
					server.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(32.0), kind::is)) {
				Creature.remove(mob);
			}
		});
		context.waitTicks(5);
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
				mob.setNoAi(true);
				mob.setTarget(null);
				mob.setYRot(180.0f);
				mob.setYHeadRot(180.0f);
				mob.setYBodyRot(180.0f);
			}
		});
		context.getInput().lookAt(0.0f, 12.0f);
		context.waitTicks(110);
		context.takeScreenshot(screenshot);
		singleplayer.getServer().runOnServer(server -> EventManager.stop(server.getPlayerList().getPlayers().get(0).level()));
		context.waitTicks(20);
	}
}
