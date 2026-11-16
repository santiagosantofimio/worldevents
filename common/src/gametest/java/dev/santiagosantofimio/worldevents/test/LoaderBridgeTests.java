package dev.santiagosantofimio.worldevents.test;

import com.mojang.datafixers.util.Either;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.EventRegistry;
import dev.santiagosantofimio.worldevents.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldevents.event.impl.GoldRushEvent;
import dev.santiagosantofimio.worldevents.event.impl.TreasureHuntEvent;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.HarvestSpirit;
import dev.santiagosantofimio.worldevents.platform.GameEvents;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One pass through every {@link GameEvents} event by the game's real code path (a click, a hit, a broken block, a bed,
 * a loot roll), to prove a mod loader feeds them. The mod's own logic is covered on Fabric by WorldEventsGameTests; this
 * is what can differ from one loader to another. Run with {@code ./gradlew -Pgametests :neoforge:runGameTest} and
 * {@code ./gradlew -Pgametests :forge:runGameTestServer}.
 */
public final class LoaderBridgeTests {
	public static final String NAME = "loader_bridge";
	private static final String LANGUAGE = "es_es";

	private LoaderBridgeTests() {
	}

	@SuppressWarnings("removal")
	public static void run(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EventManager.stop(level);
		int[] serverTicks = {0};
		int[] levelTicks = {0};
		int[] joined = {0};
		int[] left = {0};
		int[] changedLevel = {0};
		int[] deaths = {0};
		GameEvents.SERVER_TICK.register(server -> serverTicks[0]++);
		GameEvents.LEVEL_TICK.register(ticked -> levelTicks[0]++);
		GameEvents.PLAYER_JOIN.register(player -> joined[0]++);
		GameEvents.PLAYER_LEAVE.register(player -> left[0]++);
		GameEvents.PLAYER_CHANGE_LEVEL.register(player -> changedLevel[0]++);
		GameEvents.AFTER_DEATH.register((entity, source) -> deaths[0]++);

		for (String command : List.of("worldevents", "omens", "eventos")) {
			helper.assertTrue(level.getServer().getCommands().getDispatcher().getRoot().getChild(command) != null, "no /" + command);
		}

		BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(3, 0, 3);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setGameMode(GameType.SURVIVAL);
		player.updateOptions(new ClientInformation(LANGUAGE, 8, ChatVisiblity.FULL, true, 0x7F, HumanoidArm.RIGHT, false, true, ParticleStatus.ALL));
		helper.assertValueEqual(joined[0], 1, "PLAYER_JOIN");

		// USE_BLOCK: a golden seed ripens the wheat it is used on.
		BlockPos wheat = origin.offset(3, 0, 0);
		level.setBlock(wheat.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(wheat, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
		ItemStack seed = EventItems.GOLDEN_SEED.create(LANGUAGE, 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, seed);
		InteractionResult seeded = player.gameMode.useItemOn(player, level, seed, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(wheat), Direction.UP, wheat, false));
		helper.assertTrue(seeded instanceof InteractionResult.Success, "USE_BLOCK: the seed gave " + seeded);
		helper.assertTrue(((CropBlock) Blocks.WHEAT).isMaxAge(level.getBlockState(wheat)), "USE_BLOCK: the wheat did not ripen");

		// ALLOW_DAMAGE, USE_ENTITY and the creature's item displays, which need the access transformer at run time.
		Mob spirit = Creature.summon(HarvestSpirit.KIND, level, origin.offset(0, 1, 3), LANGUAGE, player);
		helper.assertTrue(spirit != null && !Creature.parts(spirit).isEmpty(), "the creature has no model");
		float spiritHealth = spirit.getHealth();
		helper.assertFalse(spirit.hurtServer(level, level.damageSources().generic(), 5.0f), "ALLOW_DAMAGE: the spirit took the hit");
		helper.assertTrue(spirit.getHealth() == spiritHealth, "ALLOW_DAMAGE: the spirit lost health");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		InteractionResult greeted = player.interactOn(spirit, InteractionHand.MAIN_HAND, Vec3.ZERO);
		helper.assertTrue(greeted instanceof InteractionResult.Success, "USE_ENTITY: the spirit gave " + greeted);

		// AFTER_DAMAGE: the scythe heals whoever hits with it.
		player.setItemInHand(InteractionHand.MAIN_HAND, EventItems.BLOOD_SCYTHE.create(LANGUAGE, 1));
		player.setHealth(10.0f);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(0, 0, 6));
		zombie.hurtServer(level, level.damageSources().playerAttack(player), 6.0f);
		helper.assertTrue(player.getHealth() > 10.0f, "AFTER_DAMAGE: the scythe did not heal: " + player.getHealth());
		zombie.discard();

		// MODIFY_DROPS, through the loader's loot modifier and its c:ores tag.
		EventManager.start(level, EventRegistry.get(GoldRushEvent.ID));
		BlockPos ore = origin.offset(-3, 0, 0);
		helper.assertValueEqual(mine(level, ore, player, new ItemStack(Items.IRON_PICKAXE), Items.RAW_IRON), 2, "MODIFY_DROPS: raw iron during the gold rush");
		helper.assertValueEqual(mine(level, ore, player, EventItems.STARFALL_PICKAXE.create(LANGUAGE, 1), Items.IRON_INGOT), 2, "MODIFY_DROPS: smelted iron");
		ActiveEvent rush = EventManager.active(level);
		int rushRemaining = rush.remainingTicks();

		helper.runAfterDelay(25, () -> {
			helper.assertTrue(serverTicks[0] > 0, "SERVER_TICK never came");
			helper.assertTrue(levelTicks[0] > 0, "LEVEL_TICK never came");
			helper.assertTrue(rush.remainingTicks() < rushRemaining, "the event did not tick");
			EventManager.stop(level);

			// BEFORE_BLOCK_BREAK: the treasure stays while the hunt is on.
			TreasureHuntEvent hunt = (TreasureHuntEvent) EventRegistry.get(TreasureHuntEvent.ID);
			EventManager.start(level, hunt);
			BlockPos chest = origin.offset(0, 0, -3);
			hunt.placeTreasure(level, EventManager.active(level), chest, LANGUAGE);
			helper.assertFalse(player.gameMode.destroyBlock(chest), "BEFORE_BLOCK_BREAK: the treasure broke");
			helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "BEFORE_BLOCK_BREAK: the treasure is gone");
			EventManager.stop(level);

			level.getServer().setWeatherParameters(0, 0, false, false);
			level.setRainLevel(0.0f);
			level.setThunderLevel(0.0f);
			helper.setTime(13000);
		});

		// Daylight is recalculated on the next tick.
		helper.runAfterDelay(29, () -> {
			EventManager.start(level, EventRegistry.get(BloodMoonEvent.ID));

			// ALLOW_SLEEPING: the blood moon's own reason, not vanilla's.
			Block bed = Blocks.BED.red();
			BlockPos foot = origin.offset(2, 0, 2);
			BlockPos head = foot.north();
			level.setBlock(foot, bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_ALL);
			level.setBlock(head, bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
			player.teleportTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 1.5);
			AbstractBedBlock bedBlock = (AbstractBedBlock) bed;
			Either<Player.BedSleepingProblem, Unit> slept = player.startSleepInBed(bedBlock, level.getBlockState(foot), bedBlock.getBedRule(level, foot), foot);
			helper.assertTrue(slept.left().isPresent(), "ALLOW_SLEEPING: the player slept");
			Component reason = slept.left().get().message();
			helper.assertTrue(reason != null && reason.getString().equals("No puedes dormir durante la luna de sangre"),
					"ALLOW_SLEEPING: wrong reason, which also proves the mod's texts loaded: " + (reason == null ? null : reason.getString()));

			// ENTITY_LOAD: the blood moon strengthens what spawns during it.
			Zombie monster = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 0, 6));
			helper.assertTrue(monster.entityTags().contains(BloodMoonEvent.MOB_TAG), "ENTITY_LOAD: the zombie was not buffed");
			monster.discard();
			EventManager.stop(level);

			// AFTER_DEATH: the creature's model goes with it.
			spirit.kill(level);
		});

		helper.runAfterDelay(32, () -> {
			helper.assertTrue(deaths[0] >= 1, "AFTER_DEATH never came");
			helper.assertTrue(Creature.parts(spirit).isEmpty(), "AFTER_DEATH: the model stayed behind");

			// USE_ITEM: bottled lightning strikes the ground the player looks at.
			ItemStack bottle = EventItems.BOTTLED_LIGHTNING.create(LANGUAGE, 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
			player.setXRot(90.0f);
			InteractionResult used = player.gameMode.useItem(player, level, bottle, InteractionHand.MAIN_HAND);
			helper.assertTrue(used instanceof InteractionResult.Success, "USE_ITEM: the bottle gave " + used);
			helper.assertValueEqual(bottle.getCount(), 1, "USE_ITEM: bottles left");

			ServerLevel nether = level.getServer().getLevel(Level.NETHER);
			helper.assertTrue(nether != null, "no Nether");
			player.teleportTo(nether, 0.5, 100.0, 0.5, Set.of(), 0.0f, 0.0f, false);
			helper.assertValueEqual(changedLevel[0], 1, "PLAYER_CHANGE_LEVEL");
			level.getServer().getPlayerList().remove(player);
			helper.assertValueEqual(left[0], 1, "PLAYER_LEAVE");
			WorldEventsMod.LOGGER.info("Every game event reached the mod");
			helper.succeed();
		});
	}

	/** Breaks a fresh iron ore as if the player mined it with {@code tool}, and counts the matching drops. */
	private static int mine(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool, Item expected) {
		level.setBlock(pos, Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL);
		Block.dropResources(level.getBlockState(pos), level, pos, null, player, tool);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		int total = 0;
		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0))) {
			if (drop.getItem().is(expected)) {
				total += drop.getItem().getCount();
			}
			drop.discard();
		}
		return total;
	}
}
