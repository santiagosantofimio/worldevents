package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.BloodEye;
import dev.santiagosantofimio.worldevents.mob.BloodMoonLord;
import dev.santiagosantofimio.worldevents.mob.Creature;
import dev.santiagosantofimio.worldevents.mob.MobSpawning;
import dev.santiagosantofimio.worldevents.platform.Hooks;
import dev.santiagosantofimio.worldevents.platform.Tags;
import dev.santiagosantofimio.worldevents.util.Advancements;
import dev.santiagosantofimio.worldevents.util.DayCycle;
import dev.santiagosantofimio.worldevents.util.DeferredDiscard;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.Particles;
import dev.santiagosantofimio.worldevents.util.Protection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A night with more hostile mobs, which are also tougher and drop more loot and XP.
 * Ends at dawn. Mobs buffed by the event carry an entity tag so the buff can be undone afterwards.
 */
public final class BloodMoonEvent implements WorldEvent {
	public static final String ID = "blood_moon";
	public static final String MOB_TAG = WorldEventsMod.MOD_ID + ".blood_moon";
	public static final UUID HEALTH_MODIFIER = UUID.fromString("5d6f0b8e-1f3a-4c7e-9b5a-2a1e6c3d4b01");
	public static final UUID DAMAGE_MODIFIER = UUID.fromString("5d6f0b8e-1f3a-4c7e-9b5a-2a1e6c3d4b02");
	private static final String ENDS_AT_DAWN = "endsAtDawn";
	private static final String REACHED_DAWN = "reachedDawn";
	private static final String LORD_SPAWNED = "lordSpawned";
	private static final int EYE_INTERVAL_SECONDS = 20;

	private static final long LATEST_START = 16000;
	private static final int MIN_SPAWN_DISTANCE = 24;
	private static final int MAX_SPAWN_DISTANCE = 44;
	private static final DustParticleOptions MIST = Particles.dust(0x8A0303, 1.6f);

	@Override
	public String id() {
		return ID;
	}

	@Override
	public JsonObject defaultSettings() {
		// At most once every 5 in-game days; lasts until dawn, capped at 500 s.
		JsonObject json = EventSettings.defaults(true, 10, 6000, 500);
		json.addProperty("healthMultiplier", 1.5);
		json.addProperty("damageMultiplier", 1.3);
		json.addProperty("dropMultiplier", 2.0);
		json.addProperty("xpMultiplier", 2.0);
		json.addProperty("extraMobsPerPlayer", 6);
		json.addProperty("maxExtraMobs", 40);
		json.addProperty("spawnInterval", 5);
		json.addProperty("spawnAttemptsPerPlayer", 4);
		json.addProperty("allowSleeping", false);
		json.addProperty("darkenSky", true);
		json.addProperty("particles", true);
		json.addProperty("bloodyTearChance", 0.02);
		json.addProperty("bloodyTearSummons", true);
		json.addProperty("lord", true);
		json.addProperty("lordDelay", 120);
		json.addProperty("lordHealth", 200.0);
		json.addProperty("scytheChance", 0.35);
		json.addProperty("bloodEyes", true);
		json.addProperty("bloodEyesPerPlayer", 2);
		return json;
	}

	@Override
	public BossEvent.BossBarColor barColor() {
		return BossEvent.BossBarColor.RED;
	}

	@Override
	public ChatFormatting textColor() {
		return ChatFormatting.DARK_RED;
	}

	@Override
	public boolean darkenSky() {
		return settings().getBoolean("darkenSky");
	}

	@Override
	public SoundEvent startSound() {
		return SoundEvents.RAID_HORN;
	}

	@Override
	public boolean isAvailable(ServerLevel level) {
		return DayCycle.hasDayCycle(level) && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
	}

	/** Only at the start of the night, so the event covers the whole night. */
	@Override
	public boolean canStart(ServerLevel level) {
		return level.isNight() && DayCycle.isBetween(level, DayCycle.NIGHT_START, LATEST_START);
	}

	@Override
	public int initialDuration(ServerLevel level) {
		return DayCycle.untilDawn(level, settings().durationTicks());
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		// Started at night (randomly or by command): end at dawn. Started by command in daytime: run the full duration.
		active.data().putBoolean(ENDS_AT_DAWN, DayCycle.hasDayCycle(level) && level.isNight());
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), BloodMoonEvent::canBuff)) {
			buff(mob);
		}
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		boolean dawn = active.data().getBoolean(ENDS_AT_DAWN) && !level.isNight();
		if (dawn) {
			active.data().putBoolean(REACHED_DAWN, true);
		}
		return dawn;
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> mob.getTags().contains(MOB_TAG))) {
			debuff(mob);
		}
		SpawnState state = active.state(SpawnState::new);
		for (Vex eye : state.eyes) {
			if (!eye.isRemoved()) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, eye.getX(), eye.getY(), eye.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
				Creature.remove(eye);
			}
		}
		state.eyes.clear();
		if (state.lord != null && state.lord.isAlive()) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, state.lord.getX(), state.lord.getY() + 1.0, state.lord.getZ(), 40, 0.5, 1.0, 0.5, 0.02);
			Creature.remove(state.lord);
			for (ServerPlayer player : level.players()) {
				player.sendMessage(Lang.component(player, "mob.blood_moon_lord.retreat").withStyle(ChatFormatting.DARK_RED), net.minecraft.Util.NIL_UUID);
			}
		}
		state.lord = null;
		if (active.data().getBoolean(REACHED_DAWN)) {
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && player.isAlive()) {
					Advancements.award(player, "survive_blood_moon");
				}
			}
		}
	}

	/**
	 * Starts a blood moon right now if it is night and nothing else is running: what the bloody tear does.
	 * Ignores cooldowns, like {@code /worldevents start}.
	 */
	public boolean summon(ServerLevel level) {
		if (!settings().getBoolean("bloodyTearSummons") || EventManager.active(level) != null || !isAvailable(level) || !level.isNight()) {
			return false;
		}
		return EventManager.start(level, this);
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		if (settings().getBoolean("particles")) {
			for (ServerPlayer player : level.players()) {
				// A faint red mist around each player, sent only to them.
				level.sendParticles(player, MIST, false, player.getX(), player.getY() + 2.0, player.getZ(), 10, 8.0, 3.0, 8.0, 0.0);
			}
		}
		SpawnState state = active.state(SpawnState::new);
		tickLord(level, active, state);
		tickEyes(level, state);
		state.spawned.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		state.ticksUntilSpawn -= EventManager.ACTIVE_TICK_INTERVAL;
		if (state.ticksUntilSpawn > 0) {
			return;
		}
		EventSettings settings = settings();
		state.ticksUntilSpawn = Math.max(1, settings.getInt("spawnInterval")) * 20;
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
			return;
		}
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				players.add(player);
			}
		}
		int cap = Math.min(settings.getInt("maxExtraMobs"), players.size() * settings.getInt("extraMobsPerPlayer"));
		int attempts = settings.getInt("spawnAttemptsPerPlayer");
		for (ServerPlayer player : players) {
			for (int i = 0; i < attempts && state.spawned.size() < cap; i++) {
				Mob mob = trySpawnNear(level, player);
				if (mob != null) {
					state.spawned.add(mob);
				}
			}
		}
	}

	/** Every so often, a blood eye appears in the sky near each player, up to a few per player. */
	private void tickEyes(ServerLevel level, SpawnState state) {
		state.eyes.removeIf(eye -> eye.isRemoved() || !eye.isAlive());
		EventSettings settings = settings();
		if (!settings.getBoolean("bloodEyes") || level.getDifficulty() == Difficulty.PEACEFUL || --state.secondsToEyes > 0) {
			return;
		}
		state.secondsToEyes = EYE_INTERVAL_SECONDS;
		int perPlayer = settings.getInt("bloodEyesPerPlayer");
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			long near = state.eyes.stream().filter(eye -> eye.distanceToSqr(player) < 64 * 64).count();
			if (near >= perPlayer) {
				continue;
			}
			BlockPos ground = MobSpawning.surfaceSpot(level, EntityType.VEX, player.blockPosition(), 10, 20, 6);
			if (ground != null) {
				BlockPos sky = ground.above(Mth.nextInt(level.getRandom(), 6, 10));
				if (level.noCollision(EntityType.VEX.getAABB(sky.getX() + 0.5, sky.getY(), sky.getZ() + 0.5))) {
					Vex eye = BloodEye.spawn(level, sky, Lang.language(player));
					// Usually already tracked: the load callback also sees new entities.
					if (eye != null && !state.eyes.contains(eye)) {
						state.eyes.add(eye);
					}
				}
			}
		}
	}

	/** Spawns a blood eye at {@code pos} now, for tests and admins. */
	public Vex spawnEye(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		Vex eye = BloodEye.spawn(level, pos, language);
		if (eye != null && !active.state(SpawnState::new).eyes.contains(eye)) {
			active.state(SpawnState::new).eyes.add(eye);
		}
		return eye;
	}

	/** Brings the lord in once the night is under way. Its boss bar and minions are its own (see BloodMoonLord). */
	private void tickLord(ServerLevel level, ActiveEvent active, SpawnState state) {
		EventSettings settings = settings();
		int elapsedSeconds = (active.totalTicks() - active.remainingTicks()) / 20;
		if (settings.getBoolean("lord") && !active.data().getBoolean(LORD_SPAWNED) && elapsedSeconds >= settings.getInt("lordDelay")
				&& level.getDifficulty() != Difficulty.PEACEFUL) {
			spawnLordNearRandomPlayer(level, active, state);
		}
		if (state.lord != null && (state.lord.isRemoved() || !state.lord.isAlive())) {
			state.lord = null;
		}
	}

	private void spawnLordNearRandomPlayer(ServerLevel level, ActiveEvent active, SpawnState state) {
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				players.add(player);
			}
		}
		if (players.isEmpty()) {
			return;
		}
		ServerPlayer target = players.get(level.getRandom().nextInt(players.size()));
		BlockPos spot = MobSpawning.surfaceSpot(level, EntityType.WITHER_SKELETON, target.blockPosition(), 16, 32, 10);
		if (spot == null) {
			return;
		}
		WitherSkeleton lord = BloodMoonLord.spawn(level, spot, Lang.language(target), settings().getDouble("lordHealth"));
		if (lord == null) {
			return;
		}
		state.lord = lord;
		active.data().putBoolean(LORD_SPAWNED, true);
		level.playSound(null, lord.getX(), lord.getY(), lord.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.8f);
		for (ServerPlayer player : level.players()) {
			player.sendMessage(Lang.component(player, "mob.blood_moon_lord.arrived").withStyle(ChatFormatting.DARK_RED), net.minecraft.Util.NIL_UUID);
		}
		WorldEventsMod.LOGGER.info("Blood Moon Lord spawned at {}", spot);
	}

	/** Spawns the lord at {@code pos} now, for tests and admins. */
	public WitherSkeleton spawnLord(ServerLevel level, ActiveEvent active, BlockPos pos, String language) {
		SpawnState state = active.state(SpawnState::new);
		WitherSkeleton lord = BloodMoonLord.spawn(level, pos, language, settings().getDouble("lordHealth"));
		if (lord != null) {
			state.lord = lord;
			active.data().putBoolean(LORD_SPAWNED, true);
		}
		return lord;
	}

	/** One spawn attempt on the surface around {@code player}, using the biome's monster list and vanilla spawn rules. */
	private static Mob trySpawnNear(ServerLevel level, ServerPlayer player) {
		Random random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = Mth.nextDouble(random, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
		int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
		int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
		// Only spawn where entities already tick. Checked before any height lookup, which would load the chunk.
		if (!level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
			return null;
		}
		BlockPos surface = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		var spawns = level.getChunkSource().getGenerator().getMobsAt(level.getBiome(surface), level.structureFeatureManager(), MobCategory.MONSTER, surface);
		MobSpawnSettings.SpawnerData spawn = spawns.getRandom(random).orElse(null);
		if (spawn == null) {
			return null;
		}
		EntityType<?> type = spawn.type;
		BlockPos pos = new BlockPos(x, level.getHeight(SpawnPlacements.getHeightmapType(type), x, z), z);
		if (!level.isPositionEntityTicking(pos)
				|| Protection.isSpawnProtected(level, pos)
				|| !NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), level, pos, type)
				|| !SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random)) {
			return null;
		}
		Entity entity = type.create(level);
		if (!(entity instanceof Mob mob)) {
			if (entity != null) {
				entity.discard();
			}
			return null;
		}
		mob.moveTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0f, 0.0f);
		if (!mob.checkSpawnRules(level, MobSpawnType.NATURAL) || !mob.checkSpawnObstruction(level) || !level.noCollision(mob)) {
			mob.discard();
			return null;
		}
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
		level.addFreshEntityWithPassengers(mob);
		return mob;
	}

	@Override
	public void registerHooks() {
		Hooks.ENTITY_LOAD.register((entity, level) -> {
			// A summoned creature is nobody's event creature, and keeps its own strength.
			if (!(entity instanceof Mob mob) || Creature.isSummoned(mob)) {
				return;
			}
			if (BloodMoonLord.is(mob)) {
				// Back from a save or a chunk reload: keep tracking it during the blood moon, remove it otherwise.
				ActiveEvent active = EventManager.active(level);
				if (active != null && active.event() == this && mob instanceof WitherSkeleton lord) {
					SpawnState state = active.state(SpawnState::new);
					if (state.lord == null || state.lord.isRemoved()) {
						state.lord = lord;
					}
				} else {
					DeferredDiscard.discard(mob);
					mob.getPassengers().forEach(DeferredDiscard::discard);
				}
				return;
			}
			if (BloodEye.is(mob)) {
				ActiveEvent active = EventManager.active(level);
				if (active != null && active.event() == this && mob instanceof Vex eye) {
					List<Vex> eyes = active.state(SpawnState::new).eyes;
					if (!eyes.contains(eye)) {
						eyes.add(eye);
					}
				} else {
					DeferredDiscard.discard(mob);
					mob.getPassengers().forEach(DeferredDiscard::discard);
				}
				return;
			}
			boolean tagged = mob.getTags().contains(MOB_TAG);
			boolean active = EventManager.isActive(level, this);
			if (active && !tagged && canBuff(mob)) {
				buff(mob);
			} else if (!active && tagged) {
				debuff(mob);
			}
		});

		Hooks.MODIFY_DROPS.register((table, context, drops) -> {
			if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Mob mob) || !mob.getTags().contains(MOB_TAG)) {
				return;
			}
			if (context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) == null || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
			if (context.getRandom().nextDouble() < settings().getDouble("bloodyTearChance")) {
				drops.add(EventItems.BLOODY_TEAR.create(Lang.languageOf(context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER)), 1));
			}
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!BloodMoonLord.is(entity) || !(entity instanceof Mob lord) || !(lord.level instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player
					: lord.getKillCredit() instanceof ServerPlayer player ? player : null;
			if (killer == null) {
				return;
			}
			BloodMoonLord.dropLoot(level, lord, killer, settings().getDouble("scytheChance"));
			if (!Creature.isSummoned(lord)) {
				EventManager.score(killer, ID, 10);
			}
			Advancements.award(killer, "defeat_blood_moon_lord");
			for (ServerPlayer player : level.players()) {
				player.sendMessage(Lang.component(player, "mob.blood_moon_lord.defeated", killer.getName().getString()).withStyle(ChatFormatting.GOLD), net.minecraft.Util.NIL_UUID);
			}
		});

		Hooks.USE_ITEM.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!EventItems.BLOODY_TEAR.is(stack) || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
				return InteractionResult.PASS;
			}
			if (!summon(level)) {
				serverPlayer.displayClientMessage(Lang.component(serverPlayer, "item.bloody_tear.fail").withStyle(ChatFormatting.DARK_RED), true);
				return InteractionResult.FAIL;
			}
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			return InteractionResult.SUCCESS;
		});

		Hooks.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !mob.getTags().contains(MOB_TAG) || mob.getKillCredit() == null) {
				return;
			}
			if (!(mob.level instanceof ServerLevel level) || !EventManager.isActive(level, this) || !mob.shouldDropExperience()) {
				return;
			}
			if (mob.getKillCredit() instanceof ServerPlayer killer) {
				EventManager.score(killer, ID, 1);
			}
			double multiplier = settings().getDouble("xpMultiplier");
			int bonus = (int) Math.round(((LivingEntity) mob).getExperienceReward(source.getEntity() instanceof Player player ? player : null) * (multiplier - 1.0));
			if (bonus > 0) {
				ExperienceOrb.award(level, mob.position(), bonus);
			}
		});

		Hooks.ALLOW_SLEEPING.register((player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer && EventManager.isActive(serverPlayer.getLevel(), this)
					&& !settings().getBoolean("allowSleeping")) {
				// 1.18 has no custom reasons for not sleeping: the player gets the message, and the bed says nothing.
				serverPlayer.displayClientMessage(Lang.component(serverPlayer, "event.blood_moon.no_sleep"), true);
				return Player.BedSleepingProblem.OTHER_PROBLEM;
			}
			return null;
		});
	}

	/** Vanilla monsters only: the mod's creatures have their own strength. */
	private static boolean canBuff(Mob mob) {
		return mob instanceof Enemy && !mob.getTags().contains(MOB_TAG) && Creature.kindOf(mob) == null
				&& !mob.getType().is(Tags.BOSSES);
	}

	private void buff(Mob mob) {
		if (!mob.addTag(MOB_TAG)) {
			return;
		}
		EventSettings settings = settings();
		float healthRatio = mob.getHealth() / mob.getMaxHealth();
		addMultiplier(mob, Attributes.MAX_HEALTH, HEALTH_MODIFIER, settings.getDouble("healthMultiplier"));
		addMultiplier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, settings.getDouble("damageMultiplier"));
		mob.setHealth(mob.getMaxHealth() * healthRatio);
	}

	private static void debuff(Mob mob) {
		mob.removeTag(MOB_TAG);
		float healthRatio = mob.getHealth() / mob.getMaxHealth();
		removeModifier(mob, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
		removeModifier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER);
		mob.setHealth(mob.getMaxHealth() * healthRatio);
	}

	private static void addMultiplier(Mob mob, Attribute attribute, UUID id, double multiplier) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null && multiplier > 0 && multiplier != 1.0) {
			instance.removeModifier(id);
			instance.addPermanentModifier(new AttributeModifier(id, "World Events blood moon", multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE));
		}
	}

	private static void removeModifier(Mob mob, Attribute attribute, UUID id) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	/** How many extra mobs spawned by this run are still alive. */
	public static int extraMobCount(ActiveEvent active) {
		SpawnState state = active.state(SpawnState::new);
		state.spawned.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		return state.spawned.size();
	}

	/** The lord of the current run, if it is loaded and alive. */
	public static WitherSkeleton lord(ActiveEvent active) {
		WitherSkeleton lord = active.state(SpawnState::new).lord;
		return lord != null && lord.isAlive() ? lord : null;
	}

	private static final class SpawnState {
		private final List<Mob> spawned = new ArrayList<>();
		private int ticksUntilSpawn;
		private WitherSkeleton lord;
		private final List<Vex> eyes = new ArrayList<>();
		private int secondsToEyes = 10;
	}
}
