package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.impl.EruptionEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The magma colossus: a hulking beast of black rock with lava running through its cracks, the heart of an eruption.
 * It charges at players and slams the ground, burning and throwing everything around it without touching a block.
 * It has its own boss bar. Moves and fights like a hoglin (the invisible mob under the model).
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

	private MagmaColossus() {
	}

	public static Hoglin spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Hoglin colossus = EntityType.HOGLIN.create(level, EntitySpawnReason.EVENT);
		if (colossus == null) {
			return null;
		}
		colossus.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		colossus.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		colossus.setBaby(false);
		// Out of the Nether a hoglin would turn into a zoglin, losing its model. Summoned ones can be anywhere.
		colossus.setImmuneToZombification(true);
		MobSpawning.setBase(colossus, Attributes.SCALE, 2.0);
		MobSpawning.setBase(colossus, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(colossus, Attributes.ATTACK_DAMAGE, 9.0);
		MobSpawning.setBase(colossus, Attributes.KNOCKBACK_RESISTANCE, 1.0);
		MobSpawning.setBase(colossus, Attributes.FOLLOW_RANGE, 40.0);
		colossus.setHealth(colossus.getMaxHealth());
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

	/** While it is after someone, the colossus slams the ground every few seconds. */
	private static void act(ServerLevel level, Mob colossus, int tick) {
		if (Creature.everySeconds(colossus, tick, SLAM_SECONDS) && colossus.getTarget() != null) {
			slam(level, colossus);
		}
	}

	/** Burns and throws everything around it except monsters. Blocks are left alone. */
	public static void slam(ServerLevel level, Mob colossus) {
		level.playSound(null, colossus.getX(), colossus.getY(), colossus.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0f, 0.5f);
		level.sendParticles(ParticleTypes.EXPLOSION, colossus.getX(), colossus.getY() + 0.5, colossus.getZ(), 6, 2.0, 0.2, 2.0, 0.0);
		level.sendParticles(ParticleTypes.FLAME, colossus.getX(), colossus.getY() + 0.2, colossus.getZ(), 60, 3.0, 0.1, 3.0, 0.05);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, colossus.getBoundingBox().inflate(SLAM_RANGE),
				entity -> entity != colossus && entity.isAlive() && !(entity instanceof Enemy))) {
			target.hurtServer(level, level.damageSources().mobAttack(colossus), SLAM_DAMAGE);
			target.igniteForSeconds(3.0f);
			Vec3 away = target.position().subtract(colossus.position()).normalize();
			target.push(away.x * 0.8, 0.6, away.z * 0.8);
			target.needsSync = true;
		}
	}

	public static void dropLoot(ServerLevel level, Mob colossus, ServerPlayer killer, double hammerChance) {
		RandomSource random = level.getRandom();
		Vec3 at = colossus.position().add(0.0, 0.5, 0.0);
		String language = Lang.language(killer);
		if (random.nextDouble() < hammerChance) {
			Drops.spawn(level, at, EventItems.COLOSSUS_HAMMER.create(language, 1));
		}
		Drops.spawn(level, at, EventItems.MAGMA_HEART.create(language, Mth.nextInt(random, 1, 2)));
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
