package dev.santiagosantofimio.worldevents.event.impl;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.EventSettings;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.WorldEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A night with more hostile mobs, which are also tougher and drop more loot and XP.
 * Ends at dawn. Mobs buffed by the event carry an entity tag so the buff can be undone afterwards.
 */
public final class BloodMoonEvent implements WorldEvent {
	public static final String ID = "blood_moon";
	private static final String MOB_TAG = WorldEventsMod.MOD_ID + ".blood_moon";
	private static final Identifier HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "blood_moon_health");
	private static final Identifier DAMAGE_MODIFIER = Identifier.fromNamespaceAndPath(WorldEventsMod.MOD_ID, "blood_moon_damage");
	private static final String ENDS_AT_DAWN = "endsAtDawn";

	private static final long DAY_LENGTH = 24000;
	private static final long NIGHT_START = 13000;
	private static final long LATEST_START = 16000;
	private static final long DAWN = 23500;
	private static final int MIN_SPAWN_DISTANCE = 24;
	private static final int MAX_SPAWN_DISTANCE = 44;

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
	public Holder<SoundEvent> startSound() {
		return SoundEvents.RAID_HORN;
	}

	private static long timeOfDay(ServerLevel level) {
		return Math.floorMod(level.getDefaultClockTime(), DAY_LENGTH);
	}

	private static boolean hasDayCycle(ServerLevel level) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime();
	}

	@Override
	public boolean canStart(ServerLevel level) {
		long time = timeOfDay(level);
		return hasDayCycle(level)
				&& level.getDifficulty() != Difficulty.PEACEFUL
				&& level.getGameRules().get(GameRules.SPAWN_MOBS)
				&& level.isDarkOutside()
				&& time >= NIGHT_START && time < LATEST_START;
	}

	@Override
	public int initialDuration(ServerLevel level) {
		int configured = settings().durationTicks();
		long time = timeOfDay(level);
		if (hasDayCycle(level) && level.isDarkOutside() && time < DAWN) {
			return (int) Math.min(configured, DAWN - time);
		}
		return configured;
	}

	@Override
	public void start(ServerLevel level, ActiveEvent active) {
		// Started at night (randomly or by command): end at dawn. Started by command in daytime: run the full duration.
		active.data().putBoolean(ENDS_AT_DAWN, hasDayCycle(level) && level.isDarkOutside());
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), BloodMoonEvent::canBuff)) {
			buff(mob);
		}
	}

	@Override
	public boolean shouldEndEarly(ServerLevel level, ActiveEvent active) {
		return active.data().getBooleanOr(ENDS_AT_DAWN, false) && !level.isDarkOutside();
	}

	@Override
	public void end(ServerLevel level, ActiveEvent active) {
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> mob.entityTags().contains(MOB_TAG))) {
			debuff(mob);
		}
	}

	@Override
	public void tick(ServerLevel level, ActiveEvent active) {
		SpawnState state = active.state(SpawnState::new);
		state.spawned.removeIf(mob -> mob.isRemoved() || !mob.isAlive());
		state.ticksUntilSpawn -= EventManager.ACTIVE_TICK_INTERVAL;
		if (state.ticksUntilSpawn > 0) {
			return;
		}
		EventSettings settings = settings();
		state.ticksUntilSpawn = Math.max(1, settings.getInt("spawnInterval")) * 20;
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
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

	/** One spawn attempt on the surface around {@code player}, using the biome's monster list and vanilla spawn rules. */
	private static Mob trySpawnNear(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = Mth.nextDouble(random, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
		int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
		int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
		// Only spawn where entities already tick. Checked before any height lookup, which would load the chunk.
		if (!level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
			return null;
		}
		BlockPos surface = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		var spawns = level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), MobCategory.MONSTER, surface);
		MobSpawnSettings.SpawnerData spawn = spawns.getRandom(random).orElse(null);
		if (spawn == null) {
			return null;
		}
		EntityType<?> type = spawn.type();
		BlockPos pos = new BlockPos(x, level.getHeight(SpawnPlacements.getHeightmapType(type), x, z), z);
		if (!level.isPositionEntityTicking(pos)
				|| !SpawnPlacements.isSpawnPositionOk(type, level, pos)
				|| !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)) {
			return null;
		}
		Entity entity = type.create(level, EntitySpawnReason.NATURAL);
		if (!(entity instanceof Mob mob)) {
			if (entity != null) {
				entity.discard();
			}
			return null;
		}
		mob.snapTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0f, 0.0f);
		if (!mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) || !mob.checkSpawnObstruction(level) || !level.noCollision(mob)) {
			mob.discard();
			return null;
		}
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
		level.addFreshEntityWithPassengers(mob);
		return mob;
	}

	@Override
	public void registerHooks() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			boolean tagged = mob.entityTags().contains(MOB_TAG);
			boolean active = EventManager.isActive(level, this);
			if (active && !tagged && canBuff(mob)) {
				buff(mob);
			} else if (!active && tagged) {
				debuff(mob);
			}
		});

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Mob mob) || !mob.entityTags().contains(MOB_TAG)) {
				return;
			}
			if (context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER) == null || !EventManager.isActive(context.getLevel(), this)) {
				return;
			}
			Drops.multiply(drops, settings().getDouble("dropMultiplier"), context.getRandom());
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Mob mob) || !mob.entityTags().contains(MOB_TAG) || mob.getLastHurtByPlayer() == null) {
				return;
			}
			if (!(mob.level() instanceof ServerLevel level) || !EventManager.isActive(level, this) || !mob.shouldDropExperience()) {
				return;
			}
			double multiplier = settings().getDouble("xpMultiplier");
			int bonus = (int) Math.round(mob.getExperienceReward(level, source.getEntity()) * (multiplier - 1.0));
			if (bonus > 0) {
				ExperienceOrb.award(level, mob.position(), bonus);
			}
		});

		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer && EventManager.isActive(serverPlayer.level(), this)
					&& !settings().getBoolean("allowSleeping")) {
				return new Player.BedSleepingProblem(Lang.component(serverPlayer, "event.blood_moon.no_sleep"));
			}
			return null;
		});
	}

	private static boolean canBuff(Mob mob) {
		return mob instanceof Enemy && !mob.entityTags().contains(MOB_TAG) && !mob.typeHolder().is(ConventionalEntityTypeTags.BOSSES);
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

	private static void addMultiplier(Mob mob, Holder<Attribute> attribute, Identifier id, double multiplier) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null && multiplier > 0 && multiplier != 1.0) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(id, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private static void removeModifier(Mob mob, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	private static final class SpawnState {
		private final List<Mob> spawned = new ArrayList<>();
		private int ticksUntilSpawn;
	}
}
