package dev.santiagosantofimio.worldomens.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.data.PlayerStatsData;
import dev.santiagosantofimio.worldomens.data.WorldOmensData;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.EventRegistry;
import dev.santiagosantofimio.worldomens.event.EventScores;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.event.impl.AssaultEvent;
import dev.santiagosantofimio.worldomens.event.impl.DrownedTideEvent;
import dev.santiagosantofimio.worldomens.event.impl.FishingFestivalEvent;
import dev.santiagosantofimio.worldomens.event.impl.PiglinWarbandEvent;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.PiglinWarchief;
import dev.santiagosantofimio.worldomens.mob.Tidecaller;
import dev.santiagosantofimio.worldomens.util.Advancements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

public class NewEventsGameTests {
	private static final String MOCK_PLAYER_NAME = "test-mock-player";

	@GameTest(environment = "worldomens-test:piglin_warchief", maxTicks = 200)
	public void piglinWarchiefDropsItsCleaverAndCallsReinforcements(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("relicChance", 1.0);
		Runnable restore = override(PiglinWarbandEvent.ID, settings);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
		EventManager.start(level, event(PiglinWarbandEvent.ID));
		PiglinBrute chief = PiglinWarchief.spawn(level, helper.absolutePos(new BlockPos(4, 1, 4)), "en_us", 160.0);
		PiglinBrute second = PiglinWarchief.spawn(level, helper.absolutePos(new BlockPos(5, 1, 5)), "en_us", 160.0);

		helper.assertTrue(chief != null && PiglinWarchief.is(chief), "no warchief");
		helper.assertTrue(Math.abs(chief.getMaxHealth() - 160.0f) < 1e-3, "warchief health " + chief.getMaxHealth());
		helper.assertTrue(chief.getMainHandItem().isEmpty() && chief.getOffhandItem().isEmpty(), "the warchief holds something");
		helper.assertTrue(chief.hasEffect(MobEffects.INVISIBILITY), "the warchief's brute is visible");
		helper.assertFalse(Creature.parts(chief).isEmpty(), "the warchief has no model");
		second.hurtServer(level, level.damageSources().generic(), 130.0f);

		helper.runAfterDelay(30, () -> {
			helper.assertTrue(second.isAlive() && second.getHealth() < second.getMaxHealth() / 2.0f, "the second warchief should be wounded");
			int brutes = 0;
			for (Mob mob : level.getEntitiesOfClass(Mob.class, second.getBoundingBox().inflate(16.0), mob -> mob.getTags().contains(PiglinWarbandEvent.MOB_TAG))) {
				brutes++;
			}
			helper.assertTrue(brutes >= 1, "no reinforcements at half health");
			int bosses = PlayerStatsData.get(level.getServer()).stats(player.getUUID()).bosses();
			chief.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
			helper.runAfterDelay(3, () -> {
				helper.assertValueEqual(countSpecial(helper, EventItems.WARCHIEFS_CLEAVER), 1, "cleavers dropped");
				helper.assertTrue(count(helper, Items.GOLD_INGOT) >= 8, "not enough gold from the warchief");
				helper.assertTrue(Advancements.has(player, "defeat_piglin_warchief"), "no advancement for the warchief");
				helper.assertValueEqual(PlayerStatsData.get(level.getServer()).stats(player.getUUID()).bosses(), bosses + 1, "boss stat");
				EventManager.stop(level);

				ItemStack cleaver = EventItems.WARCHIEFS_CLEAVER.create("en_us", 1);
				player.setItemInHand(InteractionHand.MAIN_HAND, cleaver);
				InteractionResult used = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
				helper.assertTrue(used instanceof InteractionResult.Success, "no gilded resolve: " + used);
				helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION) && player.hasEffect(MobEffects.FIRE_RESISTANCE), "no resolve effects");
				tickCooldowns(player, 3);
				helper.assertTrue(player.getCooldowns().isOnCooldown(cleaver), "the cleaver has no cooldown");
				helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
						"a second resolve during the cooldown");
				second.discard();
				restore.run();
				remove(player);
				helper.succeed();
			});
		});
	}

	@GameTest(environment = "worldomens-test:tidecaller", maxTicks = 100)
	public void tidecallerDropsItsConchAndTheConchBlessesTheSea(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("relicChance", 1.0);
		Runnable restore = override(DrownedTideEvent.ID, settings);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1));
		EventManager.start(level, event(DrownedTideEvent.ID));
		Drowned caller = Tidecaller.spawn(level, helper.absolutePos(new BlockPos(4, 1, 4)), "en_us", 180.0);

		helper.assertTrue(caller != null && Tidecaller.is(caller), "no tidecaller");
		helper.assertTrue(Math.abs(caller.getMaxHealth() - 180.0f) < 1e-3, "tidecaller health " + caller.getMaxHealth());
		helper.assertTrue(caller.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(), "the tidecaller holds something");
		helper.assertFalse(Creature.parts(caller).isEmpty(), "the tidecaller has no model");
		caller.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);

		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.TIDECALLERS_CONCH), 1, "conchs dropped");
			helper.assertTrue(count(helper, Items.PRISMARINE_CRYSTALS) >= 6, "not enough prismarine from the tidecaller");
			helper.assertTrue(Advancements.has(player, "defeat_tidecaller"), "no advancement for the tidecaller");
			EventManager.stop(level);

			ItemStack conch = EventItems.TIDECALLERS_CONCH.create("en_us", 1);
			player.setItemInHand(InteractionHand.MAIN_HAND, conch);
			InteractionResult used = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(used instanceof InteractionResult.Success, "no tidal blessing: " + used);
			helper.assertTrue(player.hasEffect(MobEffects.CONDUIT_POWER) && player.hasEffect(MobEffects.DOLPHINS_GRACE)
					&& player.hasEffect(MobEffects.WATER_BREATHING), "no blessing effects");
			tickCooldowns(player, 3);
			helper.assertTrue(player.getCooldowns().isOnCooldown(conch), "the conch has no cooldown");
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldomens-test:piglin_waves", maxTicks = 3000, skyAccess = true)
	public void piglinWarbandWavesEndInVictory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		JsonObject quick = new JsonObject();
		quick.addProperty("mobsPerPlayer", 1);
		quick.addProperty("extraMobsPerWave", 0);
		quick.addProperty("waveBreak", 1);
		quick.addProperty("boss", false);
		Runnable restore = override(PiglinWarbandEvent.ID, quick);
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4));
		List<ChunkPos> forced = forceArea(level, player.chunkPosition(), 2);
		runWaves(helper, level, player, forced, PiglinWarbandEvent.ID, "survive_piglin_warband", restore, spawnMobs);
	}

	@GameTest(environment = "worldomens-test:tide_waves", maxTicks = 3000, skyAccess = true)
	public void drownedTideWavesComeFromTheWaterAndEndInVictory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		helper.getLevel().setDayTime(15000);
		JsonObject quick = new JsonObject();
		quick.addProperty("mobsPerPlayer", 1);
		quick.addProperty("extraMobsPerWave", 0);
		quick.addProperty("waveBreak", 1);
		quick.addProperty("boss", false);
		Runnable restore = override(DrownedTideEvent.ID, quick);
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4));
		List<ChunkPos> forced = forceArea(level, player.chunkPosition(), 4);
		BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
		for (int x = -34; x <= 34; x++) {
			for (int z = -34; z <= 34; z++) {
				boolean island = Math.abs(x) <= 4 && Math.abs(z) <= 4;
				level.setBlock(center.offset(x, -7, z), Blocks.STONE.defaultBlockState(), 2);
				for (int y = -6; y <= -1; y++) {
					level.setBlock(center.offset(x, y, z), (island ? Blocks.STONE : Blocks.WATER).defaultBlockState(), 2);
				}
				level.setBlock(center.offset(x, 0, z), (island ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
			}
		}
		player.teleportTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
		helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.isDarkOutside(), "waiting for night")).thenExecute(() -> {
			DrownedTideEvent tide = (DrownedTideEvent) event(DrownedTideEvent.ID);
			helper.assertTrue(DrownedTideEvent.isOnCoast(level, player), "a player on the island is not on a coast");
			helper.assertTrue(tide.canStart(level), "the tide cannot start on a coast at night");
			runWaves(helper, level, player, forced, DrownedTideEvent.ID, "survive_drowned_tide", restore, spawnMobs);
		});
	}

	@GameTest(environment = "worldomens-test:fishing_festival", maxTicks = 100)
	public void fishingFestivalRewardsCatchesButNotDroppedItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("goal", 2);
		settings.addProperty("catchLimit", 3);
		Runnable restore = override(FishingFestivalEvent.ID, settings);
		for (int x = 0; x < 6; x++) {
			for (int z = 0; z < 6; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
				helper.setBlock(x, 1, z, Blocks.WATER);
			}
		}
		ServerPlayer player = player(helper, new BlockPos(8, 1, 8));
		helper.getLevel().setDayTime(2000);
		helper.startSequence().thenWaitUntil(() -> helper.assertFalse(level.isDarkOutside(), "waiting for daylight"))
				.thenExecute(() -> festivalChecks(helper, level, player, restore)).thenSucceed();
	}

	private static void festivalChecks(GameTestHelper helper, ServerLevel level, ServerPlayer player, Runnable restore) {
		FishingFestivalEvent festival = (FishingFestivalEvent) event(FishingFestivalEvent.ID);
		helper.assertTrue(festival.isAvailable(level), "not available in the overworld");
		helper.assertTrue(festival.canStart(level), "cannot start with water nearby in daytime");
		helper.assertTrue(EventManager.start(level, festival), "the festival did not start");
		ActiveEvent active = EventManager.active(level);
		festival.tick(level, active);
		helper.assertTrue(player.hasEffect(MobEffects.LUCK), "no luck from the festival");

		BlockPos waterPos = helper.absolutePos(new BlockPos(3, 1, 3));
		FishingHook hook = new FishingHook(player, level, 0, 0);
		hook.setPos(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);
		player.fishing = hook;
		int xp = player.totalExperience;

		ItemEntity dropped = new ItemEntity(level, waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5, new ItemStack(Items.COD));
		dropped.setThrower(player);
		level.addFreshEntity(dropped);
		helper.assertValueEqual(FishingFestivalEvent.catches(active, player), 0, "an item thrown by the player counted as a catch");

		for (int i = 1; i <= 3; i++) {
			level.addFreshEntity(new ItemEntity(level, waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5, new ItemStack(Items.SALMON)));
		}
		level.addFreshEntity(new ItemEntity(level, waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5, new ItemStack(Items.COD)));
		helper.assertValueEqual(FishingFestivalEvent.catches(active, player), 3, "catches stop at the limit");
		helper.assertValueEqual(player.totalExperience, xp + 9, "experience per catch");
		helper.assertTrue(Advancements.has(player, "festival_angler"), "no advancement at the goal");
		helper.assertValueEqual(EventScores.ranking(active).getFirst().points(), EventManager.isConvergence(active) ? 6 : 3, "festival score");
		helper.assertValueEqual(PlayerStatsData.get(level.getServer()).stats(player.getUUID()).points(),
				EventManager.isConvergence(active) ? 6 : 3, "stats points");

		player.fishing = null;
		ItemEntity far = new ItemEntity(level, waterPos.getX() + 20.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5, new ItemStack(Items.COD));
		level.addFreshEntity(far);
		helper.assertValueEqual(FishingFestivalEvent.catches(active, player), 3, "an item with no hook counted");
		far.discard();
		EventManager.stop(level);
		helper.assertFalse(player.hasEffect(MobEffects.LUCK), "luck stayed after the festival");
		restore.run();
		remove(player);
	}

	private static void runWaves(GameTestHelper helper, ServerLevel level, ServerPlayer player, List<ChunkPos> forced, String eventId,
			String advancement, Runnable restore, boolean spawnMobs) {
		boolean[] started = {false};
		int[] lastWave = {0};
		helper.onEachTick(() -> {
			if (!started[0]) {
				boolean ready = true;
				for (ChunkPos chunk : forced) {
					ready &= level.isPositionEntityTicking(new BlockPos(chunk.getMiddleBlockX(), player.getBlockY(), chunk.getMiddleBlockZ()));
				}
				if (ready) {
					EventManager.start(level, event(eventId));
					started[0] = true;
				}
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null) {
				lastWave[0] = AssaultEvent.wave(active);
				for (Mob mob : AssaultEvent.waveMobs(active)) {
					mob.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
				}
				return;
			}
			helper.assertValueEqual(lastWave[0], AssaultEvent.WAVES, "waves before the end");
			helper.assertTrue(Advancements.has(player, advancement), "no advancement for beating the event");
			restore.run();
			for (ChunkPos chunk : forced) {
				level.setChunkForced(chunk.x, chunk.z, false);
			}
			level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, level.getServer());
			remove(player);
			helper.succeed();
		});
	}

	private static WorldEvent event(String id) {
		return EventRegistry.get(id);
	}

	private static void reset(ServerLevel level) {
		EventManager.stop(level);
		WorldOmensData.get(level).setPlan(null);
		for (ServerPlayer leftover : new ArrayList<>(level.getServer().getPlayerList().getPlayers())) {
			if (leftover.getScoreboardName().equals(MOCK_PLAYER_NAME)) {
				remove(leftover);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos relative) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(relative);
		player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return player;
	}

	private static void remove(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}

	private static void tickCooldowns(ServerPlayer player, int ticks) {
		for (int i = 0; i < ticks; i++) {
			player.getCooldowns().tick();
		}
	}

	private static int count(GameTestHelper helper, net.minecraft.world.item.Item item) {
		int total = 0;
		for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
			if (entity.getItem().is(item)) {
				total += entity.getItem().getCount();
			}
		}
		return total;
	}

	private static int countSpecial(GameTestHelper helper, EventItems.EventItem item) {
		int total = 0;
		for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
			if (item.is(entity.getItem())) {
				total += entity.getItem().getCount();
			}
		}
		return total;
	}

	private static List<ChunkPos> forceArea(ServerLevel level, ChunkPos center, int radius) {
		List<ChunkPos> forced = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
				if (level.setChunkForced(chunk.x, chunk.z, true)) {
					forced.add(chunk);
				}
			}
		}
		return forced;
	}

	private static Runnable override(String eventId, JsonObject values) {
		Path path = ModConfig.path();
		try {
			String original = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject json = JsonParser.parseString(original).getAsJsonObject();
			JsonObject section = json.getAsJsonObject("events").getAsJsonObject(eventId);
			for (String key : values.keySet()) {
				section.add(key, values.get(key));
			}
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
}
