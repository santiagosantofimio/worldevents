package dev.santiagosantofimio.worldevents.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.EventScores;
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
import dev.santiagosantofimio.worldevents.util.Advancements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests on a real server, run with {@code ./gradlew runGameTest}. Events are one per dimension, so every
 * test has its own test environment: environments run as separate batches, one after another.
 */
public class WorldEventsGameTests {
	private static final String MOCK_PLAYER_NAME = "test-mock-player";

	@GameTest(environment = "worldevents-test:gold_rush", maxTicks = 100)
	public void goldRushDoublesOresButNotSilkTouch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		BlockPos ore = new BlockPos(4, 1, 4);

		helper.assertValueEqual(mine(helper, ore, Blocks.DIAMOND_ORE, player, pickaxe, Items.DIAMOND), 1, "diamonds without the event");

		EventManager.start(level, event(GoldRushEvent.ID));
		helper.assertValueEqual(mine(helper, ore, Blocks.DIAMOND_ORE, player, pickaxe, Items.DIAMOND), 2, "diamonds during the gold rush");
		helper.assertValueEqual(mine(helper, ore, Blocks.IRON_ORE, player, pickaxe, Items.RAW_IRON), 2, "raw iron during the gold rush");

		ItemStack silkTouch = new ItemStack(Items.DIAMOND_PICKAXE);
		silkTouch.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		helper.assertValueEqual(mine(helper, ore, Blocks.DIAMOND_ORE, player, silkTouch, Items.DIAMOND_ORE), 1, "silk-touched ore during the gold rush");

		EventManager.stop(level);
		helper.assertValueEqual(mine(helper, ore, Blocks.DIAMOND_ORE, player, pickaxe, Items.DIAMOND), 1, "diamonds after the event");
		remove(player);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:blood_moon_buffs", maxTicks = 100)
	public void bloodMoonBuffsMobsAndRestoresThem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
		// Compared to its own starting value: a zombie can spawn as a "leader" with extra health.
		float healthBefore = zombie.getMaxHealth();

		EventManager.start(level, event(BloodMoonEvent.ID));
		helper.assertTrue(Math.abs(zombie.getMaxHealth() - healthBefore * 1.5f) < 1e-3, "loaded zombie should have 1.5x health, has " + zombie.getMaxHealth());
		helper.assertTrue(zombie.getTags().contains(BloodMoonEvent.MOB_TAG), "loaded zombie is not tagged");
		helper.assertValueEqual(zombie.getHealth(), zombie.getMaxHealth(), "loaded zombie keeps full health");

		Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(5, 1, 5));
		helper.assertTrue(husk.getAttribute(Attributes.MAX_HEALTH).hasModifier(BloodMoonEvent.HEALTH_MODIFIER), "new husk has no health buff");
		helper.assertTrue(husk.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(BloodMoonEvent.DAMAGE_MODIFIER), "new husk has no damage buff");
		helper.assertValueEqual(husk.getHealth(), husk.getMaxHealth(), "new husk spawns with full health");

		EventManager.stop(level);
		helper.assertTrue(Math.abs(zombie.getMaxHealth() - healthBefore) < 1e-3, "zombie health not restored: " + zombie.getMaxHealth());
		helper.assertFalse(husk.getAttribute(Attributes.MAX_HEALTH).hasModifier(BloodMoonEvent.HEALTH_MODIFIER), "husk health buff not removed");
		helper.assertFalse(husk.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(BloodMoonEvent.DAMAGE_MODIFIER), "husk damage buff not removed");
		helper.assertFalse(husk.getTags().contains(BloodMoonEvent.MOB_TAG), "husk still tagged after the event");
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:blood_moon_loot", maxTicks = 100)
	public void bloodMoonDoublesLootAndGivesBonusXp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		EventManager.start(level, event(BloodMoonEvent.ID));
		// The evoker always drops one totem, so the doubled loot is easy to count.
		Evoker evoker = helper.spawnWithNoFreeWill(EntityType.EVOKER, new BlockPos(4, 1, 4));
		helper.assertTrue(evoker.getTags().contains(BloodMoonEvent.MOB_TAG), "evoker is not buffed");
		evoker.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.assertTrue(evoker.isDeadOrDying(), "evoker did not die");

		// Vanilla drops the base XP when the death animation ends (20 ticks); orbs before that are the bonus.
		helper.runAfterDelay(5, () -> {
			helper.assertValueEqual(count(helper, Items.TOTEM_OF_UNDYING), 2, "totems from a blood moon kill");
			int bonusXp = 0;
			for (ExperienceOrb orb : helper.getEntities(EntityType.EXPERIENCE_ORB)) {
				bonusXp += orb.getValue();
			}
			helper.assertTrue(bonusXp > 0, "no bonus XP right after the kill");
			EventManager.stop(level);
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:blood_moon_spawns", maxTicks = 3000, skyAccess = true)
	public void bloodMoonSpawnsExtraMobsNearPlayers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		Set<Mob> before = new HashSet<>(level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> true));
		helper.getLevel().setDayTime(18000);
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4), "en_us");

		// Extra mobs only spawn 24-44 blocks away in chunks that already tick entities. The test server runs ticks
		// faster than it loads chunks, so keep the area loaded and wait for it before starting.
		List<ChunkPos> forced = forceArea(level, player.chunkPosition(), 3);
		boolean[] started = {false};
		helper.onEachTick(() -> {
			if (!started[0]) {
				if (areaReady(level, forced, player.getBlockY())) {
					EventManager.start(level, event(BloodMoonEvent.ID));
					started[0] = true;
				}
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active == null || BloodMoonEvent.extraMobCount(active) == 0) {
				return;
			}
			EventManager.stop(level);
			for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> mob instanceof Enemy && !before.contains(mob))) {
				mob.discard();
			}
			unforceArea(level, forced);
			level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, level.getServer());
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:boss_bar", maxTicks = 100)
	public void bossBarUsesEachPlayersLanguage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer english = player(helper, new BlockPos(1, 1, 1), "en_us");
		ServerPlayer spanish = player(helper, new BlockPos(2, 1, 2), "es_mx");

		EventManager.start(level, event(GoldRushEvent.ID));
		assertTitle(helper, english, "Gold Rush");
		assertTitle(helper, spanish, "Fiebre del oro");

		// Switching language moves the player to the other bar on the next update (once per second).
		setLanguage(english, "es_es");
		helper.runAfterDelay(25, () -> {
			assertTitle(helper, english, "Fiebre del oro");
			EventManager.stop(level);
			helper.assertTrue(EventManager.bossBarTitle(english) == null, "boss bar still shown after the event");
			remove(english);
			remove(spanish);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:sleep", maxTicks = 100)
	public void bloodMoonBlocksSleeping(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		helper.getLevel().setDayTime(13000);
		BlockPos foot = new BlockPos(3, 1, 3);
		Block bed = Blocks.RED_BED;
		helper.setBlock(foot, bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
		helper.setBlock(foot.north(), bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
		ServerPlayer player = player(helper, new BlockPos(3, 1, 4), "es_es");

		// The bed rule (night or not) is worked out on the level tick after the time changes.
		helper.runAfterDelay(2, () -> {
			EventManager.start(level, event(BloodMoonEvent.ID));
			BlockPos bedPos = helper.absolutePos(foot);
			Either<Player.BedSleepingProblem, Unit> result = player.startSleepInBed(bedPos);
			helper.assertTrue(result.left().isPresent(), "sleeping was allowed during the blood moon");
			Component message = result.left().get().message();
			helper.assertTrue(message != null && message.getString().equals("No puedes dormir durante la luna de sangre"),
					"unexpected message: " + (message == null ? null : message.getString()));

			EventManager.stop(level);
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:meteor", maxTicks = 100)
	public void meteorCraterDigsTerrainButSparesBuildings(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		MeteorShowerEvent meteors = (MeteorShowerEvent) event(MeteorShowerEvent.ID);
		buildGround(helper);
		BlockPos chest = new BlockPos(6, 4, 4);
		BlockPos planks = new BlockPos(4, 3, 2);
		helper.setBlock(chest, Blocks.CHEST);
		helper.setBlock(planks, Blocks.OAK_PLANKS);

		meteors.impact(level, helper.absolutePos(new BlockPos(4, 3, 4)));

		helper.assertBlockPresent(Blocks.CHEST, chest);
		helper.assertBlockPresent(Blocks.OAK_PLANKS, planks);
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 3, 4));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
		helper.assertBlockPresent(Blocks.MAGMA_BLOCK, new BlockPos(4, 1, 4));
		int ores = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				for (int z = 0; z < 8; z++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).is(ConventionalBlockTags.ORES)) {
						ores++;
					}
				}
			}
		}
		helper.assertTrue(ores > 0, "the crater has no ores");
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:meteor_no_damage", maxTicks = 100)
	public void meteorOnlyDropsOresWhenBlockDamageIsOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		MeteorShowerEvent meteors = (MeteorShowerEvent) event(MeteorShowerEvent.ID);
		buildGround(helper);
		withBlockDamageOff(() -> meteors.impact(level, helper.absolutePos(new BlockPos(4, 3, 4))));

		helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(4, 3, 4));
		helper.assertBlockPresent(Blocks.DIRT, new BlockPos(4, 2, 4));
		helper.assertTrue(!helper.getEntities(EntityType.ITEM).isEmpty(), "no ores were dropped");
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:meteor_fall", maxTicks = 3000, skyAccess = true)
	public void meteorFallsAndLandsWithoutLeftovers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		helper.getLevel().setDayTime(18000);
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4), "en_us");
		List<ChunkPos> forced = forceArea(level, player.chunkPosition(), 4);
		// Every shooting star becomes a meteor, one per player.
		JsonObject meteorSettings = new JsonObject();
		meteorSettings.addProperty("shootingStarInterval", 1);
		meteorSettings.addProperty("impactChance", 1.0);
		meteorSettings.addProperty("impactsPerPlayer", 1);
		Runnable restore = overrideEventSettings(MeteorShowerEvent.ID, meteorSettings);

		boolean[] started = {false};
		helper.onEachTick(() -> {
			if (!started[0]) {
				if (areaReady(level, forced, player.getBlockY())) {
					EventManager.start(level, event(MeteorShowerEvent.ID));
					started[0] = true;
				}
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active == null || MeteorShowerEvent.landedCount(active) == 0) {
				return;
			}
			EventManager.stop(level);
			restore.run();
			unforceArea(level, forced);
			remove(player);
			List<? extends FallingBlockEntity> leftovers = level.getEntities(EntityTypeTest.forClass(FallingBlockEntity.class),
					entity -> entity.getTags().contains(WorldEventsMod.MOD_ID + ".meteor"));
			helper.assertTrue(leftovers.isEmpty(), "falling meteors left behind: " + leftovers.size());
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:blood_eye", maxTicks = 100, skyAccess = true)
	public void bloodEyeFliesDuringTheBloodMoonOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		BloodMoonEvent bloodMoon = (BloodMoonEvent) event(BloodMoonEvent.ID);
		EventManager.start(level, bloodMoon);
		Vex eye = bloodMoon.spawnEye(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 5, 4)), "en_us");
		helper.assertTrue(eye != null && BloodEye.is(eye), "no blood eye");
		assertCreature(helper, eye, "blood_eye");
		helper.assertFalse(eye.getTags().contains(BloodMoonEvent.MOB_TAG), "the eye got the blood moon buff meant for vanilla mobs");
		helper.assertTrue(eye.getMainHandItem().isEmpty(), "the eye holds an item that would float next to it");

		// Body parts must still be there a few ticks later, not just right after spawning.
		helper.runAfterDelay(5, () -> {
			assertCreature(helper, eye, "blood_eye");
			EventManager.stop(level);
			helper.assertTrue(eye.isRemoved(), "the eye stayed after the blood moon");
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:thunderstorm", maxTicks = 100)
	public void thunderstormBringsWeatherSpiritsAndBottledLightning(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject stormSettings = new JsonObject();
		stormSettings.addProperty("bottledLightningChance", 1.0);
		Runnable restore = overrideEventSettings(ThunderstormEvent.ID, stormSettings);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		ThunderstormEvent storm = (ThunderstormEvent) event(ThunderstormEvent.ID);
		EventManager.start(level, storm);
		helper.assertTrue(level.getLevelData().isThundering(), "no thunder during the storm");

		Vex spirit = storm.spawnSpirit(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 3, 4)), "en_us");
		helper.assertTrue(spirit != null && StormSpirit.is(spirit), "no storm spirit");
		assertCreature(helper, spirit, "storm_spirit");
		helper.assertTrue(spirit.getMainHandItem().isEmpty(), "the spirit holds an item that would float next to it");
		spirit.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);

		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.BOTTLED_LIGHTNING), 1, "bottled lightning from the spirit");
			helper.assertTrue(Advancements.has(player, "calm_the_storm"), "no advancement for the spirit");

			// Bottled lightning strikes where the player looks: straight down, at the floor.
			player.setXRot(90.0f);
			player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.BOTTLED_LIGHTNING.create("en_us", 1));
			InteractionResult used = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(used instanceof InteractionResult.Success, "bottled lightning did not work: " + used);
			helper.assertFalse(level.getEntities(EntityTypeTest.forClass(LightningBolt.class),
					bolt -> bolt.distanceToSqr(player) < 16 * 16).isEmpty(), "no lightning bolt near the player");

			EventManager.stop(level);
			helper.assertFalse(level.getLevelData().isThundering(), "the storm did not clear");
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:planner", maxTicks = 1200)
	public void plannedEventWaitsForItsMomentAndWarnsPlayers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject shortWarning = new JsonObject();
		shortWarning.addProperty("warningSeconds", 5);
		Runnable restore = overrideGlobalSettings(shortWarning);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4), "en_us");
		helper.getLevel().setDayTime(1000);
		WorldEventsData.get(level).setPlan(new WorldEventsData.Plan(BloodMoonEvent.ID, level.getGameTime()));

		// The manager checks every 200 ticks: by then it must still be waiting for the night.
		boolean[] night = {false};
		boolean[] warned = {false};
		helper.runAfterDelay(250, () -> {
			helper.assertTrue(EventManager.active(level) == null, "the blood moon started during the day");
			helper.assertTrue(EventManager.planned(level) == event(BloodMoonEvent.ID), "the plan was dropped");
			helper.assertFalse(WorldEventsData.get(level).plan().warned(), "players were warned during the day");
			helper.getLevel().setDayTime(13000);
			night[0] = true;
		});
		helper.onEachTick(() -> {
			if (!night[0]) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active == null) {
				WorldEventsData.Plan plan = WorldEventsData.get(level).plan();
				warned[0] |= plan != null && plan.warned();
				return;
			}
			helper.assertTrue(warned[0], "the blood moon started without a warning");
			helper.assertTrue(active.event() == event(BloodMoonEvent.ID), "the wrong event started");
			helper.assertTrue(EventManager.planned(level) == null, "the plan was not cleared");
			EventManager.stop(level);
			level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, level.getServer());
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:items", maxTicks = 100)
	public void eventItemsHaveNamesModelsAndEffects(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "es_es");
		ItemStack fragment = EventItems.METEORITE_FRAGMENT.create("es_es", 1);

		Component name = fragment.get(DataComponents.ITEM_NAME);
		helper.assertTrue(name != null && name.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("item.worldevents.meteorite_fragment")
				&& "Fragmento de meteorito".equals(translatable.getFallback()), "unexpected name: " + name);
		CustomModelData model = fragment.get(DataComponents.CUSTOM_MODEL_DATA);
		helper.assertTrue(model != null && model.strings().equals(List.of("worldevents:meteorite_fragment")), "unexpected model data: " + model);
		helper.assertTrue(EventItems.identify(fragment) == EventItems.METEORITE_FRAGMENT, "fragment not identified");
		helper.assertTrue(EventItems.identify(new ItemStack(Items.RAW_IRON)) == null, "plain raw iron identified as special");
		helper.assertFalse(EventItems.BOTTLED_LIGHTNING.create("es_es", 1).has(DataComponents.CONSUMABLE),
				"bottled lightning can be drunk like the honey bottle it is made from");

		fragment.finishUsingItem(level, player);
		MobEffectInstance haste = player.getEffect(MobEffects.HASTE);
		helper.assertTrue(haste != null && haste.getAmplifier() == 1, "no Haste II after the fragment");
		helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "no night vision after the fragment");
		remove(player);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:give", maxTicks = 100)
	public void giveCommandGivesItemsAndTheirAdvancement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),
				"worldevents give " + player.getScoreboardName() + " meteorite_fragment 3");
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),
				"worldevents give " + player.getScoreboardName() + " bottled_lightning");
		int given = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (EventItems.METEORITE_FRAGMENT.is(stack)) {
				given += stack.getCount();
			}
		}
		helper.assertValueEqual(given, 3, "fragments in the inventory");
		// The inventory advancement fires when the player's menu syncs, on their next tick.
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(Advancements.has(player, "meteorite_fragment"), "no advancement for the fragment");
			// Its base item must not count as a vanilla item that has an advancement of its own.
			AdvancementHolder mint = level.getServer().getAdvancements().get(Identifier.withDefaultNamespace("end/dragon_breath"));
			helper.assertFalse(player.getAdvancements().getOrStartProgress(mint).isDone(), "bottled lightning gave the End's dragon's breath advancement");
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:bloody_tear", maxTicks = 100)
	public void bloodyTearSummonsABloodMoonOnlyAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.BLOODY_TEAR.create("en_us", 2));
		helper.getLevel().setDayTime(6000);
		// Daylight is recalculated on the next tick.
		helper.runAfterDelay(2, () -> {
			InteractionResult day = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(day == InteractionResult.FAIL && EventManager.active(level) == null, "summoned at noon: " + day);
			helper.getLevel().setDayTime(18000);
		});
		helper.runAfterDelay(5, () -> {
			InteractionResult night = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(night instanceof InteractionResult.Success && EventManager.isActive(level, event(BloodMoonEvent.ID)),
					"no blood moon at midnight: " + night);
			EventManager.stop(level);
			level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, level.getServer());
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:tools", maxTicks = 100)
	public void scytheHealsTheWielderAndPickaxeSmeltsOres(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.BLOOD_SCYTHE.create("en_us", 1));
		player.setHealth(10.0f);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
		zombie.hurtServer(level, level.damageSources().playerAttack(player), 6.0f);
		helper.assertTrue(player.getHealth() > 10.0f, "the scythe did not heal: " + player.getHealth());

		ItemStack pickaxe = EventItems.STARFALL_PICKAXE.create("en_us", 1);
		helper.assertValueEqual(mine(helper, new BlockPos(5, 1, 5), Blocks.IRON_ORE, player, pickaxe, Items.IRON_INGOT), 1, "iron ingots from iron ore");
		helper.assertValueEqual(count(helper, Items.RAW_IRON), 0, "raw iron left");
		zombie.discard();
		remove(player);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:lord", maxTicks = 100)
	public void bloodMoonLordFightsAndDropsItsLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject lordSettings = new JsonObject();
		lordSettings.addProperty("scytheChance", 1.0);
		Runnable restore = overrideEventSettings(BloodMoonEvent.ID, lordSettings);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		BloodMoonEvent bloodMoon = (BloodMoonEvent) event(BloodMoonEvent.ID);
		EventManager.start(level, bloodMoon);
		WitherSkeleton lord = bloodMoon.spawnLord(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 1, 4)), "es_es");

		helper.assertTrue(lord != null && BloodMoonLord.is(lord), "no lord");
		helper.assertTrue(Math.abs(lord.getMaxHealth() - 200.0f) < 1e-3, "lord health should be 200 (not buffed), is " + lord.getMaxHealth());
		helper.assertTrue(EventItems.BLOOD_SCYTHE.is(lord.getMainHandItem()), "the lord does not hold the scythe");
		assertCreature(helper, lord, "blood_moon_lord", "blood_moon_lord_crown");
		helper.assertTrue(Creature.parts(lord).get(0).hasGlowingTag(), "the lord's body does not glow");
		helper.assertTrue(BloodMoonEvent.lord(EventManager.active(level)) == lord, "the event does not track the lord");

		lord.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.BLOOD_SCYTHE), 1, "scythes dropped");
			helper.assertTrue(countSpecial(helper, EventItems.BLOODY_TEAR) >= 1, "no bloody tear dropped");
			helper.assertTrue(Advancements.has(player, "defeat_blood_moon_lord"), "no advancement for the lord");
			EventManager.stop(level);
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:mimic_sack", maxTicks = 100)
	public void mimicSackIsACreatureThatBurstsIntoGold(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		GoldRushEvent goldRush = (GoldRushEvent) event(GoldRushEvent.ID);
		EventManager.start(level, goldRush);
		Rabbit sack = goldRush.spawnSack(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 1, 4)), "en_us");
		helper.assertTrue(sack != null && MimicSack.is(sack), "no mimic sack");
		assertCreature(helper, sack, "mimic_sack");

		boolean damaged = sack.hurtServer(level, level.damageSources().playerAttack(player), 1.0f);
		helper.assertFalse(damaged, "the hit damaged the sack instead of catching it");
		helper.assertTrue(sack.isRemoved(), "the sack is still there");
		helper.assertTrue(Creature.parts(sack).stream().allMatch(Entity::isRemoved), "the sack's model was left behind");
		helper.assertValueEqual(countSpecial(helper, EventItems.LUCKY_NUGGET), 1, "lucky nuggets from the sack");
		helper.assertTrue(count(helper, Items.GOLD_NUGGET) >= 8, "not enough gold nuggets in the sack");
		helper.assertTrue(Advancements.has(player, "catch_mimic_sack"), "no advancement for the sack");
		EventManager.stop(level);
		remove(player);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:living_meteorite", maxTicks = 100)
	public void livingMeteoriteRisesBurnsAndDropsLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject meteorSettings = new JsonObject();
		meteorSettings.addProperty("livingMeteoriteChance", 1.0);
		meteorSettings.addProperty("pickaxeChance", 1.0);
		Runnable restore = overrideEventSettings(MeteorShowerEvent.ID, meteorSettings);
		ServerPlayer player = player(helper, new BlockPos(0, 4, 0), "en_us");
		MeteorShowerEvent meteors = (MeteorShowerEvent) event(MeteorShowerEvent.ID);
		EventManager.start(level, meteors);
		buildGround(helper);
		meteors.impact(level, helper.absolutePos(new BlockPos(4, 3, 4)));

		helper.assertValueEqual(countSpecial(helper, EventItems.METEORITE_FRAGMENT), 2, "fragments from the impact");
		List<? extends MagmaCube> meteorites = level.getEntities(EntityTypeTest.forClass(MagmaCube.class), LivingMeteorite::is);
		helper.assertValueEqual(meteorites.size(), 1, "living meteorites out of the crater");
		MagmaCube meteorite = meteorites.get(0);
		helper.assertTrue(Math.abs(meteorite.getMaxHealth() - 40.0f) < 1e-3, "meteorite health should be 40, is " + meteorite.getMaxHealth());
		assertCreature(helper, meteorite, "living_meteorite");

		Zombie victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 4, 1));
		victim.hurtServer(level, level.damageSources().mobAttack(meteorite), 2.0f);
		helper.assertTrue(victim.isOnFire(), "the meteorite's hit did not set its target on fire");
		victim.discard();

		meteorite.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(countSpecial(helper, EventItems.METEORITE_FRAGMENT) > 2, "the meteorite dropped no fragments");
			helper.assertValueEqual(countSpecial(helper, EventItems.STARFALL_PICKAXE), 1, "starfall pickaxes from the meteorite");
			helper.assertTrue(Creature.parts(meteorite).isEmpty() || Creature.parts(meteorite).stream().allMatch(Entity::isRemoved),
					"the meteorite's model was left behind");
			EventManager.stop(level);
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:rare_drops", maxTicks = 100)
	public void rareDropsAppearDuringEvents(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject always = new JsonObject();
		always.addProperty("luckyNuggetChance", 1.0);
		Runnable restoreGoldRush = overrideEventSettings(GoldRushEvent.ID, always);
		JsonObject tears = new JsonObject();
		tears.addProperty("bloodyTearChance", 1.0);
		Runnable restoreBloodMoon = overrideEventSettings(BloodMoonEvent.ID, tears);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");

		EventManager.start(level, event(GoldRushEvent.ID));
		mine(helper, new BlockPos(4, 1, 4), Blocks.DIAMOND_ORE, player, new ItemStack(Items.DIAMOND_PICKAXE), Items.DIAMOND);
		helper.assertValueEqual(countSpecial(helper, EventItems.LUCKY_NUGGET), 1, "lucky nuggets from an ore");
		EventManager.stop(level);
		helper.killAllEntitiesOfClass(ItemEntity.class);

		EventManager.start(level, event(BloodMoonEvent.ID));
		Evoker evoker = helper.spawnWithNoFreeWill(EntityType.EVOKER, new BlockPos(4, 1, 4));
		evoker.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.BLOODY_TEAR), 1, "bloody tears from a blood moon kill");
			EventManager.stop(level);
			restoreBloodMoon.run();
			restoreGoldRush.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:blessed_harvest", maxTicks = 200, skyAccess = true)
	public void blessedHarvestGrowsCropsDoublesThemAndBringsASpirit(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer player = player(helper, new BlockPos(0, 2, 0), "en_us");
		BlockPos ripe = new BlockPos(7, 2, 7);
		helper.setBlock(ripe.below(), Blocks.FARMLAND);
		helper.assertValueEqual(mine(helper, ripe, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE), player,
				ItemStack.EMPTY, Items.WHEAT), 1, "wheat without the event");

		BlessedHarvestEvent harvest = (BlessedHarvestEvent) event(BlessedHarvestEvent.ID);
		EventManager.start(level, harvest);
		helper.assertValueEqual(mine(helper, ripe, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE), player,
				ItemStack.EMPTY, Items.WHEAT), 2, "wheat during the blessed harvest");

		// A golden seed used on the farmland in the middle ripens all the young wheat around it.
		plantWheat(helper, 1, 5);
		player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.GOLDEN_SEED.create("en_us", 2));
		InteractionResult used = useOn(helper, player, new BlockPos(3, 1, 3));
		helper.assertTrue(used instanceof InteractionResult.Success, "the golden seed did not work: " + used);
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				BlockState wheat = helper.getBlockState(new BlockPos(x, 2, z));
				helper.assertTrue(wheat.is(Blocks.WHEAT) && wheat.getValue(CropBlock.AGE) == CropBlock.MAX_AGE, "wheat at " + x + ", " + z + " is " + wheat);
			}
		}
		helper.assertTrue(useOn(helper, player, new BlockPos(3, 1, 3)) == InteractionResult.FAIL, "a golden seed worked on ripe wheat");

		Allay spirit = harvest.spawnSpirit(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 3, 4)), player);
		helper.assertTrue(spirit != null && HarvestSpirit.is(spirit), "no harvest spirit");
		assertCreature(helper, spirit, "harvest_spirit");
		helper.assertTrue(player.getUUID().equals(HarvestSpirit.friend(spirit)), "the spirit does not follow the player");
		// Only /kill, the void and players in creative (the mock player is one) can hurt it.
		helper.assertFalse(spirit.hurtServer(level, level.damageSources().generic(), 100.0f), "the spirit took damage");

		// Young wheat grows by itself around the player (here, as fast as it can).
		JsonObject growth = new JsonObject();
		growth.addProperty("growthRadius", 3);
		growth.addProperty("growthAttemptsPerPlayer", 300);
		Runnable restore = overrideEventSettings(BlessedHarvestEvent.ID, growth);
		plantWheat(helper, 1, 5);
		player.teleportTo(helper.absolutePos(new BlockPos(3, 2, 3)).getX() + 0.5, helper.absolutePos(new BlockPos(3, 2, 3)).getY(),
				helper.absolutePos(new BlockPos(3, 2, 3)).getZ() + 0.5);
		helper.runAfterDelay(45, () -> {
			int grown = 0;
			for (int x = 1; x <= 5; x++) {
				for (int z = 1; z <= 5; z++) {
					BlockState wheat = helper.getBlockState(new BlockPos(x, 2, z));
					if (wheat.is(Blocks.WHEAT) && wheat.getValue(CropBlock.AGE) > 0) {
						grown++;
					}
				}
			}
			helper.assertTrue(grown > 0, "no wheat grew by itself");
			EventManager.stop(level);
			helper.assertTrue(spirit.isRemoved(), "the spirit stayed after the harvest");
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:merchant_caravan", maxTicks = 100)
	public void merchantCaravanSellsRareGoodsAndLeaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "en_us");
		MerchantCaravanEvent caravan = (MerchantCaravanEvent) event(MerchantCaravanEvent.ID);
		EventManager.start(level, caravan);
		WanderingTrader merchant = caravan.spawnCaravan(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 4, 4)), "es_es");

		helper.assertTrue(merchant != null && NomadMerchant.is(merchant), "no merchant");
		assertCreature(helper, merchant, "nomad_merchant");
		helper.assertTrue(merchant.isNoAi(), "the merchant would wander off (and drink milk)");
		MerchantOffers offers = merchant.getOffers();
		helper.assertValueEqual(offers.size(), 10, "offers");
		helper.assertTrue(offers.stream().filter(offer -> EventItems.identify(offer.getResult()) != null).count() == 6, "the merchant does not sell six event items");
		// Relics cost event seals, and only real seals pay for them.
		MerchantOffer relic = offers.stream().filter(offer -> offer.getBaseCostA().is(EventItems.EVENT_SEAL.base())).findFirst().orElseThrow();
		helper.assertTrue(relic.satisfiedBy(EventItems.EVENT_SEAL.create("en_us", 64), ItemStack.EMPTY), "seals do not pay for a relic");
		helper.assertFalse(relic.satisfiedBy(new ItemStack(EventItems.EVENT_SEAL.base(), 64), ItemStack.EMPTY), "plain pottery sherds pay for a relic");
		List<TraderLlama> llamas = MerchantCaravanEvent.llamas(EventManager.active(level));
		helper.assertFalse(llamas.isEmpty(), "the caravan has no llamas");
		helper.assertTrue(llamas.stream().allMatch(llama -> llama.getLeashHolder() == merchant), "a llama is not leashed to the merchant");
		// Only /kill, the void and players in creative (the mock player is one) can hurt it.
		helper.assertFalse(merchant.hurtServer(level, level.damageSources().generic(), 100.0f), "the merchant took damage");

		// A trade, as the trading screen makes it (notifyTrade counts the use).
		merchant.setTradingPlayer(player);
		merchant.notifyTrade(offers.get(0));
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(Advancements.has(player, "trade_with_caravan"), "no advancement for the trade");
			EventManager.stop(level);
			helper.assertTrue(merchant.isRemoved(), "the merchant stayed after the caravan");
			helper.assertTrue(llamas.stream().allMatch(Entity::isRemoved), "a llama stayed after the caravan");
			helper.assertTrue(Creature.parts(merchant).stream().allMatch(Entity::isRemoved), "the merchant's model was left behind");
			helper.assertValueEqual(count(helper, Items.LEAD), 0, "leads dropped by the leaving llamas");
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:horde", maxTicks = 3000, skyAccess = true)
	public void hordeWavesComeOneAfterAnotherUntilVictory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		boolean spawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
		level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
		helper.getLevel().setDayTime(18000);
		JsonObject quick = new JsonObject();
		quick.addProperty("mobsPerPlayer", 1);
		quick.addProperty("extraMobsPerWave", 0);
		quick.addProperty("waveBreak", 1);
		quick.addProperty("warlord", false);
		Runnable restore = overrideEventSettings(HordeEvent.ID, quick);
		ServerPlayer player = player(helper, new BlockPos(4, 1, 4), "es_es");
		List<ChunkPos> forced = forceArea(level, player.chunkPosition(), 2);

		boolean[] started = {false};
		int[] lastWave = {0};
		helper.onEachTick(() -> {
			if (!started[0]) {
				if (areaReady(level, forced, player.getBlockY())) {
					EventManager.start(level, event(HordeEvent.ID));
					started[0] = true;
				}
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null) {
				lastWave[0] = HordeEvent.wave(active);
				// The player fights back: every monster of the wave falls as soon as it comes.
				for (Mob mob : HordeEvent.waveMobs(active)) {
					helper.assertTrue(mob.getTarget() == null || mob.getTarget() == player, "a monster went after something else");
					mob.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
				}
				return;
			}
			helper.assertValueEqual(lastWave[0], HordeEvent.WAVES, "waves before the end");
			helper.assertTrue(Advancements.has(player, "survive_horde"), "no advancement for beating the horde");
			restore.run();
			unforceArea(level, forced);
			level.getGameRules().set(GameRules.SPAWN_MOBS, spawnMobs, level.getServer());
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:warlord", maxTicks = 100)
	public void hordeWarlordDropsItsAxeWhoseWarCryHelpsAllies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject axe = new JsonObject();
		axe.addProperty("axeChance", 1.0);
		Runnable restore = overrideEventSettings(HordeEvent.ID, axe);
		ServerPlayer player = player(helper, new BlockPos(1, 1, 1), "en_us");
		HordeEvent horde = (HordeEvent) event(HordeEvent.ID);
		EventManager.start(level, horde);
		Vindicator warlord = horde.spawnWarlord(level, EventManager.active(level), helper.absolutePos(new BlockPos(4, 1, 4)), "es_es");

		helper.assertTrue(warlord != null && HordeWarlord.is(warlord), "no warlord");
		helper.assertTrue(Math.abs(warlord.getMaxHealth() - 150.0f) < 1e-3, "warlord health should be 150, is " + warlord.getMaxHealth());
		helper.assertTrue(EventItems.WARLORD_AXE.is(warlord.getMainHandItem()), "the warlord does not hold its axe");
		assertCreature(helper, warlord, "horde_warlord");
		ItemStack weapon = EventItems.WARLORD_AXE.create("en_us", 1);

		warlord.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.WARLORD_AXE), 1, "warlord's axes dropped");
			helper.assertTrue(count(helper, Items.EMERALD) >= 4, "not enough emeralds from the warlord");
			helper.assertTrue(Advancements.has(player, "defeat_horde_warlord"), "no advancement for the warlord");
			helper.assertTrue(Creature.parts(warlord).stream().allMatch(Entity::isRemoved), "the warlord's model was left behind");
			EventManager.stop(level);

			// Stripping a log with the axe must not use up its war cry.
			player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
			BlockPos log = new BlockPos(6, 1, 1);
			helper.setBlock(log, Blocks.OAK_LOG);
			BlockPos logPos = helper.absolutePos(log);
			weapon.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(logPos), Direction.UP, logPos, false)));
			helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, log);
			// Mock players skip the player tick that counts cooldowns down: a few ticks by hand.
			tickCooldowns(player, 3);
			helper.assertFalse(player.getCooldowns().isOnCooldown(weapon), "stripping a log used up the war cry");
		});
		helper.runAfterDelay(8, () -> {
			// The war cry: Strength for the player, Weakness for the monster next to them, then a cooldown.
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
			InteractionResult cry = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(cry instanceof InteractionResult.Success, "no war cry: " + cry);
			helper.assertTrue(player.hasEffect(MobEffects.STRENGTH), "no Strength after the war cry");
			helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "the zombie was not weakened");
			tickCooldowns(player, 3);
			helper.assertTrue(player.getCooldowns().isOnCooldown(weapon), "the axe has no cooldown");
			helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
					"a second war cry during the cooldown");
			zombie.discard();
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:summon", maxTicks = 100)
	public void creatureEggsAndCommandSummonCreaturesThatKeepToThemselves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "es_es");
		ItemStack egg = EventItems.egg(BloodMoonLord.KIND, "es_es", 2);
		helper.assertTrue(egg.is(Items.WITHER_SKELETON_SPAWN_EGG) && EventItems.creatureOf(egg) == BloodMoonLord.KIND, "not a lord egg: " + egg);
		player.setItemInHand(InteractionHand.MAIN_HAND, egg);
		helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
				"the egg worked in the air, where a vanilla egg would spawn a plain wither skeleton");
		InteractionResult used = useOn(helper, player, new BlockPos(4, 3, 4));
		helper.assertTrue(used instanceof InteractionResult.Success, "the egg did not work: " + used);
		List<? extends WitherSkeleton> lords = level.getEntities(EntityTypeTest.forClass(WitherSkeleton.class), BloodMoonLord::is);
		helper.assertValueEqual(lords.size(), 1, "lords from the egg");
		WitherSkeleton lord = lords.get(0);
		helper.assertTrue(Creature.isSummoned(lord), "the lord is not marked as summoned");
		assertCreature(helper, lord, "blood_moon_lord", "blood_moon_lord_crown");

		BlockPos at = helper.absolutePos(new BlockPos(6, 5, 6));
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),
				"worldevents summon storm_spirit " + at.getX() + " " + at.getY() + " " + at.getZ());
		List<? extends Vex> spirits = level.getEntities(EntityTypeTest.forClass(Vex.class), StormSpirit::is);
		helper.assertValueEqual(spirits.size(), 1, "storm spirits from the command");
		Vex spirit = spirits.get(0);
		helper.assertTrue(Creature.isSummoned(spirit), "the spirit is not marked as summoned");

		// Outside its event, nothing takes the lord away, and it has its own boss bar (updated once a second).
		helper.runAfterDelay(25, () -> {
			helper.assertFalse(lord.isRemoved() || spirit.isRemoved(), "a summoned creature was removed outside its event");
			Component title = Creature.bossBarTitle(lord, player);
			helper.assertTrue(title != null && title.getString().equals("Señor de la luna de sangre"), "boss bar: " + title);
			EventManager.start(level, event(BloodMoonEvent.ID));
			helper.assertFalse(lord.getAttribute(Attributes.MAX_HEALTH).hasModifier(BloodMoonEvent.HEALTH_MODIFIER),
					"the blood moon buffed the summoned lord");
			EventManager.stop(level);
			helper.assertFalse(lord.isRemoved(), "the blood moon took the summoned lord away when it ended");
			Creature.remove(lord);
			Creature.remove(spirit);
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:ranking", maxTicks = 100)
	public void bestPlayersEarnSealsWhenAnEventEnds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		ServerPlayer best = player(helper, new BlockPos(1, 1, 1), "es_es");
		ServerPlayer second = player(helper, new BlockPos(2, 1, 2), "en_us");
		ServerPlayer idle = player(helper, new BlockPos(3, 1, 3), "en_us");
		EventManager.start(level, event(GoldRushEvent.ID));
		EventManager.score(best, GoldRushEvent.ID, 7);
		EventManager.score(second, GoldRushEvent.ID, 3);
		EventManager.score(idle, GoldRushEvent.ID, 1);
		EventManager.score(idle, BloodMoonEvent.ID, 50);

		List<EventScores.Entry> ranking = EventScores.ranking(EventManager.active(level));
		helper.assertValueEqual(ranking.size(), 3, "players in the ranking");
		helper.assertTrue(ranking.get(0).player().equals(best.getUUID()) && ranking.get(0).points() == 7, "unexpected winner: " + ranking.get(0));
		helper.assertValueEqual(ranking.get(2).points(), 1, "points for another event should not count");

		EventManager.stop(level);
		helper.assertValueEqual(seals(best), 3, "seals for the winner");
		helper.assertValueEqual(seals(second), 1, "seals for taking part");
		helper.assertValueEqual(seals(idle), 0, "seals for barely taking part");
		remove(best);
		remove(second);
		remove(idle);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:treasure_hunt", maxTicks = 100)
	public void treasureStaysShutUntilTheCaptainFalls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("cutlassChance", 1.0);
		Runnable restore = overrideEventSettings(TreasureHuntEvent.ID, settings);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "es_es");
		TreasureHuntEvent hunt = (TreasureHuntEvent) event(TreasureHuntEvent.ID);
		EventManager.start(level, hunt);
		ActiveEvent active = EventManager.active(level);
		BlockPos chest = new BlockPos(4, 4, 4);
		hunt.placeTreasure(level, active, helper.absolutePos(chest), "es_es");
		helper.assertBlockPresent(Blocks.CHEST, chest);

		// Touching the chest wakes the captain, and it stays shut.
		helper.assertTrue(useOn(helper, player, chest) == InteractionResult.FAIL, "the guarded chest opened");
		Vindicator captain = TreasureHuntEvent.captain(active);
		helper.assertTrue(captain != null && SpectralCaptain.is(captain), "no captain");
		assertCreature(helper, captain, "spectral_captain");
		helper.assertTrue(EventItems.CAPTAINS_CUTLASS.is(captain.getMainHandItem()), "the captain does not hold its cutlass");
		helper.assertTrue(Math.abs(captain.getMaxHealth() - 120.0f) < 1e-3, "captain health should be 120, is " + captain.getMaxHealth());

		// A chest placed next to it would make a double chest: it stays shut too.
		BlockPos chestAt = helper.absolutePos(chest);
		BlockState left = level.getBlockState(chestAt).setValue(ChestBlock.TYPE, ChestType.LEFT);
		BlockPos neighbour = chestAt.relative(ChestBlock.getConnectedDirection(left));
		level.setBlock(chestAt, left, Block.UPDATE_ALL);
		level.setBlock(neighbour, left.setValue(ChestBlock.TYPE, ChestType.RIGHT), Block.UPDATE_ALL);
		helper.assertTrue(useOnAbsolute(helper, player, neighbour) == InteractionResult.FAIL, "the guarded chest opened as a double chest");
		level.setBlock(neighbour, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

		captain.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.CAPTAINS_CUTLASS), 1, "cutlasses from the captain");
			helper.assertTrue(Advancements.has(player, "defeat_spectral_captain"), "no advancement for the captain");
			InteractionResult opened = useOn(helper, player, chest);
			helper.assertTrue(opened == InteractionResult.PASS, "the free chest did not open: " + opened);
			helper.assertTrue(Advancements.has(player, "find_treasure"), "no advancement for the treasure");
		});
		// The hunt ends within a second, and the chest stays with its loot for whoever found it.
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(EventManager.active(level) == null, "the hunt did not end when the treasure was found");
			helper.assertBlockPresent(Blocks.CHEST, chest);
			boolean loot = false;
			if (level.getBlockEntity(helper.absolutePos(chest)) instanceof ChestBlockEntity treasure) {
				for (int slot = 0; slot < treasure.getContainerSize(); slot++) {
					loot |= EventItems.EVENT_SEAL.is(treasure.getItem(slot));
				}
			}
			helper.assertTrue(loot, "no seals in the chest");
			helper.assertTrue(seals(player) >= 3, "no seals for the winner of the hunt");
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:treasure_left", maxTicks = 100)
	public void unclaimedTreasureLeavesWithTheEventAndCannotBeBroken(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "en_us");
		TreasureHuntEvent hunt = (TreasureHuntEvent) event(TreasureHuntEvent.ID);
		EventManager.start(level, hunt);
		BlockPos chest = new BlockPos(5, 4, 5);
		BlockPos at = helper.absolutePos(chest);
		hunt.placeTreasure(level, EventManager.active(level), at, "en_us");
		helper.assertFalse(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, at, level.getBlockState(at), level.getBlockEntity(at)),
				"the treasure could be broken during the hunt");

		// The cutlass: right-click to lunge forward, then a cooldown.
		ItemStack cutlass = EventItems.CAPTAINS_CUTLASS.create("en_us", 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, cutlass);
		player.setYRot(0.0f);
		player.setXRot(0.0f);
		helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) instanceof InteractionResult.Success, "no lunge");
		helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 1.0, "the lunge did not push the player: " + player.getDeltaMovement());
		helper.assertTrue(player.getCooldowns().isOnCooldown(cutlass), "the cutlass has no cooldown");

		EventManager.stop(level);
		helper.assertBlockPresent(Blocks.AIR, chest);
		helper.assertValueEqual(helper.getEntities(EntityType.ITEM).size(), 0, "items spilled by the chest");
		remove(player);
		helper.succeed();
	}

	@GameTest(environment = "worldevents-test:eruption", maxTicks = 100)
	public void eruptionBurnsWithoutTouchingBlocksAndTheColossusDropsItsHammer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("hammerChance", 1.0);
		Runnable restore = overrideEventSettings(EruptionEvent.ID, settings);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "en_us");
		EruptionEvent eruption = (EruptionEvent) event(EruptionEvent.ID);
		helper.assertFalse(eruption.isAvailable(level), "eruptions are for the Nether");
		helper.assertTrue(eruption.isAvailable(level.getServer().getLevel(Level.NETHER)), "no eruptions in the Nether");
		EventManager.start(level, eruption);

		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 4, 4));
		EruptionEvent.erupt(level, helper.absolutePos(new BlockPos(4, 4, 4)), 6.0f);
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth() && zombie.isOnFire(), "the geyser did not burn the zombie");
		helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(4, 3, 4));
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 4, 4));
		zombie.discard();

		Hoglin colossus = eruption.spawnColossus(level, EventManager.active(level), helper.absolutePos(new BlockPos(5, 4, 5)), "en_us");
		helper.assertTrue(colossus != null && MagmaColossus.is(colossus), "no colossus");
		assertCreature(helper, colossus, "magma_colossus");
		helper.assertFalse(colossus.isBaby(), "a baby colossus");
		helper.assertTrue(Math.abs(colossus.getMaxHealth() - 250.0f) < 1e-3, "colossus health should be 250, is " + colossus.getMaxHealth());

		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 4, 5));
		MagmaColossus.slam(level, colossus);
		helper.assertTrue(pig.isOnFire(), "the slam did not burn the pig");
		helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(3, 3, 5));
		pig.discard();

		colossus.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.COLOSSUS_HAMMER), 1, "hammers from the colossus");
			helper.assertTrue(countSpecial(helper, EventItems.MAGMA_HEART) >= 1, "no magma heart from the colossus");
			helper.assertTrue(Advancements.has(player, "defeat_magma_colossus"), "no advancement for the colossus");
			helper.assertTrue(Creature.parts(colossus).stream().allMatch(Entity::isRemoved), "the colossus's model was left behind");
			EventManager.stop(level);

			player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.COLOSSUS_HAMMER.create("en_us", 1));
			Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 4, 2));
			target.hurtServer(level, level.damageSources().playerAttack(player), 1.0f);
			helper.assertTrue(target.isOnFire(), "the hammer did not set its target on fire");
			target.discard();
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	@GameTest(environment = "worldevents-test:void_storm", maxTicks = 100)
	public void voidStalkerBlinksBehindItsPreyAndDropsPearlsThatBlink(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		reset(level);
		JsonObject settings = new JsonObject();
		settings.addProperty("pearlChance", 1.0);
		Runnable restore = overrideEventSettings(VoidStormEvent.ID, settings);
		buildGround(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 4, 1), "en_us");
		VoidStormEvent storm = (VoidStormEvent) event(VoidStormEvent.ID);
		helper.assertFalse(storm.isAvailable(level), "void storms are for the End");
		helper.assertTrue(storm.isAvailable(level.getServer().getLevel(Level.END)), "no void storms in the End");
		EventManager.start(level, storm);

		Vindicator stalker = storm.spawnStalker(level, EventManager.active(level), helper.absolutePos(new BlockPos(7, 4, 7)), "en_us");
		helper.assertTrue(stalker != null && VoidStalker.is(stalker), "no stalker");
		assertCreature(helper, stalker, "void_stalker");
		helper.assertTrue(stalker.getMainHandItem().isEmpty(), "the stalker holds an item that would float next to it");

		Zombie prey = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 4, 5));
		prey.setYRot(0.0f);
		helper.assertTrue(VoidStalker.blinkBehind(level, stalker, prey), "the stalker found nowhere to blink to");
		helper.assertTrue(stalker.distanceTo(prey) < 4.0f, "the stalker did not blink next to its prey");
		helper.assertTrue(stalker.getZ() < prey.getZ(), "the stalker did not blink behind its prey");
		helper.assertTrue(Creature.parts(stalker).stream().allMatch(part -> part.getVehicle() == stalker), "the model stayed behind");
		prey.hurtServer(level, level.damageSources().mobAttack(stalker), 1.0f);
		helper.assertTrue(prey.hasEffect(MobEffects.BLINDNESS), "the stalker's touch did not blind");
		prey.discard();

		stalker.hurtServer(level, level.damageSources().playerAttack(player), 10000.0f);
		helper.runAfterDelay(3, () -> {
			helper.assertValueEqual(countSpecial(helper, EventItems.UNSTABLE_PEARL), 1, "pearls from the stalker");
			helper.assertTrue(Advancements.has(player, "defeat_void_stalker"), "no advancement for the stalker");
			EventManager.stop(level);

			// The pearl: straight ahead, stopping short of the wall.
			helper.killAllEntitiesOfClass(ItemEntity.class);
			for (int y = 4; y < 8; y++) {
				helper.setBlock(new BlockPos(1, y, 7), Blocks.STONE);
			}
			player.setYRot(0.0f);
			player.setXRot(0.0f);
			double from = player.getZ();
			player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.UNSTABLE_PEARL.create("en_us", 2));
			InteractionResult used = UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
			helper.assertTrue(used instanceof InteractionResult.Success, "the pearl did not work: " + used);
			helper.assertTrue(player.getZ() > from + 2.0, "the pearl did not move the player");
			helper.assertTrue(player.getZ() < helper.absolutePos(new BlockPos(1, 4, 7)).getZ(), "the pearl went through the wall");
			helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "no slow falling after the blink");
			restore.run();
			remove(player);
			helper.succeed();
		});
	}

	private static int seals(ServerPlayer player) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (EventItems.EVENT_SEAL.is(stack)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static WorldEvent event(String id) {
		return EventRegistry.get(id);
	}

	/**
	 * Leaves the dimension clean even if an earlier test failed halfway, including its mock player: they all share
	 * one name, so a leftover one would get the items given by name.
	 */
	private static void reset(ServerLevel level) {
		EventManager.stop(level);
		WorldEventsData.get(level).setPlan(null);
		for (ServerPlayer leftover : new ArrayList<>(level.getServer().getPlayerList().getPlayers())) {
			if (leftover.getScoreboardName().equals(MOCK_PLAYER_NAME)) {
				remove(leftover);
			}
		}
	}

	// The only helper that puts a real ServerPlayer in the level, which events need to see.
	@SuppressWarnings("removal")
	private static ServerPlayer player(GameTestHelper helper, BlockPos relative, String language) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(relative);
		player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		setLanguage(player, language);
		return player;
	}

	private static void setLanguage(ServerPlayer player, String language) {
		player.updateOptions(new ClientInformation(language, 8, ChatVisiblity.FULL, true, 0x7F, HumanoidArm.RIGHT, false, true, ParticleStatus.ALL));
	}

	private static void remove(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}

	private static void assertTitle(GameTestHelper helper, ServerPlayer player, String expectedStart) {
		Component title = EventManager.bossBarTitle(player);
		helper.assertTrue(title != null && title.getString().startsWith(expectedStart),
				"boss bar for " + player.clientInformation().language() + " was " + (title == null ? null : title.getString()));
	}

	/** Breaks a fresh block as if a player mined it with {@code tool}, and counts the matching drops. */
	private static int mine(GameTestHelper helper, BlockPos relative, Block block, ServerPlayer player, ItemStack tool, Item expected) {
		return mine(helper, relative, block.defaultBlockState(), player, tool, expected);
	}

	private static int mine(GameTestHelper helper, BlockPos relative, BlockState block, ServerPlayer player, ItemStack tool, Item expected) {
		helper.killAllEntitiesOfClass(ItemEntity.class);
		helper.setBlock(relative, block);
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(relative);
		BlockState state = level.getBlockState(pos);
		Block.dropResources(state, level, pos, null, player, tool);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		return count(helper, expected);
	}

	private static void tickCooldowns(ServerPlayer player, int ticks) {
		for (int i = 0; i < ticks; i++) {
			player.getCooldowns().tick();
		}
	}

	/** Farmland with young wheat on it, from {@code from} to {@code to} on both axes. */
	private static void plantWheat(GameTestHelper helper, int from, int to) {
		for (int x = from; x <= to; x++) {
			for (int z = from; z <= to; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WHEAT);
			}
		}
	}

	/** Right-clicks the top of a block with the item in the player's main hand. */
	private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
		return useOnAbsolute(helper, player, helper.absolutePos(relative));
	}

	private static InteractionResult useOnAbsolute(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
	}

	private static int count(GameTestHelper helper, Item item) {
		int total = 0;
		for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
			if (entity.getItem().is(item)) {
				total += entity.getItem().getCount();
			}
		}
		return total;
	}

	/** The mob is invisible and wears exactly these body parts (by model id), in this order. */
	private static void assertCreature(GameTestHelper helper, Mob mob, String... models) {
		helper.assertTrue(mob.hasEffect(MobEffects.INVISIBILITY), "the creature's vanilla mob is visible");
		List<Display.ItemDisplay> parts = Creature.parts(mob);
		helper.assertValueEqual(parts.size(), models.length, "body parts");
		for (int i = 0; i < models.length; i++) {
			CustomModelData data = parts.get(i).getItemStack().get(DataComponents.CUSTOM_MODEL_DATA);
			helper.assertTrue(data != null && data.strings().equals(List.of("worldevents:" + models[i])),
					"part " + i + " should show " + models[i] + ", shows " + data);
		}
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

	/** Three layers of dirt with grass on top, filling the test area up to y = 3. */
	private static void buildGround(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				for (int y = 0; y < 3; y++) {
					helper.setBlock(x, y, z, Blocks.DIRT);
				}
				helper.setBlock(x, 3, z, Blocks.GRASS_BLOCK);
			}
		}
	}

	/**
	 * Keeps the chunks around {@code center} loaded. Events only act in chunks that already tick entities, and the
	 * test server runs ticks faster than it loads chunks.
	 */
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

	private static boolean areaReady(ServerLevel level, List<ChunkPos> chunks, int y) {
		for (ChunkPos chunk : chunks) {
			if (!level.isPositionEntityTicking(new BlockPos(chunk.getMiddleBlockX(), y, chunk.getMiddleBlockZ()))) {
				return false;
			}
		}
		return true;
	}

	private static void unforceArea(ServerLevel level, List<ChunkPos> chunks) {
		for (ChunkPos chunk : chunks) {
			level.setChunkForced(chunk.x, chunk.z, false);
		}
	}

	/** Merges {@code values} into one event's section of the config file and reloads; the result undoes it. */
	private static Runnable overrideEventSettings(String eventId, JsonObject values) {
		return overrideSettings(eventId, values);
	}

	/** Merges {@code values} into the top of the config file and reloads; the result undoes it. */
	private static Runnable overrideGlobalSettings(JsonObject values) {
		return overrideSettings(null, values);
	}

	private static Runnable overrideSettings(String eventId, JsonObject values) {
		Path path = ModConfig.path();
		try {
			String original = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject json = JsonParser.parseString(original).getAsJsonObject();
			JsonObject section = eventId == null ? json : json.getAsJsonObject("events").getAsJsonObject(eventId);
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

	/** Runs {@code action} with {@code blockDamage: false} in the config file, then restores the file. */
	private static void withBlockDamageOff(Runnable action) {
		Path path = ModConfig.path();
		try {
			String original = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject json = JsonParser.parseString(original).getAsJsonObject();
			json.addProperty("blockDamage", false);
			Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
			try {
				ModConfig.load();
				action.run();
			} finally {
				Files.writeString(path, original, StandardCharsets.UTF_8);
				ModConfig.load();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
