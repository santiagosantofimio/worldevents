package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.HarvestSpirit;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.Entities;
import dev.santiagosantofimio.worldevents.util.Protection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A blessed harvest: for a few minutes, crops around players grow by themselves and ripe crops drop double. Each
 * player gets a harvest spirit that follows them and leaves golden seeds, which ripen the crops around them at once.
 * It only ever makes crops grow: no blocks are placed or broken.
 */
public final class BlessedHarvestEvent implements WorldEvent {
	public static final String ID = "blessed_harvest";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 9000;
	/** How far above and below a player the harvest looks for crops. */
	private static final int GROWTH_HEIGHT = 3;
	/** A golden seed ripens the crops this far around the block it is used on. */
	private static final int SEED_RADIUS = 3;
	/** Enough steps for any crop to ripen, and for a ripe stem to grow its fruit. */
	private static final int MAX_GROWTH_STEPS = 16;
	/** A spirit this far from its friend goes away; a new one comes once the friend is back in loaded land. */
	private static final double MAX_FRIEND_DISTANCE = 48.0;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 5 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 300);
		json.addProperty("growthRadius", 12);
		json.addProperty("growthAttemptsPerPlayer", 16);
		json.addProperty("dropMultiplier", 2.0);
		json.addProperty("spirits", true);
		json.addProperty("maxSpirits", 6);
		json.addProperty("seedInterval", 90);
		json.addProperty("seedsPerPlayer", 3);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.GREEN;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.GREEN;
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.NOTE_BLOCK_CHIME;
	}

	/** Crops need the sun: the overworld, not the Nether or the End. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	/** In the morning, so the whole event happens in daylight. */
	@Override
	public boolean canStart(ServerLevel level) {
		return !level.isNight() && DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		EventSettings settings = settings();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				growAround(level, player.blockPosition(), settings.getInt("growthRadius"), settings.getInt("growthAttemptsPerPlayer"));
			}
		}
		tickSpirits(level, active.state(HarvestState::new), settings);
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		HarvestState state = active.state(HarvestState::new);
		for (Parrot spirit : state.spirits.values()) {
			if (!spirit.removed) {
				HarvestSpirit.vanish(level, spirit);
			}
		}
		state.spirits.clear();
	}

	/**
	 * Gives a few random crops around {@code center} one step of growth, like bone meal. Only looks at loaded,
	 * ticking chunks, a few columns per call.
	 */
	private static void growAround(ServerLevel level, BlockPos center, int radius, int attempts) {
		Random random = level.getRandom();
		for (int i = 0; i < attempts; i++) {
			int x = center.getX() + Mth.nextInt(random, -radius, radius);
			int z = center.getZ() + Mth.nextInt(random, -radius, radius);
			if (!Entities.isTicking(level, new BlockPos(x, center.getY(), z))) {
				continue;
			}
			for (int y = center.getY() + GROWTH_HEIGHT; y >= center.getY() - GROWTH_HEIGHT; y--) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				if (isCrop(state) && !Protection.isSpawnProtected(level, pos) && grow(level, pos, state)) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
					break;
				}
			}
		}
	}

	/** Ripens every crop around {@code center} at once, as a golden seed does. Returns how many crops grew. */
	public static int ripenAround(ServerLevel level, BlockPos center) {
		int ripened = 0;
		for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-SEED_RADIUS, -1, -SEED_RADIUS), center.offset(SEED_RADIUS, 1, SEED_RADIUS))) {
			BlockPos pos = mutable.immutable();
			boolean grew = false;
			for (int step = 0; step < MAX_GROWTH_STEPS; step++) {
				BlockState state = level.getBlockState(pos);
				if (!isCrop(state) || !grow(level, pos, state)) {
					break;
				}
				grew = true;
			}
			if (grew) {
				ripened++;
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
			}
		}
		return ripened;
	}

	/** Farm crops (wheat, carrots, stems…), sweet berries and cocoa. */
	private static boolean isCrop(BlockState state) {
		return state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COCOA);
	}

	/** One step of growth, like a use of bone meal; a ripe stem tries to grow its fruit. Returns whether it grew. */
	private static boolean grow(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BonemealableBlock crop && crop.isValidBonemealTarget(level, pos, state, false)) {
			crop.performBonemeal(level, level.getRandom(), pos, state);
			return true;
		}
		if (state.getBlock() instanceof StemBlock) {
			// A ripe stem grows a melon or pumpkin on its random ticks, if there is room next to it.
			state.randomTick(level, pos, level.getRandom());
			return level.getBlockState(pos) != state;
		}
		return false;
	}

	/** Ripe crops, whose drops the harvest doubles. Fruit blocks are left out: they can be placed again. */
	private static boolean isRipe(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		return state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == 2;
	}

	private void tickSpirits(ServerLevel level, HarvestState state, EventSettings settings) {
		Iterator<Map.Entry<UUID, Parrot>> spirits = state.spirits.entrySet().iterator();
		while (spirits.hasNext()) {
			Map.Entry<UUID, Parrot> entry = spirits.next();
			Parrot spirit = entry.getValue();
			ServerPlayer friend = level.getServer().getPlayerList().getPlayer(entry.getKey());
			if (spirit.removed || !spirit.isAlive()) {
				spirits.remove();
			} else if (friend == null || friend.level != level || friend.isSpectator() || friend.distanceToSqr(spirit) > MAX_FRIEND_DISTANCE * MAX_FRIEND_DISTANCE) {
				HarvestSpirit.vanish(level, spirit);
				spirits.remove();
			} else {
				tickSeeds(level, state, settings, spirit, friend);
			}
		}
		if (!settings.getBoolean("spirits")) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || state.spirits.containsKey(player.getUUID()) || state.spirits.size() >= settings.getInt("maxSpirits")) {
				continue;
			}
			BlockPos spot = MobSpawning.spotNear(level, EntityType.PARROT, player.blockPosition(), 2, 5, 2, 8);
			if (spot != null) {
				// Hovering a block up, if there is room.
				BlockPos air = spot.above();
				boolean room = level.noCollision(EntityType.PARROT.getAABB(air.getX() + 0.5, air.getY(), air.getZ() + 0.5));
				spawnSpirit(level, state, room ? air : spot, player, settings);
			}
		}
	}

	/** Every so often, the spirit leaves its friend a golden seed, up to a few per player and event. */
	private static void tickSeeds(ServerLevel level, HarvestState state, EventSettings settings, Parrot spirit, ServerPlayer friend) {
		UUID id = friend.getUUID();
		int interval = Math.max(1, settings.getInt("seedInterval"));
		Integer seconds = state.secondsToSeed.get(id);
		if (seconds == null) {
			// A spirit back from a save starts counting again: no seed straight away.
			state.secondsToSeed.put(id, interval);
			return;
		}
		int given = state.seedsGiven.getOrDefault(id, 0);
		if (given >= settings.getInt("seedsPerPlayer")) {
			return;
		}
		if (seconds > 1) {
			state.secondsToSeed.put(id, seconds - 1);
			return;
		}
		HarvestSpirit.leaveSeed(level, spirit, friend);
		state.seedsGiven.put(id, given + 1);
		state.secondsToSeed.put(id, interval);
		friend.displayClientMessage(Lang.component(friend, "mob.harvest_spirit.seed").withStyle(ChatFormatting.GREEN), true);
	}

	private static Parrot spawnSpirit(ServerLevel level, HarvestState state, BlockPos pos, ServerPlayer friend, EventSettings settings) {
		Parrot spirit = HarvestSpirit.spawn(level, pos, Lang.language(friend), friend);
		if (spirit == null) {
			return null;
		}
		state.spirits.put(friend.getUUID(), spirit);
		// The seed count belongs to the player: a spirit that comes back does not start over.
		if (state.secondsToSeed.putIfAbsent(friend.getUUID(), Math.max(1, settings.getInt("seedInterval"))) == null) {
			friend.sendMessage(Lang.component(friend, "mob.harvest_spirit.appeared").withStyle(ChatFormatting.GREEN), net.minecraft.Util.NIL_UUID);
		}
		return spirit;
	}

	/** Spawns a harvest spirit for {@code friend} at {@code pos} now, for tests and admins. */
	public Parrot spawnSpirit(ServerLevel level, ActiveEvent active, BlockPos pos, ServerPlayer friend) {
		HarvestState state = active.state(HarvestState::new);
		Parrot old = state.spirits.get(friend.getUUID());
		if (old != null && !old.removed) {
			HarvestSpirit.vanish(level, old);
		}
		return spawnSpirit(level, state, pos, friend, settings());
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Parrot spirit) || !HarvestSpirit.is(spirit) || Creature.isSummoned(spirit)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			UUID friend = HarvestSpirit.friend(spirit);
			Map<UUID, Parrot> spirits = active != null && active.event() == this ? active.state(HarvestState::new).spirits : null;
			if (spirits != null && friend != null && (spirits.get(friend) == null || spirits.get(friend).removed)) {
				spirits.put(friend, spirit);
			} else if (spirits == null || spirits.get(friend) != spirit) {
				DeferredDiscard.discard(spirit);
				spirit.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		// Nothing hurts a spirit but /kill, the void and players in creative (as with vanilla invulnerable mobs).
		Hooks.ALLOW_DAMAGE.register((entity, source, amount) ->
				!HarvestSpirit.is(entity) || source.isBypassInvul() || source.isCreativePlayer());

		// No handing it items (it would hold them in the air, or duplicate itself with amethyst): it just says hello.
		Hooks.USE_ENTITY.register((player, world, hand, entity, hitResult) -> {
			if (!(entity instanceof Parrot spirit) || !HarvestSpirit.is(spirit) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			HarvestSpirit.greet(level, spirit);
			return InteractionResult.SUCCESS;
		});

		// Golden seed: the crops around the clicked block ripen at once. Works any time, not only during the event.
		Hooks.USE_BLOCK.register((player, world, hand, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.GOLDEN_SEED.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			BlockPos center = hitResult.getBlockPos();
			if (ripenAround(level, center) == 0) {
				serverPlayer.displayClientMessage(Lang.component(serverPlayer, "item.golden_seed.fail").withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.FAIL;
			}
			level.playSound(null, center, SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1.0f, 1.0f);
			level.playSound(null, center, SoundEvents.NOTE_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
			if (!player.abilities.instabuild) {
				stack.shrink(1);
			}
			return InteractionResult.SUCCESS;
		});

		Hooks.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
			if (state == null || !isRipe(state) || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
			if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer farmer) {
				EventManager.score(farmer, ID, 1);
			}
		});
	}

	private static final class HarvestState {
		/** Each player's spirit, by player. */
		private final Map<UUID, Parrot> spirits = new HashMap<>();
		private final Map<UUID, Integer> secondsToSeed = new HashMap<>();
		private final Map<UUID, Integer> seedsGiven = new HashMap<>();
	}
}
