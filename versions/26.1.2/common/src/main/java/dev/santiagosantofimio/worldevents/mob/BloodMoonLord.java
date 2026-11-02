package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.WorldEventsMod;
import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.impl.BloodMoonEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.util.Drops;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The Blood Moon Lord: a crowned blood-red moon with an angry face that floats after players, swinging the blood
 * scythe and calling minions. It has its own boss bar. Moves and fights like a wither skeleton (the invisible mob
 * under the model, whose scythe stays visible). Comes once per blood moon and leaves at dawn, unless summoned.
 */
public final class BloodMoonLord {
	public static final Creature.Kind KIND = Creature.kind("blood_moon_lord", Items.WITHER_SKELETON_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(BloodMoonLord::act)
			.boss(BossEvent.BossBarColor.RED, ChatFormatting.DARK_RED);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	/** Zombies and skeletons the lord called; it keeps a few around it at most. */
	public static final String MINION_TAG = WorldEventsMod.MOD_ID + ".lord_minion";
	public static final Creature.Part ORB = Creature.register(Creature.Part.of(Items.REDSTONE_BLOCK, "blood_moon_lord", 1.7f).bright().outline(0xC01010)
			.moving(Creature.Motion.PULSE));
	public static final Creature.Part CROWN = Creature.register(Creature.Part.of(Items.GOLD_BLOCK, "blood_moon_lord_crown", 1.0f).above(1.15f).bright()
			.moving(Creature.Motion.PULSE));
	private static final int MINION_INTERVAL_SECONDS = 12;
	private static final int MAX_MINIONS = 6;
	private static final double MINION_RANGE = 32.0;
	private static final int XP_REWARD = 150;

	private BloodMoonLord() {
	}

	public static WitherSkeleton spawn(ServerLevel level, BlockPos pos, String language, double health) {
		WitherSkeleton lord = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
		if (lord == null) {
			return null;
		}
		lord.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		lord.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		MobSpawning.setBase(lord, Attributes.SCALE, 1.3);
		MobSpawning.setBase(lord, Attributes.MAX_HEALTH, health);
		// The scythe it holds adds +8, for 10 damage per hit (plus the wither effect of its kind).
		MobSpawning.setBase(lord, Attributes.ATTACK_DAMAGE, 2.0);
		MobSpawning.setBase(lord, Attributes.MOVEMENT_SPEED, 0.3);
		MobSpawning.setBase(lord, Attributes.KNOCKBACK_RESISTANCE, 0.8);
		MobSpawning.setBase(lord, Attributes.FOLLOW_RANGE, 40.0);
		lord.setHealth(lord.getMaxHealth());
		lord.setItemSlot(EquipmentSlot.MAINHAND, EventItems.BLOOD_SCYTHE.create(language, 1));
		lord.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		MobSpawning.name(lord, NAME_KEY, language, ChatFormatting.DARK_RED);
		Creature.mark(lord, KIND);
		level.addFreshEntityWithPassengers(lord);
		// The orb's outline glows so players can find it in the dark.
		Creature.dress(lord, ORB, CROWN);
		return lord;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	/** The health set in the blood moon's config, for summoned lords too. */
	private static double configuredHealth() {
		return ModConfig.get().event(BloodMoonEvent.ID).getDouble("lordHealth");
	}

	/** While it is after someone, the lord calls minions every few seconds. */
	private static void act(ServerLevel level, Mob lord, int tick) {
		if (Creature.everySeconds(lord, tick, MINION_INTERVAL_SECONDS) && lord.getTarget() != null) {
			summonMinions(level, lord);
		}
	}

	/** Calls up to two zombies or skeletons next to the lord, keeping at most {@link #MAX_MINIONS} around it. */
	public static void summonMinions(ServerLevel level, Mob lord) {
		int around = level.getEntitiesOfClass(Mob.class, lord.getBoundingBox().inflate(MINION_RANGE),
				mob -> mob.isAlive() && mob.entityTags().contains(MINION_TAG)).size();
		RandomSource random = level.getRandom();
		for (int i = 0; i < 2 && around < MAX_MINIONS; i++) {
			EntityType<? extends Mob> type = random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
			BlockPos spot = MobSpawning.spotNear(level, type, lord.blockPosition(), 1, 4, 2, 6);
			if (spot == null) {
				continue;
			}
			Mob minion = type.create(level, EntitySpawnReason.EVENT);
			if (minion == null) {
				continue;
			}
			minion.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
			minion.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
			minion.addTag(MINION_TAG);
			level.addFreshEntityWithPassengers(minion);
			around++;
			level.sendParticles(new DustParticleOptions(0x8A0303, 1.5f),
					spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5, 20, 0.4, 0.8, 0.4, 0.0);
		}
	}

	public static void dropLoot(ServerLevel level, Mob lord, ServerPlayer killer, double scytheChance) {
		RandomSource random = level.getRandom();
		Vec3 at = lord.position().add(0.0, 0.5, 0.0);
		String language = Lang.language(killer);
		if (random.nextDouble() < scytheChance) {
			Drops.spawn(level, at, EventItems.BLOOD_SCYTHE.create(language, 1));
		}
		Drops.spawn(level, at, EventItems.BLOODY_TEAR.create(language, Mth.nextInt(random, 1, 2)));
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
