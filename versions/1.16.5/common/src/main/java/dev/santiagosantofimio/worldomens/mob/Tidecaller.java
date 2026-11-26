package dev.santiagosantofimio.worldomens.mob;

import net.minecraft.world.entity.EntityType;
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
import net.minecraft.util.Mth;
import java.util.Random;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import dev.santiagosantofimio.worldomens.util.Entities;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class Tidecaller {
	public static final Creature.Kind KIND = Creature.kind("tidecaller", Items.DROWNED_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, 180.0))
			.acting(Tidecaller::act)
			.boss(BossEvent.BossBarColor.BLUE, ChatFormatting.AQUA);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.PRISMARINE_BRICKS, "tidecaller", 1.45f).big());
	private static final int UNDERTOW_SECONDS = 12;
	private static final double UNDERTOW_RANGE = 14.0;
	private static final double UNDERTOW_MIN_DISTANCE = 3.0;
	private static final double UNDERTOW_PULL = 0.9;
	private static final int UNDERTOW_SLOW_TICKS = 80;
	private static final int XP_REWARD = 150;

	private Tidecaller() {
	}

	public static Drowned spawn(ServerLevel level, BlockPos pos, String language, double health) {
		Drowned caller = EntityType.DROWNED.create(level);
		if (caller == null) {
			return null;
		}
		caller.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		caller.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		caller.setCanBreakDoors(false);
		MobSpawning.setBase(caller, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(caller, Attributes.ATTACK_DAMAGE, 7.0);
		MobSpawning.setBase(caller, Attributes.ARMOR, 6.0);
		MobSpawning.setBase(caller, Attributes.KNOCKBACK_RESISTANCE, 0.9);
		MobSpawning.setBase(caller, Attributes.FOLLOW_RANGE, 40.0);
		caller.setHealth(caller.getMaxHealth());
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			caller.setItemSlot(slot, ItemStack.EMPTY);
		}
		MobSpawning.name(caller, NAME_KEY, language, ChatFormatting.AQUA);
		Creature.mark(caller, KIND);
		level.addFreshEntityWithPassengers(caller);
		Creature.dress(caller, BODY);
		return caller;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	private static void act(ServerLevel level, Mob caller, int tick) {
		if (Creature.everySeconds(caller, tick, UNDERTOW_SECONDS) && caller.getTarget() != null) {
			undertow(level, caller);
		}
	}

	private static void undertow(ServerLevel level, Mob caller) {
		level.playSound(null, caller.getX(), caller.getY(), caller.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2.0f, 0.8f);
		level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, caller.getX(), caller.getY() + 0.5, caller.getZ(), 30, 1.5, 0.5, 1.5, 0.1);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, caller.getBoundingBox().inflate(UNDERTOW_RANGE),
				player -> player.isAlive() && !player.isSpectator() && !player.abilities.instabuild)) {
			Vec3 toward = caller.position().subtract(player.position());
			double distance = toward.length();
			if (distance < UNDERTOW_MIN_DISTANCE) {
				continue;
			}
			Vec3 pull = toward.scale(UNDERTOW_PULL / distance).add(0.0, 0.25, 0.0);
			player.setDeltaMovement(player.getDeltaMovement().add(pull));
			player.hurtMarked = true;
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, UNDERTOW_SLOW_TICKS, 1));
			level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
		}
	}

	public static void dropLoot(ServerLevel level, Mob caller, ServerPlayer killer, double relicChance) {
		Random random = level.getRandom();
		Vec3 at = caller.position().add(0.0, 0.5, 0.0);
		if (random.nextDouble() < relicChance) {
			Drops.spawn(level, at, EventItems.TIDECALLERS_CONCH.create(Lang.language(killer), 1));
		}
		Drops.spawn(level, at, new ItemStack(Items.PRISMARINE_CRYSTALS, Mth.nextInt(random, 6, 12)));
		Drops.spawn(level, at, new ItemStack(Items.NAUTILUS_SHELL, 1));
		Entities.awardExperience(level, at, XP_REWARD);
	}
}
