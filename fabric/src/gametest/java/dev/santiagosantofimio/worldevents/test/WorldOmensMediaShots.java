package dev.santiagosantofimio.worldevents.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
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
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

/**
 * The pictures for the mod's pages, in real terrain at 1920x1080, without the HUD unless a shot needs it. Part of
 * {@code ./gradlew :fabric:runClientGameTest}; they land in {@code build/run/clientGameTest/screenshots}.
 */
public class WorldOmensMediaShots implements FabricClientGameTest {
	private static final String LANGUAGE = "en_us";
	private static final String SEED = "worldomens";
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;
	private static final int CLEARING_SIDE = 34;
	private static final int CLEARING_BACK = 8;
	private static final int CLEARING_FRONT = 44;
	private static final int CLEARING_HEIGHT = 28;
	private static final int NOON = 6000;
	private static final int DUSK = 12800;
	private static final int MIDNIGHT = 18000;

	@FunctionalInterface
	private interface Stage {
		void set(ServerLevel level, ServerPlayer player, BlockPos ground);
	}

	private int nextSpot;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> client.options.chatVisibility().set(ChatVisiblity.HIDDEN));
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(SEED)).create()) {
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("difficulty easy");
			context.waitTicks(40);

			context.getInput().pressKey(InputConstants.KEY_F1);
			shoot(context, world, "01_blood_moon", Level.OVERWORLD, MIDNIGHT, 40, 3, 6.0f, (level, player, ground) -> {
				BloodMoonEvent event = (BloodMoonEvent) EventRegistry.get(BloodMoonEvent.ID);
				ActiveEvent active = start(level, event);
				pose(event.spawnLord(level, active, at(level, ground, 0, 7), LANGUAGE));
				pose(event.spawnEye(level, active, at(level, ground, -5, 7).above(3), LANGUAGE));
				pose(event.spawnEye(level, active, at(level, ground, 5, 7).above(4), LANGUAGE));
				crowd(level, ground, EntityTypes.ZOMBIE, -6, 10);
				crowd(level, ground, EntityTypes.SKELETON, 6, 10);
				crowd(level, ground, EntityTypes.ZOMBIE, -3, 11);
				crowd(level, ground, EntityTypes.ZOMBIE, 3, 11);
			});
			shoot(context, world, "02_horde", Level.OVERWORLD, MIDNIGHT, 40, 4, 10.0f, (level, player, ground) -> {
				HordeEvent event = (HordeEvent) EventRegistry.get(HordeEvent.ID);
				ActiveEvent active = start(level, event);
				pose(event.spawnWarlord(level, active, at(level, ground, 0, 7), LANGUAGE));
				for (int side = -9; side <= 9; side += 2) {
					crowd(level, ground, side % 4 == 1 ? EntityTypes.SKELETON : EntityTypes.ZOMBIE, side, 10 + Math.abs(side) / 3);
				}
			});
			shoot(context, world, "03_meteor_shower", Level.OVERWORLD, MIDNIGHT, 80, 6, 26.0f, (level, player, ground) -> {
				MeteorShowerEvent event = (MeteorShowerEvent) EventRegistry.get(MeteorShowerEvent.ID);
				start(level, event);
				event.impact(level, at(level, ground, 0, 10).below());
				pose(LivingMeteorite.spawn(level, at(level, ground, -5, 7), LANGUAGE));
				pose(LivingMeteorite.spawn(level, at(level, ground, 5, 8), LANGUAGE));
			});
			shoot(context, world, "04_treasure_hunt", Level.OVERWORLD, DUSK, 12, 3, 10.0f, (level, player, ground) -> {
				TreasureHuntEvent event = (TreasureHuntEvent) EventRegistry.get(TreasureHuntEvent.ID);
				ActiveEvent active = start(level, event);
				event.placeTreasure(level, active, at(level, ground, 2, 6), LANGUAGE);
				pose(SpectralCaptain.spawn(level, at(level, ground, -2, 8), LANGUAGE, 120.0));
			});
			shoot(context, world, "05_thunderstorm", Level.OVERWORLD, DUSK, 120, 3, -2.0f, (level, player, ground) -> {
				ThunderstormEvent event = (ThunderstormEvent) EventRegistry.get(ThunderstormEvent.ID);
				ActiveEvent active = start(level, event);
				pose(event.spawnSpirit(level, active, at(level, ground, -3, 6).above(2), LANGUAGE));
				pose(event.spawnSpirit(level, active, at(level, ground, 3, 7).above(3), LANGUAGE));
				pose(event.spawnSpirit(level, active, at(level, ground, 0, 10).above(4), LANGUAGE));
			}, (level, player, ground) -> {
				for (int side : new int[]{-14, 11}) {
					LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
					if (bolt != null) {
						bolt.setVisualOnly(true);
						bolt.snapTo(Vec3.atBottomCenterOf(at(level, ground, side, 26)));
						level.addFreshEntity(bolt);
					}
				}
			});
			shoot(context, world, "06_gold_rush", Level.OVERWORLD, NOON, 40, 3, 12.0f, (level, player, ground) -> {
				GoldRushEvent event = (GoldRushEvent) EventRegistry.get(GoldRushEvent.ID);
				ActiveEvent active = start(level, event);
				for (int side = -9; side <= 9; side++) {
					for (int height = 0; height <= 6 - Math.abs(side) / 2; height++) {
						boolean gold = (side * 7 + height * 5) % 4 == 0;
						level.setBlockAndUpdate(at(level, ground, side, 9).above(height), gold ? Blocks.GOLD_ORE.defaultBlockState() : Blocks.STONE.defaultBlockState());
					}
				}
				pose(event.spawnSack(level, active, at(level, ground, -2, 4), LANGUAGE));
				pose(event.spawnSack(level, active, at(level, ground, 0, 5), LANGUAGE));
				pose(event.spawnSack(level, active, at(level, ground, 2, 4), LANGUAGE));
			});
			shoot(context, world, "07_blessed_harvest", Level.OVERWORLD, NOON, 40, 3, 12.0f, (level, player, ground) -> {
				BlessedHarvestEvent event = (BlessedHarvestEvent) EventRegistry.get(BlessedHarvestEvent.ID);
				ActiveEvent active = start(level, event);
				for (int side = -7; side <= 7; side++) {
					for (int distance = 1; distance <= 13; distance++) {
						BlockPos crop = at(level, ground, side, distance);
						level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
						level.setBlockAndUpdate(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(((CropBlock) Blocks.WHEAT).getMaxAge()));
					}
				}
				pose(event.spawnSpirit(level, active, at(level, ground, 0, 5).above(2), player));
			});
			shoot(context, world, "08_merchant_caravan", Level.OVERWORLD, NOON, 60, 3, 16.0f, (level, player, ground) -> {
				MerchantCaravanEvent event = (MerchantCaravanEvent) EventRegistry.get(MerchantCaravanEvent.ID);
				pose(event.spawnCaravan(level, start(level, event), at(level, ground, 0, 5), LANGUAGE));
			});
			shoot(context, world, "09_eruption", Level.NETHER, MIDNIGHT, 160, 3, 12.0f, (level, player, ground) -> {
				EruptionEvent event = (EruptionEvent) EventRegistry.get(EruptionEvent.ID);
				pose(event.spawnColossus(level, start(level, event), at(level, ground, 0, 9), LANGUAGE));
				for (int[] pool : new int[][]{{-9, 7}, {9, 8}, {-5, 13}, {6, 14}, {-14, 12}, {14, 13}}) {
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							level.setBlockAndUpdate(at(level, ground, pool[0] + x, pool[1] + z).below(), Blocks.LAVA.defaultBlockState());
						}
					}
				}
			});
			shoot(context, world, "10_void_storm", Level.END, MIDNIGHT, 60, 3, 6.0f, (level, player, ground) -> {
				VoidStormEvent event = (VoidStormEvent) EventRegistry.get(VoidStormEvent.ID);
				ActiveEvent active = start(level, event);
				pose(event.spawnStalker(level, active, at(level, ground, 0, 8), LANGUAGE));
				pose(event.spawnStalker(level, active, at(level, ground, -4, 10), LANGUAGE));
				pose(event.spawnStalker(level, active, at(level, ground, 4, 10), LANGUAGE));
			});
			shoot(context, world, "11_creatures", Level.OVERWORLD, NOON, 60, 4, 16.0f, (level, player, ground) -> {
				List<Creature.Kind> back = List.of(BloodMoonLord.KIND, HordeWarlord.KIND, SpectralCaptain.KIND, MagmaColossus.KIND, NomadMerchant.KIND);
				List<Creature.Kind> front = List.of(BloodEye.KIND, MimicSack.KIND, LivingMeteorite.KIND, StormSpirit.KIND, HarvestSpirit.KIND, VoidStalker.KIND);
				for (int i = 0; i < back.size(); i++) {
					pose(Creature.summon(back.get(i), level, at(level, ground, -10 + i * 5, 12), LANGUAGE, player));
				}
				for (int i = 0; i < front.size(); i++) {
					pose(Creature.summon(front.get(i), level, at(level, ground, -8 + i * 3, 7), LANGUAGE, player));
				}
			});

			// With the HUD: the event's name and time left, and the creative tab. The HUD only fills a picture of the
			// window's real size.
			context.getInput().pressKey(InputConstants.KEY_F1);
			context.getInput().resizeWindow(WIDTH, HEIGHT);
			context.waitTicks(5);
			for (String item : List.of("blood_scythe", "starfall_pickaxe", "warlord_axe", "bottled_lightning", "bloody_tear", "golden_seed")) {
				world.getServer().runCommand("worldevents give @a " + item);
			}
			shoot(context, world, "12_blood_moon_hud", Level.OVERWORLD, MIDNIGHT, 40, 3, 6.0f, (level, player, ground) -> {
				BloodMoonEvent event = (BloodMoonEvent) EventRegistry.get(BloodMoonEvent.ID);
				ActiveEvent active = start(level, event);
				pose(event.spawnLord(level, active, at(level, ground, 0, 9), LANGUAGE));
			});
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitForScreen(CreativeModeInventoryScreen.class);
			context.runOnClient(client -> ((FabricCreativeModeInventoryScreen) client.gui.screen())
					.setSelectedTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorldEventsFabricClient.TAB)));
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("13_creative_tab").withSize(WIDTH, HEIGHT));
			context.setScreen(() -> null);
		}
	}

	private void shoot(ClientGameTestContext context, TestSingleplayerContext world, String name, ResourceKey<Level> dimension, int time, int settleTicks,
			int cameraHeight, float pitch, Stage stage) {
		shoot(context, world, name, dimension, time, settleTicks, cameraHeight, pitch, stage, (level, player, ground) -> {
		});
	}

	/**
	 * Clears the last scene, opens a clearing on fresh ground, sets the scene with the camera looking south and takes
	 * the picture. {@code lastMoment} is for what only lasts a few ticks.
	 */
	private void shoot(ClientGameTestContext context, TestSingleplayerContext world, String name, ResourceKey<Level> dimension, int time, int settleTicks,
			int cameraHeight, float pitch, Stage stage, Stage lastMoment) {
		int spot = nextSpot++;
		BlockPos[] ground = new BlockPos[1];
		world.getServer().runCommand("kill @e[type=!minecraft:player]");
		world.getServer().runCommand("time set " + time);
		world.getServer().runOnServer(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				EventManager.stop(level);
			}
			server.setWeatherParameters(100000, 0, false, false);
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.getLevel(dimension);
			ground[0] = clearing(level, spot);
			player.teleportTo(level, ground[0].getX() + 0.5, ground[0].getY() + cameraHeight, ground[0].getZ() + 0.5, Set.of(), 0.0f, pitch, false);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			// The scenes are about what is in them, even at midnight. The Nether is bright enough, and only turns red.
			player.removeEffect(MobEffects.NIGHT_VISION);
			if (dimension != Level.NETHER) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000000, 0, false, false));
			}
		});
		// Long enough for the chunks around to reach the client.
		context.waitTicks(120);
		// Again, for what the first one left: the loot of the last scene, and whatever spawned here meanwhile.
		world.getServer().runCommand("kill @e[type=!minecraft:player]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			stage.set(player.level(), player, ground[0]);
		});
		context.getInput().lookAt(0.0f, pitch);
		context.waitTicks(settleTicks);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			lastMoment.set(player.level(), player, ground[0]);
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
	}

	/** Levels and empties the area in front of the camera, and returns the ground under the camera. */
	private static BlockPos clearing(ServerLevel level, int spot) {
		BlockState floor = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState under = Blocks.DIRT.defaultBlockState();
		BlockPos ground;
		if (level.dimension() == Level.NETHER) {
			floor = Blocks.NETHERRACK.defaultBlockState();
			under = floor;
			ground = new BlockPos(spot * 128, 45, 0);
		} else if (level.dimension() == Level.END) {
			floor = Blocks.END_STONE.defaultBlockState();
			under = floor;
			ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, loaded(level, new BlockPos(1000, 64, 0)));
			if (ground.getY() <= level.getMinY() + 1) {
				ground = new BlockPos(1000, 64, 0);
			}
		} else {
			BlockPos center = flattest(level, new BlockPos(spot * 1200, 64, 0));
			ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, loaded(level, center));
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = -CLEARING_SIDE; x <= CLEARING_SIDE; x++) {
			for (int z = -CLEARING_BACK; z <= CLEARING_FRONT; z++) {
				loaded(level, ground.offset(x, 0, z));
				for (int y = -5; y < CLEARING_HEIGHT; y++) {
					BlockState state = y >= 0 ? Blocks.AIR.defaultBlockState() : y == -1 ? floor : under;
					level.setBlock(pos.set(ground.getX() + x, ground.getY() + y, ground.getZ() + z), state, Block.UPDATE_CLIENTS);
				}
			}
		}
		return ground;
	}

	/** The levelest dry land around {@code near}, going by the world generator alone so no chunks are made for it. */
	private static BlockPos flattest(ServerLevel level, BlockPos near) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState random = level.getChunkSource().randomState();
		BlockPos best = near;
		int bestRange = Integer.MAX_VALUE;
		for (int dx = -512; dx <= 512; dx += 128) {
			for (int dz = -512; dz <= 512; dz += 128) {
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for (int x = -64; x <= 64; x += 32) {
					for (int z = -32; z <= 96; z += 32) {
						int height = generator.getBaseHeight(near.getX() + dx + x, near.getZ() + dz + z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
						min = Math.min(min, height);
						max = Math.max(max, height);
					}
				}
				if (min > level.getSeaLevel() + 1 && max - min < bestRange) {
					bestRange = max - min;
					best = near.offset(dx, 0, dz);
				}
			}
		}
		return best;
	}

	private static BlockPos loaded(ServerLevel level, BlockPos pos) {
		level.getChunk(pos);
		return pos;
	}

	/** The surface {@code side} blocks to the side of the camera and {@code distance} in front of it. */
	private static BlockPos at(ServerLevel level, BlockPos ground, int side, int distance) {
		return ground.offset(side, 0, distance);
	}

	private static ActiveEvent start(ServerLevel level, dev.santiagosantofimio.worldevents.event.WorldEvent event) {
		EventManager.start(level, event);
		return EventManager.active(level);
	}

	private static void crowd(ServerLevel level, BlockPos ground, EntityType<? extends Mob> type, int side, int distance) {
		Mob mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob != null) {
			BlockPos pos = at(level, ground, side, distance);
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0f, 0.0f);
			level.addFreshEntity(mob);
			pose(mob);
		}
	}

	/** Facing the camera and standing still. */
	private static void pose(Mob mob) {
		if (mob != null) {
			mob.setNoAi(true);
			mob.setTarget(null);
			mob.setYRot(180.0f);
			mob.setYHeadRot(180.0f);
			mob.setYBodyRot(180.0f);
		}
	}
}
