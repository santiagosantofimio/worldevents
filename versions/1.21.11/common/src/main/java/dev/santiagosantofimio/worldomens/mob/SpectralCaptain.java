package dev.santiagosantofimio.worldomens.mob;

import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.event.impl.TreasureHuntEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.util.Drops;
import java.util.Map;
import java.util.WeakHashMap;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpectralCaptain {
	public static final Creature.Kind KIND = Creature.kind("spectral_captain", Items.VINDICATOR_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(SpectralCaptain::act)
			.boss(BossEvent.BossBarColor.PURPLE, ChatFormatting.AQUA);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.SEA_LANTERN, "spectral_captain", 1.05f).big().bright()
			.moving(Creature.Motion.FLOAT));
	private static final int CANNON_SECONDS = 9;
	private static final int CANNON_DELAY_TICKS = 40;
	private static final float CANNON_POWER = 2.5f;
	private static final int XP_REWARD = 100;
	private static final Map<Mob, Shot> SHOTS = new WeakHashMap<>();

	private record Shot(Vec3 at, int tick) {
	}

	private SpectralCaptain() {
	}

	public static Vindicator spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Vindicator captain = EntityType.VINDICATOR.create(level, EntitySpawnReason.EVENT);
		if (captain == null) {
			return null;
		}
		captain.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		captain.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		captain.setCanJoinRaid(false);
		MobSpawning.setBase(captain, Attributes.SCALE, 1.1);
		MobSpawning.setBase(captain, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(captain, Attributes.ATTACK_DAMAGE, 2.0);
		MobSpawning.setBase(captain, Attributes.KNOCKBACK_RESISTANCE, 0.6);
		MobSpawning.setBase(captain, Attributes.FOLLOW_RANGE, 32.0);
		captain.setHealth(captain.getMaxHealth());
		captain.setItemSlot(EquipmentSlot.MAINHAND, EventItems.CAPTAINS_CUTLASS.create(language, 1));
		captain.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		MobSpawning.name(captain, NAME_KEY, language, ChatFormatting.AQUA);
		Creature.mark(captain, KIND);
		level.addFreshEntityWithPassengers(captain);
		Creature.dress(captain, BODY);
		level.sendParticles(ParticleTypes.SOUL, captain.getX(), captain.getY() + 1.0, captain.getZ(), 30, 0.5, 1.0, 0.5, 0.02);
		return captain;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	private static double configuredHealth() {
		return ModConfig.get().event(TreasureHuntEvent.ID).getDouble("captainHealth");
	}

	private static void act(ServerLevel level, Mob captain, int tick) {
		Shot shot = SHOTS.get(captain);
		if (shot == null) {
			if (Creature.everySeconds(captain, tick, CANNON_SECONDS) && captain.getTarget() != null) {
				aimCannon(level, captain, captain.getTarget().position(), tick);
			}
		} else if (tick >= shot.tick()) {
			SHOTS.remove(captain);
			level.explode(null, shot.at().x, shot.at().y + 0.2, shot.at().z, CANNON_POWER, Level.ExplosionInteraction.NONE);
		} else if (tick % 5 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, shot.at().x, shot.at().y + 0.1, shot.at().z, 12, 1.0, 0.05, 1.0, 0.0);
		}
	}

	public static void aimCannon(ServerLevel level, Mob captain, Vec3 at, int tick) {
		SHOTS.put(captain, new Shot(at, tick + CANNON_DELAY_TICKS));
		level.playSound(null, at.x, at.y, at.z, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.5f, 0.7f);
	}

	public static void dropLoot(ServerLevel level, Mob captain, ServerPlayer killer, double cutlassChance) {
		RandomSource random = level.getRandom();
		Vec3 at = captain.position().add(0.0, 0.5, 0.0);
		if (random.nextDouble() < cutlassChance) {
			Drops.spawn(level, at, EventItems.CAPTAINS_CUTLASS.create(Lang.language(killer), 1));
		}
		Drops.spawn(level, at, new ItemStack(Items.GOLD_INGOT, Mth.nextInt(random, 3, 6)));
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
