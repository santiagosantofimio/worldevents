package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.LivingMeteorite;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.Protection;
import dev.santiagosantofimio.worldevents.util.WeightedPicker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A night of shooting stars. A few meteors fall near players and leave a small crater lined with ores, or just
 * drop the ores if block damage is off. Craters only dig natural terrain (dirt, sand, stone…), never blocks with
 * a block entity, liquids or anything inside spawn protection, so player builds are left alone.
 *
 * <p>Shooting stars are particles only. Meteors are falling magma blocks that we track every tick: we predict the
 * impact one tick ahead and handle it ourselves, so the block never lands on its own.
 */
public final class MeteorShowerEvent implements WorldEvent {
	public static final String ID = "meteor_shower";
	private static final String METEOR_TAG = WorldEventsMod.MOD_ID + ".meteor";

	private static final long LATEST_START = 18000;

	private static final int MAX_FALLING = 8;
	private static final int MAX_STREAKS = 12;
	private static final int MIN_IMPACT_DISTANCE = 12;
	private static final int MAX_IMPACT_DISTANCE = 40;
	private static final double MIN_PLAYER_CLEARANCE = 8.0;
	private static final double FALL_HEIGHT = 50.0;
	private static final double NOTIFY_DISTANCE = 48.0;
	private static final double MIN_CRATER_RADIUS = 1.5;
	private static final double MAX_CRATER_RADIUS = 4.0;

	private record Ore(int weight, Block stone, Block deepslate) {
	}

	private record Loot(int weight, Item item, int min, int max) {
	}

	private static final List<Ore> ORES = List.of(
			new Ore(35, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
			new Ore(25, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
			new Ore(15, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE),
			new Ore(10, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
			new Ore(8, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
			new Ore(5, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
			new Ore(2, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE));

	private static final List<Ore> NETHER_ORES = List.of(
			new Ore(55, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
			new Ore(43, Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_QUARTZ_ORE),
			new Ore(2, Blocks.ANCIENT_DEBRIS, Blocks.ANCIENT_DEBRIS));

	/** What a meteor leaves when it cannot make a crater. */
	private static final List<Loot> LOOT = List.of(
			new Loot(35, Items.RAW_IRON, 1, 3),
			new Loot(25, Items.RAW_COPPER, 2, 4),
			new Loot(15, Items.RAW_GOLD, 1, 2),
			new Loot(10, Items.REDSTONE, 3, 5),
			new Loot(8, Items.LAPIS_LAZULI, 3, 5),
			new Loot(5, Items.DIAMOND, 1, 1),
			new Loot(2, Items.EMERALD, 1, 1));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 4 in-game days; lasts 3 minutes.
		JsonObject json = EventSettings.defaults(true, 10, 4800, 180);
		json.addProperty("shootingStarInterval", 2);
		json.addProperty("impactChance", 0.04);
		json.addProperty("impactsPerPlayer", 3);
		json.addProperty("craters", true);
		json.addProperty("craterRadius", 2.5);
		json.addProperty("oreChance", 0.35);
		json.addProperty("fragmentsPerImpact", 2);
		json.addProperty("livingMeteoriteChance", 0.2);
		json.addProperty("pickaxeChance", 0.15);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.PURPLE;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.LIGHT_PURPLE;
	}

	@Override
	public Holder<SoundEvent> startSound() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR);
	}

	/** Needs an open sky with a day cycle: the overworld, not the Nether or the End. */
	@Override
	public boolean isAvailable(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime() && !level.dimensionType().hasCeiling();
	}

	@Override
	public boolean canStart(ServerLevel level) {
		return level.isDarkOutside() && DayCycle.isBetween(level, DayCycle.NIGHT_START, LATEST_START);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		ShowerState state = active.state(ShowerState::new);
		state.meteorites.removeIf(meteorite -> meteorite.isRemoved() || !meteorite.isAlive());
		EventSettings settings = settings();
		double starChance = 1.0 / Math.max(1, settings.getInt("shootingStarInterval"));
		double impactChance = settings.getDouble("impactChance");
		int impactCap = settings.getInt("impactsPerPlayer");
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || random.nextDouble() >= starChance) {
				continue;
			}
			int impacts = state.impacts.getOrDefault(player.getUUID(), 0);
			if (impacts < impactCap && state.falling.size() < MAX_FALLING && random.nextDouble() < impactChance
					&& launchMeteor(level, player, state)) {
				state.impacts.put(player.getUUID(), impacts + 1);
			} else if (state.streaks.size() < MAX_STREAKS) {
				state.streaks.add(Streak.near(player, level, random));
			}
		}
	}

	@Override
	public void fastTick(ServerLevel level, ActiveEvent active) {
		ShowerState state = active.state(ShowerState::new);
		Iterator<Streak> streaks = state.streaks.iterator();
		while (streaks.hasNext()) {
			Streak streak = streaks.next();
			streak.render(level);
			if (streak.advance()) {
				streaks.remove();
			}
		}

		Iterator<Meteor> meteors = state.falling.iterator();
		while (meteors.hasNext()) {
			Meteor meteor = meteors.next();
			FallingBlockEntity entity = meteor.entity;
			if (entity.isRemoved()) {
				meteors.remove();
				if (entity.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
					// It landed before we caught it and may have placed its block.
					landedOnItsOwn(level, entity);
					state.landed++;
				}
				continue;
			}
			Vec3 pos = entity.position();
			level.sendParticles(ParticleTypes.FLAME, true, false, pos.x, pos.y + 0.5, pos.z, 4, 0.25, 0.25, 0.25, 0.01);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, pos.x, pos.y + 0.5, pos.z, 2, 0.2, 0.2, 0.2, 0.01);
			level.sendParticles(ParticleTypes.LAVA, true, false, pos.x, pos.y + 0.5, pos.z, 1, 0.2, 0.2, 0.2, 0.0);

			// Where the falling block will be next tick (gravity is applied before it moves).
			Vec3 next = pos.add(entity.getDeltaMovement().add(0.0, -0.04, 0.0).scale(1.5));
			BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
			if (hit.getType() == HitResult.Type.BLOCK || entity.onGround() || entity.isInWater()) {
				BlockPos ground = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().immutable() : entity.blockPosition().below();
				entity.discard();
				meteors.remove();
				impact(level, ground);
				state.landed++;
			}
		}
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		ShowerState state = active.state(ShowerState::new);
		for (Meteor meteor : state.falling) {
			meteor.entity.discard();
		}
		state.falling.clear();
		state.streaks.clear();
		for (MagmaCube meteorite : level.getEntities(EntityTypeTest.forClass(MagmaCube.class),
				meteorite -> LivingMeteorite.is(meteorite) && !Creature.isSummoned(meteorite))) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, meteorite.getX(), meteorite.getY() + 1.0, meteorite.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
			Creature.remove(meteorite);
		}
		state.meteorites.clear();
	}

	@Override
	public void registerHooks() {
		// Meteors are only tracked in memory. One loaded from a save (after a restart or a chunk reload) is removed
		// instead of landing as a plain block. Removal waits for the end of the tick: the entity is still being
		// added to the level when the load callback runs.
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof FallingBlockEntity) || !entity.getTags().contains(METEOR_TAG)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			boolean tracked = active != null && active.event() == this && active.state(ShowerState::new).tracks(entity);
			if (!tracked) {
				DeferredDiscard.discard(entity);
			}
		});

		// Living meteorites only live during the shower: one loaded afterwards crumbles.
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof MagmaCube meteorite) || !LivingMeteorite.is(meteorite) || Creature.isSummoned(meteorite)) {
				return;
			}
			ActiveEvent active = EventManager.active(level);
			if (active != null && active.event() == this) {
				List<MagmaCube> meteorites = active.state(ShowerState::new).meteorites;
				if (!meteorites.contains(meteorite)) {
					meteorites.add(meteorite);
				}
			} else {
				DeferredDiscard.discard(meteorite);
				meteorite.getPassengers().forEach(DeferredDiscard::discard);
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof MagmaCube meteorite) || !LivingMeteorite.is(meteorite) || !(meteorite.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: meteorite.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
			if (killer != null) {
				LivingMeteorite.dropLoot(level, meteorite, killer, settings().getInt("fragmentsPerImpact") + 2, settings().getDouble("pickaxeChance"));
				if (!Creature.isSummoned(meteorite)) {
					EventManager.score(killer, ID, 5);
				}
			}
		});

		// Its burning fists set whatever it hits on fire.
		Hooks.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken > 0 && source.getEntity() instanceof MagmaCube meteorite && LivingMeteorite.is(meteorite) && source.getDirectEntity() == meteorite) {
				entity.igniteForSeconds(4.0f);
			}
		});
	}

	private static boolean launchMeteor(ServerLevel level, ServerPlayer player, ShowerState state) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 4; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Mth.nextDouble(random, MIN_IMPACT_DISTANCE, MAX_IMPACT_DISTANCE);
			int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
			// Checked before any height lookup, which would load the chunk.
			if (!level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
				continue;
			}
			int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos target = new BlockPos(x, surfaceY - 1, z);
			if (Protection.isSpawnProtected(level, target) || !level.getFluidState(target).isEmpty() || isNearPlayer(level, target)) {
				continue;
			}
			// Come in at an angle from a random direction so the trail is visible.
			double heading = random.nextDouble() * Math.PI * 2;
			Vec3 velocity = new Vec3(Math.cos(heading) * 0.7, -0.6, Math.sin(heading) * 0.7);
			Vec3 drift = horizontalDrift(velocity, FALL_HEIGHT);
			BlockPos start = BlockPos.containing(x + 0.5 - drift.x, surfaceY + FALL_HEIGHT, z + 0.5 - drift.z);
			if (start.getY() >= level.getMaxY() || !level.isPositionEntityTicking(start) || !level.getBlockState(start).isAir()) {
				continue;
			}
			// fall() replaces the block at its position with air, which is why we checked it is already air.
			FallingBlockEntity meteor = FallingBlockEntity.fall(level, start, Blocks.MAGMA_BLOCK.defaultBlockState());
			meteor.setDeltaMovement(velocity);
			meteor.disableDrop();
			meteor.addTag(METEOR_TAG);
			state.falling.add(new Meteor(meteor));
			level.playSound(null, start.getX(), start.getY(), start.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.WEATHER, 6.0f, 0.5f);
			return true;
		}
		return false;
	}

	/** Horizontal distance a falling block travels while dropping {@code height}, using its own physics. */
	private static Vec3 horizontalDrift(Vec3 velocity, double height) {
		double x = 0;
		double y = 0;
		double z = 0;
		Vec3 motion = velocity;
		for (int i = 0; i < 400 && y > -height; i++) {
			motion = motion.add(0.0, -0.04, 0.0);
			x += motion.x;
			y += motion.y;
			z += motion.z;
			motion = motion.scale(0.98);
		}
		return new Vec3(x, 0.0, z);
	}

	private static boolean isNearPlayer(ServerLevel level, BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(center) < MIN_PLAYER_CLEARANCE * MIN_PLAYER_CLEARANCE) {
				return true;
			}
		}
		return false;
	}

	private void landedOnItsOwn(ServerLevel level, FallingBlockEntity entity) {
		BlockPos pos = entity.blockPosition();
		if (!level.isLoaded(pos)) {
			return;
		}
		if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
			// Remove the block it placed; the crater (or the loot) replaces it.
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		impact(level, pos.below());
	}

	/**
	 * A meteor hits the top of {@code ground}: effects, then a crater with ores, or loot if craters are disabled
	 * or nothing around could be dug.
	 */
	public void impact(ServerLevel level, BlockPos ground) {
		Vec3 center = Vec3.atCenterOf(ground).add(0.0, 0.6, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.LAVA, true, false, center.x, center.y, center.z, 20, 1.2, 0.6, 1.2, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, center.x, center.y, center.z, 30, 1.5, 1.0, 1.5, 0.02);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f, 0.7f);

		EventSettings settings = settings();
		boolean craters = ModConfig.get().blockDamage() && settings.getBoolean("craters");
		int removed = 0;
		if (craters && level.getFluidState(ground).isEmpty() && !Protection.isSpawnProtected(level, ground)) {
			removed = carveCrater(level, ground, settings.getDouble("craterRadius"), settings.getDouble("oreChance"));
		}
		if (removed == 0) {
			dropLoot(level, center);
		}
		String language = Lang.languageNear(level, center, NOTIFY_DISTANCE);
		int fragments = settings.getInt("fragmentsPerImpact");
		if (fragments > 0) {
			Drops.spawn(level, center, EventItems.METEORITE_FRAGMENT.create(language, fragments));
		}
		if (level.getDifficulty() != Difficulty.PEACEFUL && level.getRandom().nextDouble() < settings.getDouble("livingMeteoriteChance")) {
			raiseMeteorite(level, ground, language);
		}

		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(center) < NOTIFY_DISTANCE * NOTIFY_DISTANCE) {
				player.sendSystemMessage(Lang.component(player, "event.meteor_shower.impact").withStyle(textColor()), true);
			}
		}
	}

	/** A living meteorite climbs out of the crater (or off the impact site when there is no crater). */
	private void raiseMeteorite(ServerLevel level, BlockPos ground, String language) {
		BlockPos spot = MobSpawning.spotInColumn(level, EntityType.MAGMA_CUBE, ground.getX(), ground.getZ(), ground.getY() + 2, ground.getY() - 4);
		if (spot == null) {
			return;
		}
		MagmaCube meteorite = LivingMeteorite.spawn(level, spot, language);
		if (meteorite == null) {
			return;
		}
		ActiveEvent active = EventManager.active(level);
		// Usually already tracked: the load callback also sees new entities.
		if (active != null && active.event() == this && !active.state(ShowerState::new).meteorites.contains(meteorite)) {
			active.state(ShowerState::new).meteorites.add(meteorite);
		}
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(meteorite) < NOTIFY_DISTANCE * NOTIFY_DISTANCE) {
				player.sendSystemMessage(Lang.component(player, "mob.living_meteorite.rises").withStyle(textColor()));
			}
		}
	}

	/** Digs a shallow bowl in natural terrain and lines it with ores. Returns how many blocks were removed. */
	private static int carveCrater(ServerLevel level, BlockPos ground, double radius, double oreChance) {
		double r = Math.clamp(radius, MIN_CRATER_RADIUS, MAX_CRATER_RADIUS);
		int reach = Mth.ceil(r) + 1;
		// Sphere center slightly above the surface: about 5 blocks wide and 2 deep for the default radius.
		double cx = ground.getX() + 0.5;
		double cy = ground.getY() + 0.5 + r * 0.35;
		double cz = ground.getZ() + 0.5;
		RandomSource random = level.getRandom();
		List<BlockPos> shell = new ArrayList<>();
		int removed = 0;
		int lowestCenterY = Integer.MAX_VALUE;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dz = -reach; dz <= reach; dz++) {
					pos.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz);
					double distance = Math.sqrt(Mth.square(pos.getX() + 0.5 - cx) + Mth.square(pos.getY() + 0.5 - cy) + Mth.square(pos.getZ() + 0.5 - cz));
					if (distance > r + 1.0 || !canChange(level, pos)) {
						continue;
					}
					BlockState state = level.getBlockState(pos);
					if (distance <= r) {
						if (isNaturalGround(state) || isLoosePlant(state)) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
							removed++;
							if (dx == 0 && dz == 0) {
								lowestCenterY = Math.min(lowestCenterY, pos.getY());
							}
						}
					} else if (pos.getY() + 0.5 < cy && isNaturalGround(state)) {
						shell.add(pos.immutable());
					}
				}
			}
		}
		if (removed == 0) {
			return 0;
		}
		BlockPos core = null;
		if (lowestCenterY != Integer.MAX_VALUE) {
			// The meteorite itself, glowing at the bottom of the crater.
			BlockPos below = new BlockPos(ground.getX(), lowestCenterY - 1, ground.getZ());
			if (canChange(level, below) && isNaturalGround(level.getBlockState(below))) {
				level.setBlock(below, Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
				core = below;
			}
		}
		for (BlockPos wall : shell) {
			if (!wall.equals(core) && random.nextDouble() < oreChance) {
				level.setBlock(wall, pickOre(level.getBlockState(wall), random), Block.UPDATE_ALL);
			}
		}
		return removed;
	}

	private static boolean canChange(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return level.isLoaded(pos) && !state.hasBlockEntity() && state.getFluidState().isEmpty() && !Protection.isSpawnProtected(level, pos);
	}

	/** Terrain a crater may dig. Building blocks (planks, bricks, glass, terracotta…) are never touched. */
	public static boolean isNaturalGround(BlockState state) {
		return state.is(BlockTags.DIRT)
				|| state.is(BlockTags.SAND)
				|| state.is(BlockTags.BASE_STONE_OVERWORLD)
				|| state.is(BlockTags.BASE_STONE_NETHER)
				|| state.is(BlockTags.NYLIUM)
				|| state.is(BlockTags.SNOW)
				|| state.is(Blocks.GRAVEL)
				|| state.is(Blocks.CLAY)
				|| state.is(Blocks.END_STONE);
	}

	private static boolean isLoosePlant(BlockState state) {
		return !state.isAir() && (state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.SMALL_FLOWERS));
	}

	private static BlockState pickOre(BlockState replaced, RandomSource random) {
		List<Ore> table = replaced.is(BlockTags.BASE_STONE_NETHER) ? NETHER_ORES : ORES;
		Ore ore = WeightedPicker.pick(table, Ore::weight, random.nextDouble());
		boolean deep = replaced.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
		return (deep ? ore.deepslate() : ore.stone()).defaultBlockState();
	}

	private static void dropLoot(ServerLevel level, Vec3 at) {
		RandomSource random = level.getRandom();
		int stacks = Mth.nextInt(random, 2, 3);
		for (int i = 0; i < stacks; i++) {
			Loot loot = WeightedPicker.pick(LOOT, Loot::weight, random.nextDouble());
			Drops.spawn(level, at, new ItemStack(loot.item(), Mth.nextInt(random, loot.min(), loot.max())));
		}
	}

	/** How many meteors of this run have hit the ground so far. */
	public static int landedCount(ActiveEvent active) {
		return active.state(ShowerState::new).landed;
	}

	private static final class ShowerState {
		private final List<Meteor> falling = new ArrayList<>();
		private final List<Streak> streaks = new ArrayList<>();
		private final Map<UUID, Integer> impacts = new HashMap<>();
		private final List<MagmaCube> meteorites = new ArrayList<>();
		private int landed;

		private boolean tracks(Entity entity) {
			for (Meteor meteor : falling) {
				if (meteor.entity == entity) {
					return true;
				}
			}
			return false;
		}
	}

	private record Meteor(FallingBlockEntity entity) {
	}

	/** A shooting star: a short line of particles crossing the sky, visible from far away. */
	private static final class Streak {
		private Vec3 pos;
		private final Vec3 step;
		private int ticksLeft;

		private Streak(Vec3 pos, Vec3 step, int ticksLeft) {
			this.pos = pos;
			this.step = step;
			this.ticksLeft = ticksLeft;
		}

		private static Streak near(ServerPlayer player, ServerLevel level, RandomSource random) {
			double angle = random.nextDouble() * Math.PI * 2;
			double offset = Mth.nextDouble(random, 0, 40);
			double y = Math.max(player.getY(), level.getSeaLevel()) + Mth.nextDouble(random, 45, 70);
			Vec3 start = new Vec3(player.getX() + Math.cos(angle) * offset, y, player.getZ() + Math.sin(angle) * offset);
			double heading = random.nextDouble() * Math.PI * 2;
			double speed = Mth.nextDouble(random, 2.0, 3.5);
			Vec3 step = new Vec3(Math.cos(heading) * speed, -Mth.nextDouble(random, 0.3, 0.9), Math.sin(heading) * speed);
			return new Streak(start, step, Mth.nextInt(random, 12, 20));
		}

		private void render(ServerLevel level) {
			Vec3 mid = pos.add(step.scale(0.5));
			level.sendParticles(ParticleTypes.END_ROD, true, false, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.FIREWORK, true, false, mid.x, mid.y, mid.z, 1, 0.05, 0.05, 0.05, 0.01);
		}

		/** Moves the star forward; returns true once it has burnt out. */
		private boolean advance() {
			pos = pos.add(step);
			return --ticksLeft <= 0;
		}
	}
}
