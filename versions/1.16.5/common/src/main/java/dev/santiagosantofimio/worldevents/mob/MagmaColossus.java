package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.impl.EruptionEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import dev.santiagosantofimio.worldevents.util.Entities;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The magma colossus: a hulking beast of black rock with lava running through its cracks, the heart of an eruption.
 * It charges at players and slams the ground, burning and throwing everything around it without touching a block.
 * It has its own boss bar. Moves and fights like a hoglin (the invisible mob under the model), and unlike one it
 * does not burn, does not calm down near nether portals or warped fungus, and goes after players it cannot see.
 */
public final class MagmaColossus {
	public static final Creature.Kind KIND = Creature.kind("magma_colossus", Items.HOGLIN_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(MagmaColossus::act)
			.boss(BossEvent.BossBarColor.RED, ChatFormatting.GOLD);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.MAGMA_BLOCK, "magma_colossus", 2.0f).big().bright());
	private static final int SLAM_SECONDS = 8;
	private static final double SLAM_RANGE = 6.0;
	private static final float SLAM_DAMAGE = 8.0f;
	private static final int XP_REWARD = 150;
	/** How far from where it should be it may go, to find room. */
	private static final int ROOM_RADIUS = 10;
	/** How far it looks for a player to go after when it has none. */
	private static final double HUNT_RANGE = 32.0;

	private MagmaColossus() {
	}

	public static Hoglin spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Hoglin colossus = EntityType.HOGLIN.create(level);
		if (colossus == null) {
			return null;
		}
		colossus.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		colossus.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		colossus.setBaby(false);
		// Out of the Nether a hoglin would turn into a zoglin, losing its model. Summoned ones can be anywhere.
		colossus.setImmuneToZombification(true);
		MobSpawning.setBase(colossus, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(colossus, Attributes.ATTACK_DAMAGE, 9.0);
		MobSpawning.setBase(colossus, Attributes.KNOCKBACK_RESISTANCE, 1.0);
		MobSpawning.setBase(colossus, Attributes.FOLLOW_RANGE, 40.0);
		colossus.setHealth(colossus.getMaxHealth());
		// Summoned with a command, it may be inside a hill.
		BlockPos room = MobSpawning.roomFor(level, EntityType.HOGLIN.getDimensions(), pos, ROOM_RADIUS);
		if (room != null) {
			colossus.moveTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5, colossus.yRot, 0.0f);
		}
		colossus.addEffect(fireproof());
		MobSpawning.name(colossus, NAME_KEY, language, ChatFormatting.GOLD);
		Creature.mark(colossus, KIND);
		level.addFreshEntity(colossus);
		Creature.dress(colossus, BODY);
		level.sendParticles(ParticleTypes.LAVA, colossus.getX(), colossus.getY() + 1.0, colossus.getZ(), 40, 1.0, 1.0, 1.0, 0.0);
		return colossus;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** The health set in the eruption's config, for summoned colossi too. */
	private static double configuredHealth() {
		return ModConfig.get().event(EruptionEvent.ID).getDouble("colossusHealth");
	}

	/** Lava is its home: fire resistance for good, which colossi from older versions get on their next tick. */
	private static MobEffectInstance fireproof() {
		return new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false);
	}

	/** While it is after someone, the colossus slams the ground every few seconds. */
	private static void act(ServerLevel level, Mob colossus, int tick) {
		if (!colossus.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			colossus.addEffect(fireproof());
		}
		// No flames around the invisible hoglin either.
		colossus.clearFire();
		if (tick % 20 == 0) {
			getOut(level, colossus);
		}
		hunt(level, colossus);
		if (Creature.everySeconds(colossus, tick, SLAM_SECONDS) && target(colossus) != null) {
			slam(level, colossus);
		}
	}

	/**
	 * Out of the blocks it is stuck in, to the nearest spot with room: one that spawned where there was none (on a
	 * cliff, or with an older version of the mod) or was walled in would stay there, suffocating.
	 */
	private static void getOut(ServerLevel level, Mob colossus) {
		if (level.noCollision(colossus)) {
			return;
		}
		BlockPos room = MobSpawning.roomFor(level, colossus.getDimensions(colossus.getPose()), colossus.blockPosition(), ROOM_RADIUS);
		if (room != null) {
			colossus.teleportTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5);
			level.sendParticles(ParticleTypes.LAVA, colossus.getX(), colossus.getY() + 1.0, colossus.getZ(), 20, 1.0, 1.0, 1.0, 0.0);
		}
	}

	/**
	 * Keeps the colossus after the nearest player. A hoglin only goes for players it can see, and near nether portals,
	 * respawn anchors and warped fungus (where players often are in the Nether) it calms down for a while, letting go
	 * of its target. It cannot calm down while it counts as calm already, so it always does: then it picks no targets
	 * by itself, and this picks them instead. Being hit still makes it turn on whoever hit it.
	 */
	private static void hunt(ServerLevel level, Mob colossus) {
		Brain<?> brain = colossus.getBrain();
		brain.setMemory(MemoryModuleType.PACIFIED, true);
		brain.eraseMemory(MemoryModuleType.NEAREST_REPELLENT);
		if (target(colossus) == null) {
			Player player = level.getNearestPlayer(colossus.getX(), colossus.getY(), colossus.getZ(), HUNT_RANGE,
					EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(Entity::isAlive));
			if (player != null) {
				brain.setMemory(MemoryModuleType.ATTACK_TARGET, player);
			}
		}
	}

	/** Who it is after. A hoglin keeps its target in its brain: on this version getTarget() is always null. */
	private static LivingEntity target(Mob colossus) {
		return colossus.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	/** Burns and throws everything around it except monsters. Blocks are left alone. */
	public static void slam(ServerLevel level, Mob colossus) {
		level.playSound(null, colossus.getX(), colossus.getY(), colossus.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0f, 0.5f);
		level.sendParticles(ParticleTypes.EXPLOSION, colossus.getX(), colossus.getY() + 0.5, colossus.getZ(), 6, 2.0, 0.2, 2.0, 0.0);
		level.sendParticles(ParticleTypes.FLAME, colossus.getX(), colossus.getY() + 0.2, colossus.getZ(), 60, 3.0, 0.1, 3.0, 0.05);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, colossus.getBoundingBox().inflate(SLAM_RANGE),
				entity -> entity != colossus && entity.isAlive() && !(entity instanceof Enemy))) {
			target.hurt(DamageSource.mobAttack(colossus), SLAM_DAMAGE);
			target.setSecondsOnFire(3);
			Vec3 away = target.position().subtract(colossus.position()).normalize();
			target.push(away.x * 0.8, 0.6, away.z * 0.8);
			target.hurtMarked = true;
		}
	}

	public static void dropLoot(ServerLevel level, Mob colossus, ServerPlayer killer, double hammerChance) {
		Random random = level.getRandom();
		Vec3 at = colossus.position().add(0.0, 0.5, 0.0);
		String language = Lang.language(killer);
		if (random.nextDouble() < hammerChance) {
			Drops.spawn(level, at, EventItems.COLOSSUS_HAMMER.create(language, 1));
		}
		Drops.spawn(level, at, EventItems.MAGMA_HEART.create(language, Mth.nextInt(random, 1, 2)));
		Entities.awardExperience(level, at, XP_REWARD);
	}
}
