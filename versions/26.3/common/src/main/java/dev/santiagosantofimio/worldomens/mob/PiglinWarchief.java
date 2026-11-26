package dev.santiagosantofimio.worldomens.mob;

import dev.santiagosantofimio.worldomens.event.impl.PiglinWarbandEvent;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class PiglinWarchief {
	public static final Creature.Kind KIND = Creature.kind("piglin_warchief", Items.PIGLIN_BRUTE_SPAWN_EGG,
					(level, pos, language, summoner) -> spawn(level, pos, language, 150.0))
			.acting(PiglinWarchief::act)
			.boss(BossEvent.BossBarColor.YELLOW, ChatFormatting.GOLD);
	public static final String TAG = KIND.tag();
	public static final String NAME_KEY = KIND.nameKey();
	public static final Creature.Part BODY = Creature.register(Creature.Part.of(Items.GILDED_BLACKSTONE, "piglin_warchief", 1.45f).big());
	private static final String REINFORCED_TAG = TAG + ".reinforced";
	private static final int ROAR_SECONDS = 14;
	private static final double ROAR_RANGE = 16.0;
	private static final int ROAR_TICKS = 160;
	private static final int REINFORCEMENTS = 2;
	private static final int XP_REWARD = 150;

	private PiglinWarchief() {
	}

	public static PiglinBrute spawn(ServerLevel level, BlockPos pos, String language, double health) {
		PiglinBrute chief = EntityTypes.PIGLIN_BRUTE.create(level, EntitySpawnReason.EVENT);
		if (chief == null) {
			return null;
		}
		chief.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		chief.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		chief.setImmuneToZombification(true);
		MobSpawning.setBase(chief, Attributes.SCALE, 1.4);
		MobSpawning.setBase(chief, Attributes.MAX_HEALTH, health);
		MobSpawning.setBase(chief, Attributes.ATTACK_DAMAGE, 10.0);
		MobSpawning.setBase(chief, Attributes.ARMOR, 8.0);
		MobSpawning.setBase(chief, Attributes.KNOCKBACK_RESISTANCE, 0.9);
		MobSpawning.setBase(chief, Attributes.FOLLOW_RANGE, 40.0);
		chief.setHealth(chief.getMaxHealth());
		chief.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		chief.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		MobSpawning.name(chief, NAME_KEY, language, ChatFormatting.GOLD);
		Creature.mark(chief, KIND);
		level.addFreshEntityWithPassengers(chief);
		Creature.dress(chief, BODY);
		return chief;
	}

	public static boolean is(Entity entity) {
		return KIND.is(entity);
	}

	private static void act(ServerLevel level, Mob chief, int tick) {
		if (Creature.everySeconds(chief, tick, ROAR_SECONDS) && chief.getTarget() != null) {
			roar(level, chief);
		}
		if (tick % 20 == 0 && !chief.entityTags().contains(REINFORCED_TAG) && chief.getHealth() < chief.getMaxHealth() / 2.0f
				&& !Creature.isSummoned(chief)) {
			chief.addTag(REINFORCED_TAG);
			callReinforcements(level, chief);
		}
	}

	private static void roar(ServerLevel level, Mob chief) {
		level.playSound(null, chief.getX(), chief.getY(), chief.getZ(), SoundEvents.PIGLIN_BRUTE_ANGRY, SoundSource.HOSTILE, 2.0f, 0.7f);
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, chief.getX(), chief.getY() + chief.getBbHeight(), chief.getZ(), 6, 0.5, 0.3, 0.5, 0.0);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, chief.getBoundingBox().inflate(ROAR_RANGE),
				mob -> mob != chief && mob.isAlive() && (mob instanceof Enemy || mob.entityTags().contains(PiglinWarbandEvent.MOB_TAG)))) {
			mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ROAR_TICKS, 0));
			mob.addEffect(new MobEffectInstance(MobEffects.SPEED, ROAR_TICKS, 0));
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
		}
	}

	private static void callReinforcements(ServerLevel level, Mob chief) {
		level.playSound(null, chief.getX(), chief.getY(), chief.getZ(), SoundEvents.PIGLIN_BRUTE_ANGRY, SoundSource.HOSTILE, 3.0f, 0.5f);
		for (int i = 0; i < REINFORCEMENTS; i++) {
			BlockPos spot = MobSpawning.spotNear(level, EntityTypes.PIGLIN_BRUTE, chief.blockPosition(), 3, 7, 3, 8);
			if (spot == null) {
				continue;
			}
			PiglinBrute brute = EntityTypes.PIGLIN_BRUTE.create(level, EntitySpawnReason.EVENT);
			if (brute == null) {
				continue;
			}
			brute.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
			brute.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
			brute.setImmuneToZombification(true);
			brute.addTag(PiglinWarbandEvent.MOB_TAG);
			level.addFreshEntityWithPassengers(brute);
			if (chief.getTarget() != null) {
				brute.setTarget(chief.getTarget());
			}
			level.sendParticles(ParticleTypes.LARGE_SMOKE, brute.getX(), brute.getY() + 0.5, brute.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		}
	}

	public static void dropLoot(ServerLevel level, Mob chief, ServerPlayer killer, double relicChance) {
		RandomSource random = level.getRandom();
		Vec3 at = chief.position().add(0.0, 0.5, 0.0);
		if (random.nextDouble() < relicChance) {
			Drops.spawn(level, at, EventItems.WARCHIEFS_CLEAVER.create(Lang.language(killer), 1));
		}
		Drops.spawn(level, at, new ItemStack(Items.GOLD_INGOT, Mth.nextInt(random, 8, 16)));
		ExperienceOrb.award(level, at, XP_REWARD);
	}
}
