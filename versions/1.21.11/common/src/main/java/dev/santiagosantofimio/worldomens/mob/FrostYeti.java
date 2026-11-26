package dev.santiagosantofimio.worldomens.mob;

import dev.santiagosantofimio.worldomens.config.ModConfig;
import dev.santiagosantofimio.worldomens.event.impl.BlizzardEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.util.Drops;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class FrostYeti {
	public static final Creature.Kind KIND = Creature.kind("frost_yeti", Items.RAVAGER_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, configuredHealth()))
			.acting(FrostYeti::act)
			.boss(BossEvent.BossBarColor.BLUE, ChatFormatting.AQUA);
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.PACKED_ICE, "frost_yeti", 1.4f).big());
	private static final int ROAR_SECONDS = 8;
	private static final double ROAR_RANGE = 8.0;
	private static final int XP_REWARD = 120;

	private FrostYeti() {
	}

	public static Ravager spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Ravager yeti = EntityType.RAVAGER.create(level, EntitySpawnReason.EVENT);
		if (yeti == null) {
			return null;
		}
		yeti.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		yeti.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		MobSpawning.setBase(yeti, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(yeti, Attributes.ATTACK_DAMAGE, 10.0);
		yeti.setHealth(yeti.getMaxHealth());
		MobSpawning.name(yeti, NAME_KEY, language, ChatFormatting.AQUA);
		Creature.mark(yeti, KIND);
		level.addFreshEntity(yeti);
		Creature.dress(yeti, BODY);
		return yeti;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	private static double configuredHealth() {
		return ModConfig.get().event(BlizzardEvent.ID).getDouble("yetiHealth");
	}

	private static void act(ServerLevel level, Mob yeti, int tick) {
		if (Creature.everySeconds(yeti, tick, ROAR_SECONDS) && yeti.getTarget() != null) {
			roar(level, yeti);
		}
	}

	public static int roar(ServerLevel level, Mob yeti) {
		level.playSound(null, yeti.getX(), yeti.getY(), yeti.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0f, 0.7f);
		level.sendParticles(ParticleTypes.SNOWFLAKE, yeti.getX(), yeti.getY() + 1.5, yeti.getZ(), 120, ROAR_RANGE / 2, 1.0, ROAR_RANGE / 2, 0.05);
		int reached = 0;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || player.isCreative() || player.distanceToSqr(yeti) > ROAR_RANGE * ROAR_RANGE) {
				continue;
			}
			BlizzardEvent.chill(player, 100);
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
			reached++;
		}
		return reached;
	}

	public static void dropLoot(ServerLevel level, Mob yeti, ServerPlayer killer, double fangChance) {
		Vec3 at = yeti.position().add(0.0, 0.5, 0.0);
		if (level.getRandom().nextDouble() < fangChance) {
			Drops.spawn(level, at, EventItems.FROSTFANG.create(Lang.language(killer), 1));
		}
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
