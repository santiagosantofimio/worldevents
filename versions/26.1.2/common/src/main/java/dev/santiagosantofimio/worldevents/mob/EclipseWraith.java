package dev.santiagosantofimio.worldevents.mob;

import dev.santiagosantofimio.worldevents.config.ModConfig;
import dev.santiagosantofimio.worldevents.event.impl.EclipseEvent;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The eclipse wraith: a shadow of the darkened sun that circles the sky and swoops at players, the boss of an
 * eclipse. Every few seconds it pulses darkness over the players below and calls phantoms. It does not burn in the
 * daylight. Flies like a vex (the invisible mob under the model: a phantom's glowing eyes would show through), three times as big.
 */
public final class EclipseWraith {
	public static final Creature.Kind KIND = Creature.kind("eclipse_wraith", Items.VEX_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(EclipseWraith::act)
			.boss(BossEvent.BossBarColor.PURPLE, ChatFormatting.DARK_PURPLE);
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.COAL_BLOCK, "eclipse_wraith", 1.6f).bright()
			.moving(Creature.Motion.FLOAT));
	private static final int PULSE_SECONDS = 10;
	private static final double PULSE_RANGE = 20.0;
	private static final int PHANTOMS_PER_PULSE = 2;
	private static final int XP_REWARD = 120;

	private EclipseWraith() {
	}

	public static Vex spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Vex wraith = EntityType.VEX.create(level, EntitySpawnReason.EVENT);
		if (wraith == null) {
			return null;
		}
		wraith.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		wraith.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		// A vex spawns with a sword, which would float next to the wraith.
		wraith.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		MobSpawning.setBase(wraith, Attributes.SCALE, 3.0);
		MobSpawning.setBase(wraith, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(wraith, Attributes.ATTACK_DAMAGE, 8.0);
		wraith.setHealth(wraith.getMaxHealth());
		EclipseEvent.shadeFromSun(wraith);
		MobSpawning.name(wraith, NAME_KEY, language, ChatFormatting.DARK_PURPLE);
		Creature.mark(wraith, KIND);
		level.addFreshEntity(wraith);
		Creature.dress(wraith, BODY);
		return wraith;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	private static double configuredHealth() {
		return ModConfig.get().event(EclipseEvent.ID).getDouble("wraithHealth");
	}

	private static void act(ServerLevel level, Mob wraith, int tick) {
		EclipseEvent.keepShaded(wraith);
		if (Creature.everySeconds(wraith, tick, PULSE_SECONDS)) {
			pulse(level, wraith);
		}
	}

	/** Darkness over the players below and fresh phantoms at its side. Returns how many players it reached. */
	public static int pulse(ServerLevel level, Mob wraith) {
		int reached = 0;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || player.isCreative() || player.distanceToSqr(wraith) > PULSE_RANGE * PULSE_RANGE) {
				continue;
			}
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
			reached++;
		}
		if (reached == 0) {
			return 0;
		}
		level.sendParticles(ParticleTypes.SQUID_INK, wraith.getX(), wraith.getY(), wraith.getZ(), 60, 2.0, 1.0, 2.0, 0.05);
		level.playSound(null, wraith.getX(), wraith.getY(), wraith.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.6f);
		for (int i = 0; i < PHANTOMS_PER_PULSE; i++) {
			EclipseEvent.spawnPhantom(level, wraith.blockPosition());
		}
		return reached;
	}

	public static void dropLoot(ServerLevel level, Mob wraith, ServerPlayer killer, double shardChance) {
		Vec3 at = wraith.position();
		if (level.getRandom().nextDouble() < shardChance) {
			Drops.spawn(level, at, EventItems.ECLIPSE_SHARD.create(Lang.language(killer), 1));
		}
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
