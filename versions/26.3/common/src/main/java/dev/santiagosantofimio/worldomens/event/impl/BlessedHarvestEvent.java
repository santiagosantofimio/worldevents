package dev.santiagosantofimio.worldomens.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldomens.config.EventSettings;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.WorldEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.Creature;
import dev.santiagosantofimio.worldomens.mob.HarvestSpirit;
import dev.santiagosantofimio.worldomens.mob.MobSpawning;
import dev.santiagosantofimio.worldomens.platform.Hooks;
import dev.santiagosantofimio.worldomens.util.DayCycle;
import dev.santiagosantofimio.worldomens.util.DeferredDiscard;
import dev.santiagosantofimio.worldomens.util.Drops;
import dev.santiagosantofimio.worldomens.util.Protection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class BlessedHarvestEvent implements WorldEvent {
	public static final String ID = "blessed_harvest";
	private static final long EARLIEST_START = 1000;
	private static final long LATEST_START = 9000;
	private static final int GROWTH_HEIGHT = 3;
	private static final int SEED_RADIUS = 3;
	private static final int MAX_GROWTH_STEPS = 16;
	private static final double MAX_FRIEND_DISTANCE = 48.0;

	@Override
	public boolean peaceful() {
		return true;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
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
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_RESONATE);
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level);
	}

	@Override
	public boolean canStart(ServerLevel level) {
		return !level.isDarkOutside() && DayCycle.isBetween(level, EARLIEST_START, LATEST_START);
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
		for (Allay spirit : state.spirits.values()) {
			if (!spirit.isRemoved()) {
				HarvestSpirit.vanish(level, spirit);
			}
		}
		state.spirits.clear();
	}

	private static void growAround(ServerLevel level, BlockPos center, int radius, int attempts) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < attempts; i++) {
			int x = center.getX() + Mth.nextInt(random, -radius, radius);
			int z = center.getZ() + Mth.nextInt(random, -radius, radius);
			if (!level.isPositionEntityTicking(new BlockPos(x, center.getY(), z))) {
				continue;
			}
			for (int y = center.getY() + GROWTH_HEIGHT; y >= center.getY() - GROWTH_HEIGHT; y--) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				if (isCrop(state) && !Protection.isSpawnProtected(level, pos) && grow(level, pos, state, BonemealSource.MOB)) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
					break;
				}
			}
		}
	}

	public static int ripenAround(ServerLevel level, BlockPos center) {
		int ripened = 0;
		for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-SEED_RADIUS, -1, -SEED_RADIUS), center.offset(SEED_RADIUS, 1, SEED_RADIUS))) {
			BlockPos pos = mutable.immutable();
			boolean grew = false;
			for (int step = 0; step < MAX_GROWTH_STEPS; step++) {
				BlockState state = level.getBlockState(pos);
				if (!isCrop(state) || !grow(level, pos, state, BonemealSource.INTERACTION)) {
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

	private static boolean isCrop(BlockState state) {
		return state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.COCOA);
	}

	private static boolean grow(ServerLevel level, BlockPos pos, BlockState state, BonemealSource source) {
		if (state.getBlock() instanceof BonemealableBlock crop && crop.isValidBonemealTarget(level, pos, state, source)) {
			crop.performBonemeal(level, level.getRandom(), pos, state, source);
			return true;
		}
		if (state.getBlock() instanceof StemBlock) {
			state.randomTick(level, pos, level.getRandom());
			return level.getBlockState(pos) != state;
		}
		return false;
	}

	private static boolean isRipe(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		return state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
	}

	private void tickSpirits(ServerLevel level, HarvestState state, EventSettings settings) {
		Iterator<Map.Entry<UUID, Allay>> spirits = state.spirits.entrySet().iterator();
		while (spirits.hasNext()) {
			Map.Entry<UUID, Allay> entry = spirits.next();
			Allay spirit = entry.getValue();
			ServerPlayer friend = level.getServer().getPlayerList().getPlayer(entry.getKey());
			if (spirit.isRemoved() || !spirit.isAlive()) {
				spirits.remove();
			} else if (friend == null || friend.level() != level || friend.isSpectator() || friend.distanceToSqr(spirit) > MAX_FRIEND_DISTANCE * MAX_FRIEND_DISTANCE) {
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
			BlockPos spot = MobSpawning.spotNear(level, EntityTypes.ALLAY, player.blockPosition(), 2, 5, 2, 8);
			if (spot != null) {
				BlockPos air = spot.above();
				boolean room = level.noCollision(EntityTypes.ALLAY.getSpawnAABB(air.getX() + 0.5, air.getY(), air.getZ() + 0.5));
				spawnSpirit(level, state, room ? air : spot, player, settings);
			}
		}
	}

	private static void tickSeeds(ServerLevel level, HarvestState state, EventSettings settings, Allay spirit, ServerPlayer friend) {
		UUID id = friend.getUUID();
		int interval = Math.max(1, settings.getInt("seedInterval"));
		Integer seconds = state.secondsToSeed.get(id);
		if (seconds == null) {
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
		friend.sendSystemMessage(Lang.component(friend, "mob.harvest_spirit.seed").withStyle(ChatFormatting.GREEN), true);
	}

	private static Allay spawnSpirit(ServerLevel level, HarvestState state, BlockPos pos, ServerPlayer friend, EventSettings settings) {
		Allay spirit = HarvestSpirit.spawn(level, pos, Lang.language(friend), friend);
		if (spirit == null) {
			return null;
		}
		state.spirits.put(friend.getUUID(), spirit);
		if (state.secondsToSeed.putIfAbsent(friend.getUUID(), Math.max(1, settings.getInt("seedInterval"))) == null) {
			friend.sendSystemMessage(Lang.component(friend, "mob.harvest_spirit.appeared").withStyle(ChatFormatting.GREEN));
		}
		return spirit;
	}

	public Allay spawnSpirit(ServerLevel level, ActiveEvent active, BlockPos pos, ServerPlayer friend) {
		HarvestState state = active.state(HarvestState::new);
		Allay old = state.spirits.get(friend.getUUID());
		if (old != null && !old.isRemoved()) {
			HarvestSpirit.vanish(level, old);
		}
		return spawnSpirit(level, state, pos, friend, settings());
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Allay spirit) || !HarvestSpirit.is(spirit) || Creature.isSummoned(spirit)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			UUID friend = HarvestSpirit.friend(spirit);
			Map<UUID, Allay> spirits = active != null && active.event() == this ? active.state(HarvestState::new).spirits : null;
			if (spirits != null && friend != null && (spirits.get(friend) == null || spirits.get(friend).isRemoved())) {
				spirits.put(friend, spirit);
			} else if (spirits == null || spirits.get(friend) != spirit) {
				DeferredDiscard.discard(spirit);
				spirit.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.ALLOW_DAMAGE.register((entity, source, amount) ->
				!HarvestSpirit.is(entity) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.isCreativePlayer());

		Hooks.USE_ENTITY.register((player, world, hand, entity, hitResult) -> {
			if (!(entity instanceof Allay spirit) || !HarvestSpirit.is(spirit) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			HarvestSpirit.greet(level, spirit);
			return InteractionResult.SUCCESS;
		});

		Hooks.USE_BLOCK.register((player, world, hand, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.GOLDEN_SEED.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			BlockPos center = hitResult.getBlockPos();
			if (ripenAround(level, center) == 0) {
				serverPlayer.sendSystemMessage(Lang.component(serverPlayer, "item.golden_seed.fail").withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.FAIL;
			}
			level.playSound(null, center, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
			level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		});

		Hooks.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
			if (state == null || !isRipe(state) || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
			if (context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer farmer) {
				EventManager.score(farmer, ID, 1);
			}
		});
	}

	private static final class HarvestState {
		private final Map<UUID, Allay> spirits = new HashMap<>();
		private final Map<UUID, Integer> secondsToSeed = new HashMap<>();
		private final Map<UUID, Integer> seedsGiven = new HashMap<>();
	}
}
